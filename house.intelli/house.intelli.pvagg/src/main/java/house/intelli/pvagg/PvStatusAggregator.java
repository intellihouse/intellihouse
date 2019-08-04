package house.intelli.pvagg;

import static java.util.Objects.*;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import house.intelli.core.TimeInterval;
import house.intelli.core.pv.AggregatedPvStatus;
import house.intelli.core.pv.PvStatus;
import house.intelli.jdo.IntelliHouseTransaction;
import house.intelli.jdo.model.PvStatusEntity;

public abstract class PvStatusAggregator<A extends PvStatus> {

	private IntelliHouseTransaction transaction;

	protected abstract A createAggregatedPvStatusEntity();

	protected abstract A getAggregatedPvStatus(String deviceName, Date measured);

	protected abstract void persistAggregatedPvStatus(A aggregatedPvStatus);

	private static final Map<String, AggregateType> propertyName2AggregateType = new HashMap<>();
	private static final Map<String, AggregateSource> propertyName2AggregateSource = new HashMap<>();

	protected final Map<String, PropertyDescriptor> sourcePropertyName2PropertyDescriptor;

	static {
		propertyName2AggregateType.put("class", AggregateType.NONE);
		propertyName2AggregateType.put("id", AggregateType.NONE);
		propertyName2AggregateType.put("deviceName", AggregateType.NONE);
		propertyName2AggregateType.put("measured", AggregateType.NONE);
		propertyName2AggregateType.put("inputCountInterpolated", AggregateType.NONE);
		propertyName2AggregateType.put("inputCountMeasured", AggregateType.NONE);
		propertyName2AggregateType.put("created", AggregateType.NONE);
		propertyName2AggregateType.put("changed", AggregateType.NONE);
		propertyName2AggregateType.put("coveredPeriodMillis", AggregateType.NONE);

		propertyName2AggregateType.put("deviceMode", AggregateType.MAX); // 'L' > 'B' : We want it to show the worst case.

		propertyName2AggregateType.put("acInVoltage", AggregateType.AVERAGE);
		propertyName2AggregateType.put("acInFrequency", AggregateType.AVERAGE);
		propertyName2AggregateType.put("acOutVoltage", AggregateType.AVERAGE);
		propertyName2AggregateType.put("acOutFrequency", AggregateType.AVERAGE);
		propertyName2AggregateType.put("acOutApparentPower", AggregateType.AVERAGE);
		propertyName2AggregateType.put("acOutActivePower", AggregateType.AVERAGE);
		propertyName2AggregateType.put("acOutLoadPercentage", AggregateType.AVERAGE);
		propertyName2AggregateType.put("internalBusVoltage", AggregateType.AVERAGE);
		propertyName2AggregateType.put("batteryVoltageAtInverter", AggregateType.AVERAGE);
		propertyName2AggregateType.put("batteryChargeCurrent", AggregateType.AVERAGE);
		propertyName2AggregateType.put("batteryCapacityPercentage", AggregateType.AVERAGE);
		propertyName2AggregateType.put("heatSinkTemperature", AggregateType.AVERAGE);
		propertyName2AggregateType.put("pvToBatteryCurrent", AggregateType.AVERAGE);
		propertyName2AggregateType.put("pvVoltage", AggregateType.AVERAGE);
		propertyName2AggregateType.put("batteryVoltageAtCharger", AggregateType.AVERAGE);
		propertyName2AggregateType.put("batteryDischargeCurrent", AggregateType.AVERAGE);
		propertyName2AggregateType.put("pvPower", AggregateType.AVERAGE);

		propertyName2AggregateType.put("statusBitmask", AggregateType.LAST);
		propertyName2AggregateType.put("eepromVersion", AggregateType.LAST);

		propertyName2AggregateSource.put("acInVoltageMin", new AggregateSource("acInVoltage", AggregateType.MIN));
		propertyName2AggregateSource.put("acInFrequencyMin", new AggregateSource("acInFrequency", AggregateType.MIN));
		propertyName2AggregateSource.put("acOutVoltageMin", new AggregateSource("acOutVoltage", AggregateType.MIN));
		propertyName2AggregateSource.put("acOutFrequencyMin", new AggregateSource("acOutFrequency", AggregateType.MIN));
		propertyName2AggregateSource.put("acOutApparentPowerMin", new AggregateSource("acOutApparentPower", AggregateType.MIN));
		propertyName2AggregateSource.put("acOutActivePowerMin", new AggregateSource("acOutActivePower", AggregateType.MIN));
		propertyName2AggregateSource.put("acOutLoadPercentageMin", new AggregateSource("acOutLoadPercentage", AggregateType.MIN));
		propertyName2AggregateSource.put("internalBusVoltageMin", new AggregateSource("internalBusVoltage", AggregateType.MIN));
		propertyName2AggregateSource.put("batteryVoltageAtInverterMin", new AggregateSource("batteryVoltageAtInverter", AggregateType.MIN));
		propertyName2AggregateSource.put("batteryChargeCurrentMin", new AggregateSource("batteryChargeCurrent", AggregateType.MIN));
		propertyName2AggregateSource.put("batteryCapacityPercentageMin", new AggregateSource("batteryCapacityPercentage", AggregateType.MIN));
		propertyName2AggregateSource.put("heatSinkTemperatureMin", new AggregateSource("heatSinkTemperature", AggregateType.MIN));
		propertyName2AggregateSource.put("pvToBatteryCurrentMin", new AggregateSource("pvToBatteryCurrent", AggregateType.MIN));
		propertyName2AggregateSource.put("pvVoltageMin", new AggregateSource("pvVoltage", AggregateType.MIN));
		propertyName2AggregateSource.put("batteryVoltageAtChargerMin", new AggregateSource("batteryVoltageAtCharger", AggregateType.MIN));
		propertyName2AggregateSource.put("batteryDischargeCurrentMin", new AggregateSource("batteryDischargeCurrent", AggregateType.MIN));
		propertyName2AggregateSource.put("pvPowerMin", new AggregateSource("pvPower", AggregateType.MIN));

		propertyName2AggregateSource.put("acInVoltageMax", new AggregateSource("acInVoltage", AggregateType.MAX));
		propertyName2AggregateSource.put("acInFrequencyMax", new AggregateSource("acInFrequency", AggregateType.MAX));
		propertyName2AggregateSource.put("acOutVoltageMax", new AggregateSource("acOutVoltage", AggregateType.MAX));
		propertyName2AggregateSource.put("acOutFrequencyMax", new AggregateSource("acOutFrequency", AggregateType.MAX));
		propertyName2AggregateSource.put("acOutApparentPowerMax", new AggregateSource("acOutApparentPower", AggregateType.MAX));
		propertyName2AggregateSource.put("acOutActivePowerMax", new AggregateSource("acOutActivePower", AggregateType.MAX));
		propertyName2AggregateSource.put("acOutLoadPercentageMax", new AggregateSource("acOutLoadPercentage", AggregateType.MAX));
		propertyName2AggregateSource.put("internalBusVoltageMax", new AggregateSource("internalBusVoltage", AggregateType.MAX));
		propertyName2AggregateSource.put("batteryVoltageAtInverterMax", new AggregateSource("batteryVoltageAtInverter", AggregateType.MAX));
		propertyName2AggregateSource.put("batteryChargeCurrentMax", new AggregateSource("batteryChargeCurrent", AggregateType.MAX));
		propertyName2AggregateSource.put("batteryCapacityPercentageMax", new AggregateSource("batteryCapacityPercentage", AggregateType.MAX));
		propertyName2AggregateSource.put("heatSinkTemperatureMax", new AggregateSource("heatSinkTemperature", AggregateType.MAX));
		propertyName2AggregateSource.put("pvToBatteryCurrentMax", new AggregateSource("pvToBatteryCurrent", AggregateType.MAX));
		propertyName2AggregateSource.put("pvVoltageMax", new AggregateSource("pvVoltage", AggregateType.MAX));
		propertyName2AggregateSource.put("batteryVoltageAtChargerMax", new AggregateSource("batteryVoltageAtCharger", AggregateType.MAX));
		propertyName2AggregateSource.put("batteryDischargeCurrentMax", new AggregateSource("batteryDischargeCurrent", AggregateType.MAX));
		propertyName2AggregateSource.put("pvPowerMax", new AggregateSource("pvPower", AggregateType.MAX));

		// The estimated properties are not aggregated, but estimated ;-)
		propertyName2AggregateType.put("estBatteryChargeEnergyIdeal", AggregateType.NONE);
		propertyName2AggregateType.put("estBatteryChargeEnergyReal", AggregateType.NONE);
		propertyName2AggregateType.put("estBatteryEnergyCapacity", AggregateType.NONE);
		propertyName2AggregateType.put("estBatteryEnergyLevel", AggregateType.NONE);
	}

