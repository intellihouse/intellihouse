package house.intelli.pvagg;

import static java.util.Objects.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import javax.jdo.PersistenceManagerFactory;

import house.intelli.core.TimeInterval;
import house.intelli.jdo.IntelliHouseTransaction;
import house.intelli.jdo.IntelliHouseTransactionImpl;
import house.intelli.jdo.model.PvStatusAggregationStateDao;
import house.intelli.jdo.model.PvStatusAggregationStateEntity;
import house.intelli.jdo.model.PvStatusDao;
import house.intelli.jdo.model.PvStatusEntity;

public class MainAggregator {

	protected final PersistenceManagerFactory pmf;

	public MainAggregator(final PersistenceManagerFactory pmf) {
		this.pmf = requireNonNull(pmf, "pmf");
	}

	public void run() {
		Date firstNonAggregatedMeasured = getFirstNonAggregatedMeasured();

		List<PvStatusAggregator<?>> pvStatusAggregators = createPvStatusAggregators();
		TimeInterval startInterval = null;
		for (PvStatusAggregator<?> pvStatusAggregator : pvStatusAggregators) {
			TimeInterval interval = pvStatusAggregator.getTimeInterval(firstNonAggregatedMeasured);
			if (startInterval == null
					|| interval.getFromIncl().before(startInterval.getFromIncl())) {
					startInterval = interval;
			}
		}
		if (startInterval == null)
			return; // nothing to do ;-)

		TimeInterval interval = startInterval;
		while (true) {
			try (IntelliHouseTransaction tx = beginTransaction()) {
				PvStatusDao pvStatusDao = tx.getDao(PvStatusDao.class);

				List<PvStatusEntity> pvStatusEntities = pvStatusDao.getPvStatusEntitiesMeasuredBetween(interval);
				if (pvStatusEntities.isEmpty())
					break;

				// Currently the largest time-interval must be an aligned multiple of all smaller time-intervals!
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

				for (PvStatusAggregator<?> pvStatusAggregator : pvStatusAggregators) {
					pvStatusAggregator.setTransaction(tx);
					pvStatusAggregator.aggregate(pvStatusEntities);
				}

				PvStatusEntity lastAggregatedPvStatusEntity = null;
				for (PvStatusEntity pvStatusEntity : pvStatusEntities) {
					if (lastAggregatedPvStatusEntity == null
							|| lastAggregatedPvStatusEntity.getId() < pvStatusEntity.getId())
						lastAggregatedPvStatusEntity = pvStatusEntity;
				}
				requireNonNull(lastAggregatedPvStatusEntity, "lastAggregatedPvStatusEntity"); // impossible to be null

				PvStatusAggregationStateEntity aggregationStateEntity = getPvStatusAggregationStateEntity(tx);
				aggregationStateEntity.setLastAggregatedPvStatusEntity(lastAggregatedPvStatusEntity);
			  tx.commit();
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
		return result;
	}

}
