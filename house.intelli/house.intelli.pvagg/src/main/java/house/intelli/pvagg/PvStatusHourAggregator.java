package house.intelli.pvagg;

import static java.util.Objects.*;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import house.intelli.core.TimeInterval;
import house.intelli.jdo.model.PvStatusHourDao;
import house.intelli.jdo.model.PvStatusHourEntity;

public class PvStatusHourAggregator extends PvStatusAggregator<PvStatusHourEntity> {

	@Override
	protected PvStatusHourEntity createAggregatedPvStatusEntity() {
		return new PvStatusHourEntity();
	}

	@Override
	protected PvStatusHourEntity getAggregatedPvStatus(String deviceName, Date measured) {
		return getTransactionOrFail().getDao(PvStatusHourDao.class).getPvStatusHourEntity(deviceName, measured);
	}

	@Override
	protected void persistAggregatedPvStatus(PvStatusHourEntity aggregatedPvStatus) {
		requireNonNull(aggregatedPvStatus, "aggregatedPvStatus");
		getTransactionOrFail().getDao(PvStatusHourDao.class).makePersistent(aggregatedPvStatus);
	}

	@Override
	public TimeInterval getTimeInterval(Date timestamp) {
		requireNonNull(timestamp, "timestamp");
		GregorianCalendar cal = new GregorianCalendar();
		cal.setTime(timestamp);
		cal.set(Calendar.MILLISECOND, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MINUTE, 0);

		Date startDateInclusive = cal.getTime();
		cal.add(Calendar.HOUR, 1);
		Date endDateExclusive = cal.getTime();

		if (startDateInclusive.after(timestamp))
			throw new IllegalStateException("startDateInclusive > timestamp");

		if (! timestamp.before(endDateExclusive))
			throw new IllegalStateException("endDateExclusive <= timestamp");

		return new TimeInterval(startDateInclusive, endDateExclusive);
	}
}