	public PvStatusAggregator() {
		sourcePropertyName2PropertyDescriptor = getPropertyName2PropertyDescriptorMap(PvStatusEntity.class);
	}

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

	public void aggregate(final List<PvStatusEntity> pvStatusEntities) {
		SortedMap<TimeInterval, Map<String, List<PvStatusEntity>>> timeInterval2DeviceName2PvStatusEntities = split(pvStatusEntities);
		for (Map.Entry<TimeInterval, Map<String, List<PvStatusEntity>>> me1 : timeInterval2DeviceName2PvStatusEntities.entrySet()) {
			TimeInterval timeInterval = me1.getKey();
			Map<String, List<PvStatusEntity>> deviceName2PvStatusEntities = me1.getValue();
			for (Map.Entry<String, List<PvStatusEntity>> me2 : deviceName2PvStatusEntities.entrySet()) {
				String deviceName = me2.getKey();
				List<PvStatusEntity> subPvStatusEntities = me2.getValue();
				if (subPvStatusEntities.isEmpty())
					throw new IllegalStateException("subPvStatusEntities.isEmpty()");

				A aggregatedPvStatus = getAggregatedPvStatus(deviceName, timeInterval.getFromIncl());
				if (aggregatedPvStatus == null)
					aggregatedPvStatus = createAggregatedPvStatusEntity();

				aggregatedPvStatus.setMeasured(timeInterval.getFromIncl());
				aggregatedPvStatus.setDeviceName(deviceName);

				if (aggregatedPvStatus instanceof AggregatedPvStatus) {
					AggregatedPvStatus aps = (AggregatedPvStatus) aggregatedPvStatus;

					int inputCountInterpolated = 0;
					int inputCountMeasured = 0;
					for (PvStatusEntity pvStatusEntity : subPvStatusEntities) {
						if (pvStatusEntity.getId() < 0)
							++inputCountInterpolated;
						else
							++inputCountMeasured;
					}

					aps.setInputCountInterpolated(inputCountInterpolated);
					aps.setInputCountMeasured(inputCountMeasured);
				}

				aggregate(subPvStatusEntities, aggregatedPvStatus);

				persistAggregatedPvStatus(aggregatedPvStatus);
			}
		}
	}

