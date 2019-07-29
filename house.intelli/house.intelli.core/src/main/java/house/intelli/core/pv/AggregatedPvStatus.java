package house.intelli.core.pv;

import java.util.Date;

public interface AggregatedPvStatus extends PvStatus {

	/**
	 * {@inheritDoc}
	 * <p>
	 * For measured data, this is the timestamp following immediately after the data was measured,
	 * because it is pretty straight forward that a machine measures data first, and then sends the
	 * data to the data-collector. When the data collector receives the data (and assigns the
	 * {@code measured} timestamp), then the actual measurement has already happened in the past.
	 * <p>
	 * For aggregated data, however, this {@code measured} timestamp is the beginning of the time-period
	 * covered by this entity.
	 * <p>
	 * So for example, if {@code PvStatus} objects were measured at:
	 * <ul>
	 * <li>2019-07-26 14:23:00.294
	 * <li>2019-07-26 14:23:01.301
	 * <li>2019-07-26 14:23:02.194
	 * <li>2019-07-26 14:23:03.243
	 * <li>2019-07-26 14:23:04.314
	 * <li>...
	 * <li>2019-07-26 14:23:57.476
	 * <li>2019-07-26 14:23:58.510
	 * <li>2019-07-26 14:23:59.620
	 * </ul>
	 * Then the aggregated entity for this one minute starts at 2019-07-26 14:23:00.000 including
	 * and ends at 2019-07-26 14:24:00.000 excluding. Its {@code measured} timestamp is
	 * 2019-07-26 14:23:00.000 and given a time resolution of 1 millisecond, the last possible measured
	 * timestamp of the raw-data is 2019-07-26 14:23:59.999.
	 * <p>
	 * The corresponding aggregated entity for a quarter of an hour has the {@code measured} timestamp
	 * 2019-07-26 14:15:00.000.
	 */
	@Override
	Date getMeasured();

	/**
	 * How many milliseconds were aggregated by this instance?
	 * <p>
	 * The time covered by this aggregation-entity is from {@link #getMeasured() measured} <i>including</i>
	 * until {@link #getMeasured() measured} + {@code aggregatePeriodMillis} <i>excluding</i>.
	 * @return how many milliseconds were aggregated by this instance?
	 */
	int getAggregatePeriodMillis();

}
