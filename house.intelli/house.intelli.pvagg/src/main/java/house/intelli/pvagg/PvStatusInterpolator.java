package house.intelli.pvagg;

import static java.util.Objects.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import house.intelli.core.TimeInterval;
import house.intelli.jdo.model.PvStatusDao;
import house.intelli.jdo.model.PvStatusEntity;

public class PvStatusInterpolator extends PvStatusAggregator<PvStatusEntity> {

	private static final Logger logger = LoggerFactory.getLogger(PvStatusInterpolator.class);

	private static final long THOUSAND_MILLIS = 1000L;

	private PvStatusEntity lastAggregatedPvStatus;

	@Override
	protected PvStatusEntity createAggregatedPvStatusEntity() {
		throw new UnsupportedOperationException();
	}

	@Override
	protected PvStatusEntity getAggregatedPvStatus(String deviceName, Date measured) {
		throw new UnsupportedOperationException();
	}

	public List<PvStatusEntity> interpolate(List<PvStatusEntity> pvStatusEntities, TimeInterval interval) {
		requireNonNull(pvStatusEntities, "pvStatusEntities");
		requireNonNull(interval, "interval");

		final int listCapacity = (int) (interval.getLengthMillis() / 1000) + 10;

		Map<String, List<PvStatusEntity>> deviceName2PvStatusEntities = new HashMap<>();
		for (PvStatusEntity pvStatusEntity : pvStatusEntities) {
			String deviceName = pvStatusEntity.getDeviceName();
			List<PvStatusEntity> list = deviceName2PvStatusEntities.get(deviceName);
			if (list == null) {
				list = new ArrayList<>(listCapacity);
				deviceName2PvStatusEntities.put(deviceName, list);
			}
			list.add(pvStatusEntity);
		}

		List<PvStatusEntity> interpolatedList = new ArrayList<>(listCapacity * deviceName2PvStatusEntities.size());
		for (Map.Entry<String, List<PvStatusEntity>> me : deviceName2PvStatusEntities.entrySet()) {
			String deviceName = me.getKey();
			interpolatedList.addAll(interpolateOneDevice(deviceName, me.getValue(), interval));
		}
		Collections.sort(interpolatedList, (o1, o2) -> {
			Date measured1 = o1.getMeasured();
			Date measured2 = o2.getMeasured();
			int result = measured1.compareTo(measured2);
			if (result != 0)
				return result;

			result = o1.getDeviceName().compareTo(o2.getDeviceName());
			return result;
		});
		return interpolatedList;
	}

