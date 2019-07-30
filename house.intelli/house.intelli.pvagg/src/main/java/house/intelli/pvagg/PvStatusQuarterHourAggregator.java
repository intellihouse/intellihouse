package house.intelli.pvagg;

import static java.util.Objects.*;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.SortedSet;
import java.util.TreeSet;

import house.intelli.core.TimeInterval;
import house.intelli.jdo.model.PvStatusQuarterHourDao;
import house.intelli.jdo.model.PvStatusQuarterHourEntity;

public class PvStatusQuarterHourAggregator extends PvStatusAggregator<PvStatusQuarterHourEntity> {

	/**
	 * The starting minutes of each quarter of an hour.
	 */
	private static final SortedSet<Integer> QUARTERLY_MINUTES = new TreeSet<>(Arrays.asList(0, 15, 30, 45));

	@Override
	protected PvStatusQuarterHourEntity createAggregatedPvStatusEntity() {
		return new PvStatusQuarterHourEntity();
	}

	@Override
	protected PvStatusQuarterHourEntity getAggregatedPvStatus(String deviceName, Date measured) {
		return getTransactionOrFail().getDao(PvStatusQuarterHourDao.class).getPvStatusQuarterHourEntity(deviceName, measured);
	}

	@Override
	protected void persistAggregatedPvStatus(PvStatusQuarterHourEntity aggregatedPvStatus) {
		requireNonNull(aggregatedPvStatus, "aggregatedPvStatus");
		getTransactionOrFail().getDao(PvStatusQuarterHourDao.class).makePersistent(aggregatedPvStatus);
	}

	@Override
	public TimeInterval getTimeInterval(Date timestamp) {
		requireNonNull(timestamp, "timestamp");
		GregorianCalendar cal = new GregorianCalendar();
		cal.setTime(timestamp);
		cal.set(Calendar.MILLISECOND, 0);
		cal.set(Calendar.SECOND, 0);

		while (! QUARTERLY_MINUTES.contains(cal.get(Calendar.MINUTE))) {
			cal.add(Calendar.MINUTE, -1);
		}
		Date startDateInclusive = cal.getTime();

		cal.add(Calendar.MINUTE, 15);
		Date endDateExclusive = cal.getTime();

		if (startDateInclusive.after(timestamp))
			throw new IllegalStateException("startDateInclusive > timestamp");

		if (! timestamp.before(endDateExclusive))
			throw new IllegalStateException("endDateExclusive <= timestamp");

		return new TimeInterval(startDateInclusive, endDateExclusive);
	}

}