	protected void aggregate(List<PvStatusEntity> subPvStatusEntities, A aggregatedPvStatus) {
		requireNonNull(subPvStatusEntities, "subPvStatusEntities");
		requireNonNull(aggregatedPvStatus, "aggregatedPvStatus");

		BeanInfo targetBeanInfo;
		try {
			targetBeanInfo = Introspector.getBeanInfo(aggregatedPvStatus.getClass());
		} catch (IntrospectionException e) {
			throw new RuntimeException(e);
		}

		for (PropertyDescriptor targetPropertyDescriptor : targetBeanInfo.getPropertyDescriptors()) {
			String targetPropertyName = targetPropertyDescriptor.getName();
			AggregateSource aggregateSource = getAggregateSource(targetPropertyName);
			requireNonNull(aggregateSource, "getAggregateSource(\"" + targetPropertyName + "\")");
			if (AggregateType.NONE == aggregateSource.getAggregateType())
				continue;

			String sourcePropertyName = aggregateSource.getSourcePropertyName();
			PropertyDescriptor sourcePropertyDescriptor = sourcePropertyName2PropertyDescriptor.get(sourcePropertyName);
			requireNonNull(sourcePropertyDescriptor, "sourcePropertyDescriptor[\"" + sourcePropertyName + "\"]");

			Method sourceReadMethod = sourcePropertyDescriptor.getReadMethod();
			requireNonNull(sourceReadMethod, "sourceReadMethod for sourcePropertyName=\"" + sourcePropertyName + "\"");

			Method targetWriteMethod = targetPropertyDescriptor.getWriteMethod();
			requireNonNull(targetWriteMethod, "targetWriteMethod for targetPropertyName=\"" + targetPropertyName + "\"");

			aggregate(aggregateSource.getAggregateType(), subPvStatusEntities, aggregatedPvStatus, sourceReadMethod, targetWriteMethod);
		}
	}