	protected List<PvStatusEntity> interpolateOneDevice(final String deviceName, final List<PvStatusEntity> pvStatusEntities, TimeInterval interval) {
		requireNonNull(deviceName, "deviceName");
		requireNonNull(pvStatusEntities, "pvStatusEntities");

		int targetSize = (int) (interval.getLengthMillis() / THOUSAND_MILLIS);

		if (pvStatusEntities.size() >= targetSize) {
			if (pvStatusEntities.size() > targetSize)
				logger.warn("pvStatusEntities.size > targetSize :: {} > {}", pvStatusEntities.size(), targetSize);

			return pvStatusEntities;
		}

		SortedMap<Date, List<PvStatusEntityWrapper>> measuredSecond2PvStatusEntityWrappers = new TreeMap<>();
		for (long second = interval.getFromIncl().getTime(); second < interval.getToExcl().getTime(); second += 1000L) {
			Date date = new Date(second);
			measuredSecond2PvStatusEntityWrappers.put(date, new LinkedList<>());
		}

		for (PvStatusEntity pvStatusEntity : pvStatusEntities) {
			PvStatusEntityWrapper wrapper = new PvStatusEntityWrapper(pvStatusEntity);
			List<PvStatusEntityWrapper> list = measuredSecond2PvStatusEntityWrappers.get(wrapper.measuredSecond);
			requireNonNull(list, "measuredSecond2PvStatusEntityWrappers.get(" + wrapper.measuredSecond + ")");
			list.add(wrapper);
		}

		Date emptyMeasuredSecond;
		Date overfullMeasuredSecond;
		while ((emptyMeasuredSecond = getFirstEmptyMeasuredSecond(measuredSecond2PvStatusEntityWrappers)) != null
				&&(overfullMeasuredSecond = getFirstOverfullMeasuredSecond(measuredSecond2PvStatusEntityWrappers)) != null) {
			if (emptyMeasuredSecond.before(overfullMeasuredSecond))
				balanceToLeft(measuredSecond2PvStatusEntityWrappers, emptyMeasuredSecond, overfullMeasuredSecond);
			else
				balanceToRight(measuredSecond2PvStatusEntityWrappers, emptyMeasuredSecond, overfullMeasuredSecond);
		}

		while ((emptyMeasuredSecond = getFirstEmptyMeasuredSecond(measuredSecond2PvStatusEntityWrappers)) != null) {
			PvStatusEntity pvStatusEntity = new PvStatusEntity();
			pvStatusEntity.setMeasured(emptyMeasuredSecond);
			pvStatusEntity.setDeviceName(deviceName);

			PvStatusEntity previousPvStatusEntity = getPreviousPvStatusEntity(measuredSecond2PvStatusEntityWrappers, deviceName, emptyMeasuredSecond);
			PvStatusEntity nextPvStatusEntity = getNextPvStatusEntity(measuredSecond2PvStatusEntityWrappers, deviceName, emptyMeasuredSecond);

			if (previousPvStatusEntity == null && nextPvStatusEntity == null)
				throw new IllegalStateException("previousPvStatusEntity == null && nextPvStatusEntity == null");

			if (previousPvStatusEntity == null)
				previousPvStatusEntity = nextPvStatusEntity;

			if (nextPvStatusEntity == null)
				nextPvStatusEntity = previousPvStatusEntity;

			List<PvStatusEntity> subPvStatusEntities = Arrays.asList(previousPvStatusEntity, nextPvStatusEntity);

			aggregate(subPvStatusEntities, pvStatusEntity);
			List<PvStatusEntityWrapper> list = measuredSecond2PvStatusEntityWrappers.get(emptyMeasuredSecond);
			requireNonNull(list, "measuredSecond2PvStatusEntityWrappers.get(" + emptyMeasuredSecond + ")");
			list.add(new PvStatusEntityWrapper(pvStatusEntity));
			if (list.size() != 1)
				throw new IllegalStateException("list.size() != 1");
		}

		// populate result and do some sanity checks
		List<PvStatusEntity> result = new ArrayList<>(measuredSecond2PvStatusEntityWrappers.size());
		Date lastMeasured = null;
		for (List<PvStatusEntityWrapper> list : measuredSecond2PvStatusEntityWrappers.values()) {
			if (list.size() == 0) // we did interpolate, didn't we? why should there be an empty list?!???
				throw new IllegalStateException("list.size() == 0");

			if (list.size() > 1) // we don't interpolate at all, if we have more entities than seconds ;-)
				throw new IllegalStateException("list.size() > 1");

			for (PvStatusEntityWrapper pvStatusEntityWrapper : list) {
				if (lastMeasured == null)
					lastMeasured = pvStatusEntityWrapper.pvStatusEntity.getMeasured();
				else if (lastMeasured.after(pvStatusEntityWrapper.pvStatusEntity.getMeasured()))
					throw new IllegalStateException("lastMeasured > pvStatusEntityWrapper.pvStatusEntity.measured");

				result.add(pvStatusEntityWrapper.pvStatusEntity);
			}
		}
		return result;
	}

	private PvStatusEntity getPreviousPvStatusEntity(
			final SortedMap<Date, List<PvStatusEntityWrapper>> measuredSecond2PvStatusEntityWrappers,
			final String deviceName,
			final Date emptyMeasuredSecond) {
		requireNonNull(measuredSecond2PvStatusEntityWrappers, "measuredSecond2PvStatusEntityWrappers");
		requireNonNull(deviceName, "deviceName");
		requireNonNull(emptyMeasuredSecond, "emptyMeasuredSecond");
		Date measuredSecond = emptyMeasuredSecond;
		while (true) {
			List<PvStatusEntityWrapper> list = measuredSecond2PvStatusEntityWrappers.get(measuredSecond);
			if (list == null)
				break; // out of time-range

			if (! list.isEmpty())
				return list.get(list.size() - 1).pvStatusEntity;

			measuredSecond = new Date(measuredSecond.getTime() - THOUSAND_MILLIS);
		}
		return getTransactionOrFail().getDao(PvStatusDao.class).getLastPvStatusEntityMeasuredBefore(deviceName, emptyMeasuredSecond);
	}

	private PvStatusEntity getNextPvStatusEntity(
			final SortedMap<Date, List<PvStatusEntityWrapper>> measuredSecond2PvStatusEntityWrappers,
			final String deviceName,
			final Date emptyMeasuredSecond) {
		requireNonNull(measuredSecond2PvStatusEntityWrappers, "measuredSecond2PvStatusEntityWrappers");
		requireNonNull(deviceName, "deviceName");
		requireNonNull(emptyMeasuredSecond, "emptyMeasuredSecond");
		Date measuredSecond = emptyMeasuredSecond;
		while (true) {
			List<PvStatusEntityWrapper> list = measuredSecond2PvStatusEntityWrappers.get(measuredSecond);
			if (list == null)
				break; // out of time-range

			if (! list.isEmpty())
				return list.get(0).pvStatusEntity;

			measuredSecond = new Date(measuredSecond.getTime() + THOUSAND_MILLIS);
		}
		return getTransactionOrFail().getDao(PvStatusDao.class).getFirstPvStatusEntityMeasuredAfter(deviceName, emptyMeasuredSecond);
	}

