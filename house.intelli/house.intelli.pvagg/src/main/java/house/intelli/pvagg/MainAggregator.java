package house.intelli.pvagg;

import static java.util.Objects.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import javax.jdo.PersistenceManagerFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import house.intelli.core.TimeInterval;
import house.intelli.jdo.IntelliHouseTransaction;
import house.intelli.jdo.IntelliHouseTransactionImpl;
import house.intelli.jdo.model.PvStatusAggregationStateDao;
import house.intelli.jdo.model.PvStatusAggregationStateEntity;
import house.intelli.jdo.model.PvStatusDao;
import house.intelli.jdo.model.PvStatusEntity;

public class MainAggregator {

	private static final Logger logger = LoggerFactory.getLogger(MainAggregator.class);

	protected final PersistenceManagerFactory pmf;

	public MainAggregator(final PersistenceManagerFactory pmf) {
		this.pmf = requireNonNull(pmf, "pmf");
	}

	public void run() {
		Date firstNonAggregatedMeasured = getFirstNonAggregatedMeasured();

		PvStatusInterpolator pvStatusInterpolator = new PvStatusInterpolator();
		List<PvStatusAggregator<?>> pvStatusAggregators = createPvStatusAggregators();
		TimeInterval startInterval = null;
		for (PvStatusAggregator<?> pvStatusAggregator : pvStatusAggregators) {
			TimeInterval interval = pvStatusAggregator.getTimeInterval(firstNonAggregatedMeasured);
			if (startInterval == null
					|| interval.getFromIncl().before(startInterval.getFromIncl())
					|| (interval.getFromIncl().equals(startInterval.getFromIncl()) && interval.getToExcl().after(startInterval.getToExcl()))) {
					startInterval = interval;
			}
		}
		logger.info("run: startInterval={}", startInterval);
		if (startInterval == null)
			return; // nothing to do ;-)

		TimeInterval interval = startInterval;
		while (true) {
			try (IntelliHouseTransaction tx = beginTransaction()) {
				PvStatusDao pvStatusDao = tx.getDao(PvStatusDao.class);

				List<PvStatusEntity> pvStatusEntities = pvStatusDao.getPvStatusEntitiesMeasuredBetween(interval);
				logger.info("run: Found {} PvStatusEntity instances for {}", pvStatusEntities.size(), interval);

				if (pvStatusEntities.isEmpty()) {
					if (interval.getToExcl().after(new Date()))
						break;
				} else {
					pvStatusInterpolator.setTransaction(tx);
					pvStatusEntities = pvStatusInterpolator.interpolate(pvStatusEntities, interval);
					pvStatusInterpolator.setTransaction(null);

					// The current logic requires that the largest time-interval must be an aligned
					// multiple of all smaller time-intervals! => Sanity-check now!
					for (PvStatusAggregator<?> pvStatusAggregator : pvStatusAggregators) {
						TimeInterval ti = pvStatusAggregator.getTimeInterval(interval.getFromIncl());
						if (ti.getFromIncl().before(interval.getFromIncl()))
							throw new IllegalStateException(String.format(
									"Time-interval %s of PvStatusAggregator %s does not align fromIncl with main interval %s!",
									ti, pvStatusAggregator.getClass().getName(), interval));

						ti = pvStatusAggregator.getTimeInterval(new Date(interval.getToExcl().getTime() - 1));
						if (ti.getToExcl().before(interval.getToExcl()))
							throw new IllegalStateException(String.format(
									"Time-interval %s of PvStatusAggregator %s does not align toExcl with main interval %s!",
									ti, pvStatusAggregator.getClass().getName(), interval));
					}

					// Make all aggregators do their work.
					for (PvStatusAggregator<?> pvStatusAggregator : pvStatusAggregators) {
						pvStatusAggregator.setTransaction(tx);
						pvStatusAggregator.aggregate(pvStatusEntities);
						pvStatusAggregator.setTransaction(null);
					}

					// Update the 'last' persistent state so we continue here, if we're interrupted.
					PvStatusEntity lastAggregatedPvStatusEntity = null;
					for (PvStatusEntity pvStatusEntity : pvStatusEntities) {
						if (pvStatusEntity.getId() < 0)
							continue; // ignore extrapolated entities!

						if (lastAggregatedPvStatusEntity == null
								|| lastAggregatedPvStatusEntity.getId() < pvStatusEntity.getId())
							lastAggregatedPvStatusEntity = pvStatusEntity;
					}
					requireNonNull(lastAggregatedPvStatusEntity, "lastAggregatedPvStatusEntity"); // impossible to be null

					PvStatusAggregationStateEntity aggregationStateEntity = getPvStatusAggregationStateEntity(tx);
					aggregationStateEntity.setLastAggregatedPvStatusEntity(lastAggregatedPvStatusEntity);

					// Commit all work done for the current interval.
					tx.commit();
				}
				// Proceed to next interval.
				interval = new TimeInterval(interval.getToExcl(), new Date(interval.getToExcl().getTime() + interval.getLengthMillis()));
			}
		}
	}

	protected Date getFirstNonAggregatedMeasured() {
		try (IntelliHouseTransaction tx = beginTransaction()) {
			PvStatusAggregationStateEntity stateEntity = getPvStatusAggregationStateEntity(tx);

			PvStatusDao pvStatusDao = tx.getDao(PvStatusDao.class);
			PvStatusEntity lastAggregatedPvStatusEntity = stateEntity.getLastAggregatedPvStatusEntity();

			Date result;
			if (lastAggregatedPvStatusEntity == null)
				result = pvStatusDao.getFirstMeasured();
			else
				result = pvStatusDao.getFirstMeasuredAfter(lastAggregatedPvStatusEntity.getId());

			tx.commit();
			return result;
		}
	}

	protected PvStatusAggregationStateEntity getPvStatusAggregationStateEntity(IntelliHouseTransaction tx) {
		PvStatusAggregationStateDao stateDao = tx.getDao(PvStatusAggregationStateDao.class);
		Collection<PvStatusAggregationStateEntity> stateEntities = stateDao.getObjects();
		PvStatusAggregationStateEntity stateEntity;
		if (stateEntities.isEmpty()) {
			stateEntity = new PvStatusAggregationStateEntity();
			stateDao.makePersistent(stateEntity);
		} else if (stateEntities.size() == 1)
			stateEntity = stateEntities.iterator().next();
		else
			throw new IllegalStateException("Multiple PvStatusAggregationStateEntity found! Should be 0 or 1!");
		return stateEntity;
	}

	protected IntelliHouseTransaction beginTransaction() {
		return new IntelliHouseTransactionImpl(pmf);
	}

	protected List<PvStatusAggregator<?>> createPvStatusAggregators() {
		List<PvStatusAggregator<?>> result = new ArrayList<>();
		result.add(new PvStatusMinuteAggregator());
		result.add(new PvStatusQuarterHourAggregator());
		result.add(new PvStatusHourAggregator());
		return result;
	}

}
