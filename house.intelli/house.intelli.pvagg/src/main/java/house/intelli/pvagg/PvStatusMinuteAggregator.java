package house.intelli.pvagg;

import static java.util.Objects.*;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import house.intelli.core.TimeInterval;
import house.intelli.jdo.model.PvStatusMinuteDao;
import house.intelli.jdo.model.PvStatusMinuteEntity;

public class PvStatusMinuteAggregator extends PvStatusAggregator<PvStatusMinuteEntity> {

	@Override
	protected PvStatusMinuteEntity createAggregatedPvStatusEntity() {
		return new PvStatusMinuteEntity();
	}

	@Override
	protected PvStatusMinuteEntity getAggregatedPvStatus(String deviceName, Date measured) {
		return getTransactionOrFail().getDao(PvStatusMinuteDao.class).getPvStatusMinuteEntity(deviceName, measured);
	}

	@Override
	protected void persistAggregatedPvStatus(PvStatusMinuteEntity aggregatedPvStatus) {
		requireNonNull(aggregatedPvStatus, "aggregatedPvStatus");
		getTransactionOrFail().getDao(PvStatusMinuteDao.class).makePersistent(aggregatedPvStatus);
	}

	@Override
	public long getAggregatePeriodMillis() {
		return PvStatusMinuteEntity.AGGREGATE_PERIOD_MILLIS;
	}

	@Override
	public TimeInterval getTimeInterval(Date timestamp) {
		requireNonNull(timestamp, "timestamp");
		GregorianCalendar cal = new GregorianCalendar();
		cal.setTime(timestamp);
		cal.set(Calendar.MILLISECOND, 0);
		cal.set(Calendar.SECOND, 0);

		Date startDateInclusive = cal.getTime();
		cal.add(Calendar.MINUTE, 1);
		Date endDateExclusive = cal.getTime();

		if (startDateInclusive.after(timestamp))
			throw new IllegalStateException("startDateInclusive > timestamp");

		if (! timestamp.before(endDateExclusive))
			throw new IllegalStateException("endDateExclusive <= timestamp");

		return new TimeInterval(startDateInclusive, endDateExclusive);
	}
}