	private void balanceToLeft(SortedMap<Date, List<PvStatusEntityWrapper>> measuredSecond2PvStatusEntityWrappers,
			Date emptyMeasuredSecond, Date overfullMeasuredSecond) {
		Date measuredSecond = overfullMeasuredSecond;
		while (emptyMeasuredSecond.before(measuredSecond)) {
			Date nextMeasuredSecond = new Date(measuredSecond.getTime() - THOUSAND_MILLIS);
			List<PvStatusEntityWrapper> listFrom = measuredSecond2PvStatusEntityWrappers.get(measuredSecond);
			List<PvStatusEntityWrapper> listTo = measuredSecond2PvStatusEntityWrappers.get(nextMeasuredSecond);
			listTo.add(listFrom.remove(0));
			if (listTo.size() == 1)
				return;

			measuredSecond = nextMeasuredSecond;
		}
	}

	private void balanceToRight(SortedMap<Date, List<PvStatusEntityWrapper>> measuredSecond2PvStatusEntityWrappers,
			Date emptyMeasuredSecond, Date overfullMeasuredSecond) {
		Date measuredSecond = overfullMeasuredSecond;
		while (emptyMeasuredSecond.after(measuredSecond)) {
			Date nextMeasuredSecond = new Date(measuredSecond.getTime() + THOUSAND_MILLIS);
			List<PvStatusEntityWrapper> listFrom = measuredSecond2PvStatusEntityWrappers.get(measuredSecond);
			List<PvStatusEntityWrapper> listTo = measuredSecond2PvStatusEntityWrappers.get(nextMeasuredSecond);
			listTo.add(0, listFrom.remove(listFrom.size() - 1));
			if (listTo.size() == 1)
				return;

			measuredSecond = nextMeasuredSecond;
		}
	}

	private Date getFirstEmptyMeasuredSecond(SortedMap<Date, List<PvStatusEntityWrapper>> measuredSecond2PvStatusEntityWrappers) {
		for (Map.Entry<Date, List<PvStatusEntityWrapper>> me : measuredSecond2PvStatusEntityWrappers.entrySet()) {
			if (me.getValue().isEmpty())
				return me.getKey();
		}
		return null;
	}

	private Date getFirstOverfullMeasuredSecond(SortedMap<Date, List<PvStatusEntityWrapper>> measuredSecond2PvStatusEntityWrappers) {
		for (Map.Entry<Date, List<PvStatusEntityWrapper>> me : measuredSecond2PvStatusEntityWrappers.entrySet()) {
			if (me.getValue().size() > 1)
				return me.getKey();
		}
		return null;
	}

	@Override
	public void aggregate(List<PvStatusEntity> pvStatusEntities) {
		// We abuse the logic of the PvStatusAggregator in this interpolator -- hence we prohibit using the original API method.
		throw new UnsupportedOperationException();
	}

	@Override
	protected void persistAggregatedPvStatus(PvStatusEntity aggregatedPvStatus) {
		requireNonNull(aggregatedPvStatus, "aggregatedPvStatus");

		// We do *NOT* persist them. Instead we just keep and *use* this result.
		if (lastAggregatedPvStatus != null)
			throw new IllegalStateException("lastAggregatedPvStatus != null");

		lastAggregatedPvStatus = aggregatedPvStatus;
	}

	@Override
	protected SortedMap<TimeInterval, Map<String, List<PvStatusEntity>>> split(List<PvStatusEntity> pvStatusEntities) {
		requireNonNull(pvStatusEntities, "pvStatusEntities");
		if (pvStatusEntities.size() != 2)
			throw new IllegalArgumentException("pvStatusEntities.size() != 2");

		SortedMap<TimeInterval, Map<String, List<PvStatusEntity>>> result = new TreeMap<>();

		return result;
	}

	@Override
	public TimeInterval getTimeInterval(Date timestamp) {
		throw new UnsupportedOperationException();
	}

	public static class PvStatusEntityWrapper {

		public final PvStatusEntity pvStatusEntity;
		public final Date measuredSecond;

		public PvStatusEntityWrapper(PvStatusEntity pvStatusEntity) {
			this.pvStatusEntity = requireNonNull(pvStatusEntity, "pvStatusEntity");
			this.measuredSecond = getMeasuredSecond(pvStatusEntity.getMeasured());
		}

		private static Date getMeasuredSecond(Date measured) {
			GregorianCalendar cal = new GregorianCalendar();
			cal.setTime(measured);
			cal.set(Calendar.MILLISECOND, 0);
			return cal.getTime();
		}

		@Override
		public String toString() {
			return "PvStatusEntityWrapper["
					+ "measuredSecond=" + measuredSecond
					+ ",pvStatusEntity.measured=" + pvStatusEntity.getMeasured()
					+ ",pvStatusEntity.deviceName=" + pvStatusEntity.getDeviceName()
					+ "]";
		}
	}

}
