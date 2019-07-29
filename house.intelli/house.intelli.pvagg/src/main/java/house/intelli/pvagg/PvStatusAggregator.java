package house.intelli.pvagg;

import static java.util.Objects.*;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import house.intelli.core.TimeInterval;
import house.intelli.core.pv.AggregatedPvStatus;
import house.intelli.jdo.IntelliHouseTransaction;
import house.intelli.jdo.model.PvStatusEntity;

public abstract class PvStatusAggregator<A extends AggregatedPvStatus> {

	private IntelliHouseTransaction transaction;

	protected abstract A createAggregatedPvStatusEntity();

	protected abstract A getAggregatedPvStatus(String deviceName, Date measured);

	protected abstract void persistAggregatedPvStatus(A aggregatedPvStatus);

	public abstract long getAggregatePeriodMillis();

	/**
	 * Calculate the time-interval which encloses the given {@code timestamp}.
	 * <p>
	 * Does not require a transaction (called outside of a transactional context).
	 *
	 * @param timestamp timestamp for which to calculate the interval. Never <code>null</code>.
	 * @return the interval enclosing the given {@code timestamp}.
	 */
	public abstract TimeInterval getTimeInterval(Date timestamp);

	public IntelliHouseTransaction getTransactionOrFail() {
		return requireNonNull(getTransaction(), "transaction");
	}

	public IntelliHouseTransaction getTransaction() {
		return transaction;
	}

	public void setTransaction(IntelliHouseTransaction transaction) {
		this.transaction = transaction;
	}

	public void aggregate(List<PvStatusEntity> pvStatusEntities) {
		SortedMap<TimeInterval, Map<String, List<PvStatusEntity>>> timeInterval2DeviceName2PvStatusEntities = split(pvStatusEntities);
		for (Map.Entry<TimeInterval, Map<String, List<PvStatusEntity>>> me1 : timeInterval2DeviceName2PvStatusEntities.entrySet()) {
			TimeInterval timeInterval = me1.getKey();
			Map<String, List<PvStatusEntity>> deviceName2PvStatusEntities = me1.getValue();
			for (Map.Entry<String, List<PvStatusEntity>> me2 : deviceName2PvStatusEntities.entrySet()) {
				String deviceName = me2.getKey();
				List<PvStatusEntity> subPvStatusEntities = me2.getValue();
				A aggregatedPvStatus = getAggregatedPvStatus(deviceName, timeInterval.getFromIncl());
				if (aggregatedPvStatus == null)
					aggregatedPvStatus = createAggregatedPvStatusEntity();

				aggregatedPvStatus.setMeasured(timeInterval.getFromIncl());
				aggregatedPvStatus.setDeviceName(deviceName);

				aggregatedPvStatus.setAcInFrequency(average(subPvStatusEntities, PvStatusEntity::getAcInFrequency));
				aggregatedPvStatus.setAcInVoltage(average(subPvStatusEntities, PvStatusEntity::getAcInVoltage));
				aggregatedPvStatus.setAcOutActivePower(average(subPvStatusEntities, PvStatusEntity::getAcOutActivePower));
				aggregatedPvStatus.setAcOutApparentPower(average(subPvStatusEntities, PvStatusEntity::getAcOutApparentPower));
				aggregatedPvStatus.setPvPower(average(subPvStatusEntities, PvStatusEntity::getPvPower));

				persistAggregatedPvStatus(aggregatedPvStatus);
			}
		}
	}

	@FunctionalInterface
	public static interface PvStatusEntityFloatProperty {

		float getPropertyValue(PvStatusEntity pvStatusEntity);

	}

	private float average(List<PvStatusEntity> subPvStatusEntities, PvStatusEntityFloatProperty property) {
		double valueSum = 0;

		for (PvStatusEntity pvStatusEntity : subPvStatusEntities) {
			float value = property.getPropertyValue(pvStatusEntity);
			valueSum += value;
		}

		double result = valueSum / subPvStatusEntities.size();
		return (float) result;
	}

//	protected float average(List<PvStatusEntity> subPvStatusEntities, Function<T, R>)

	protected SortedMap<TimeInterval, Map<String, List<PvStatusEntity>>> split(List<PvStatusEntity> pvStatusEntities) {
		SortedMap<TimeInterval, Map<String, List<PvStatusEntity>>> map = new TreeMap<>();
		for (PvStatusEntity pvStatusEntity : pvStatusEntities) {
			TimeInterval timeInterval = getTimeInterval(pvStatusEntity.getMeasured());
			Map<String, List<PvStatusEntity>> deviceName2List = map.get(timeInterval);
			if (deviceName2List == null) {
				deviceName2List = new TreeMap<>();
				map.put(timeInterval, deviceName2List);
			}
			List<PvStatusEntity> list = deviceName2List.get(pvStatusEntity.getDeviceName());
			if (list == null) {
				list = new LinkedList<>();
				deviceName2List.put(pvStatusEntity.getDeviceName(), list);
			}
			list.add(pvStatusEntity);
		}
		return map;
	}

}