	protected void aggregate(final AggregateType aggregateType,
			final List<PvStatusEntity> subPvStatusEntities, final A aggregatedPvStatus,
			final Method sourceReadMethod, final Method targetWriteMethod) {
		requireNonNull(aggregateType, "aggregateType");
		requireNonNull(subPvStatusEntities, "subPvStatusEntities");
		requireNonNull(aggregatedPvStatus, "aggregatedPvStatus");
		requireNonNull(sourceReadMethod, "sourceReadMethod");
		requireNonNull(targetWriteMethod, "targetWriteMethod");

		switch (aggregateType) {
			case NONE:
				return; // do nothing
			case AVERAGE: {
				double value = aggAverage(subPvStatusEntities, sourceReadMethod);
				writePropertyValueWithConversion(aggregatedPvStatus, targetWriteMethod, value);
				break;
			}
			case MIN: {
				Comparable<?> value = aggMin(subPvStatusEntities, sourceReadMethod);
				writePropertyValue(aggregatedPvStatus, targetWriteMethod, value);
				break;
			}
			case MAX: {
				Comparable<?> value = aggMax(subPvStatusEntities, sourceReadMethod);
				writePropertyValue(aggregatedPvStatus, targetWriteMethod, value);
				break;
			}
			case FIRST: {
				Object value = aggFirst(subPvStatusEntities, sourceReadMethod);
				writePropertyValue(aggregatedPvStatus, targetWriteMethod, value);
				break;
			}
			case LAST: {
				Object value = aggLast(subPvStatusEntities, sourceReadMethod);
				writePropertyValue(aggregatedPvStatus, targetWriteMethod, value);
				break;
			}
			default:
				throw new IllegalStateException("Unknown aggregateType: " + aggregateType);
		}
	}

	protected Map<String, PropertyDescriptor> getPropertyName2PropertyDescriptorMap(final Class<?> beanClass) {
		requireNonNull(beanClass, "beanClass");
		final BeanInfo beanInfo;
		try {
			beanInfo = Introspector.getBeanInfo(beanClass);
		} catch (IntrospectionException e) {
			throw new RuntimeException(e);
		}
		requireNonNull(beanInfo, "beanInfo");
		PropertyDescriptor[] propertyDescriptors = requireNonNull(beanInfo.getPropertyDescriptors(), "beanInfo.getPropertyDescriptors() for beanClass=" + beanClass.getName());
		Map<String, PropertyDescriptor> propertyName2PropertyDescriptor = new HashMap<>(propertyDescriptors.length);
		for (PropertyDescriptor propertyDescriptor : propertyDescriptors) {
			propertyName2PropertyDescriptor.put(propertyDescriptor.getName(), propertyDescriptor);
		}
		return propertyName2PropertyDescriptor;
	}

	protected AggregateSource getAggregateSource(String targetPropertyName) {
		AggregateType aggregateType = propertyName2AggregateType.get(targetPropertyName);
		if (aggregateType != null)
			return new AggregateSource(targetPropertyName, aggregateType);

		return propertyName2AggregateSource.get(targetPropertyName);
	}

	private final Object readPropertyValue(final Object bean, final Method readMethod) {
		requireNonNull(bean, "bean");
		requireNonNull(readMethod, "readMethod");
		try {
			return readMethod.invoke(bean);
		} catch (IllegalAccessException | InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}

	private final void writePropertyValue(final Object bean, final Method writeMethod, final Object value) {
		requireNonNull(bean, "bean");
		requireNonNull(writeMethod, "writeMethod");
		try {
			writeMethod.invoke(bean, value);
		} catch (IllegalAccessException | InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}

	private final void writePropertyValueWithConversion(final Object bean, final Method writeMethod, final Object value) {
		requireNonNull(bean, "bean");
		requireNonNull(writeMethod, "writeMethod");
		final Class<?> targetType = writeMethod.getParameterTypes()[0];
		final Object convertedValue = convert(targetType, value);
		writePropertyValue(bean, writeMethod, convertedValue);
	}

	private final Object convert(final Class<?> targetType, final Object value) {
		if (value == null)
			return value;

		if (targetType.isAssignableFrom(value.getClass()))
			return value;

		if (value instanceof Number) {
			if (float.class.isAssignableFrom(targetType) || Float.class.isAssignableFrom(targetType))
				return ((Number) value).floatValue();

			if (double.class.isAssignableFrom(targetType) || Double.class.isAssignableFrom(targetType))
				return ((Number) value).doubleValue();
		}

		throw new IllegalArgumentException(String.format("Cannot convert value of type %s to targetType %s!", value.getClass().getName(), targetType.getName()));
	}

	private <T extends Comparable<T>> T aggMin(final List<PvStatusEntity> subPvStatusEntities, final Method readMethod) {
		requireNonNull(subPvStatusEntities, "subPvStatusEntities");
		requireNonNull(readMethod, "readMethod");
		T result = null;
		for (PvStatusEntity pvStatusEntity : subPvStatusEntities) {
			@SuppressWarnings("unchecked")
			final T val = (T) readPropertyValue(pvStatusEntity, readMethod);
			requireNonNull(val, "readPropertyValue(...) returned null for " + readMethod);
			if (result == null || result.compareTo(val) > 0)
				result = val;
		}
		return result;
	}

	private <T extends Comparable<T>> T  aggMax(final List<PvStatusEntity> subPvStatusEntities, final Method readMethod) {
		requireNonNull(subPvStatusEntities, "subPvStatusEntities");
		requireNonNull(readMethod, "readMethod");
		T result = null;
		for (PvStatusEntity pvStatusEntity : subPvStatusEntities) {
			@SuppressWarnings("unchecked")
			final T val = (T) readPropertyValue(pvStatusEntity, readMethod);
			requireNonNull(val, "readPropertyValue(...) returned null for " + readMethod);
			if (result == null || result.compareTo(val) < 0)
				result = val;
		}
		return result;
	}

	private double aggAverage(final List<PvStatusEntity> subPvStatusEntities, final Method readMethod) {
		requireNonNull(subPvStatusEntities, "subPvStatusEntities");
		requireNonNull(readMethod, "readMethod");
		double valueSum = 0;

		for (PvStatusEntity pvStatusEntity : subPvStatusEntities) {
			final Object val = readPropertyValue(pvStatusEntity, readMethod);
			requireNonNull(val, "readPropertyValue(...) returned null for " + readMethod);
			valueSum += toDouble(val);
		}

		return valueSum / subPvStatusEntities.size();
	}

	private Object aggFirst(final List<PvStatusEntity> subPvStatusEntities, final Method readMethod) {
		requireNonNull(subPvStatusEntities, "subPvStatusEntities");
		requireNonNull(readMethod, "readMethod");
		final PvStatusEntity pvStatusEntity = subPvStatusEntities.get(0);
		return readPropertyValue(pvStatusEntity, readMethod);
	}

	private Object aggLast(final List<PvStatusEntity> subPvStatusEntities, final Method readMethod) {
		requireNonNull(subPvStatusEntities, "subPvStatusEntities");
		requireNonNull(readMethod, "readMethod");
		final PvStatusEntity pvStatusEntity = subPvStatusEntities.get(subPvStatusEntities.size() - 1);
		return readPropertyValue(pvStatusEntity, readMethod);
	}

	protected double toDouble(final Object value) {
		requireNonNull(value, "value");
		if (value instanceof Number) {
			return ((Number) value).doubleValue();
		}
		throw new IllegalArgumentException("value is not an instance of Number: (" + value.getClass().getName() + ") " + value);
	}

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
