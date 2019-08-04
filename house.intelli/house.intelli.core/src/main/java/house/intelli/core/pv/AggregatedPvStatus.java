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
	 * {@inheritDoc}
	 * <p>
	 * The time covered by this <i>aggregation</i>-entity is from >>>{@link #getMeasured() measured}<<< <i>including</i>
	 * until >>>{@link #getMeasured() measured}{@code  + }{@code coveredPeriodMillis}<<< <i>excluding</i>.
	 * <p>
	 * So in contrast to a <i>measured</i> entity, whose time-period is directly <i>before</i> (and including) the
	 * {@link #getMeasured() measured} timestamp, the aggregated entity has its time-period directly <i>after</i>
	 * (and including) the {@code measured} timestamp.
	 * @return how many milliseconds were aggregated by this instance?
	 */
	@Override
	int getCoveredPeriodMillis();

	int getInputCountInterpolated();

	void setInputCountInterpolated(int inputCountInterpolated);

	int getInputCountMeasured();

	void setInputCountMeasured(int inputCountMeasured);

	float getAcInVoltageMin();

	void setAcInVoltageMin(float acInVoltageMin);

	float getAcInFrequencyMin();

	void setAcInFrequencyMin(float acInFrequencyMin);

	float getAcOutVoltageMin();

	void setAcOutVoltageMin(float acOutVoltageMin);

	float getAcOutFrequencyMin();

	void setAcOutFrequencyMin(float acOutFrequencyMin);

	float getAcOutApparentPowerMin();

	void setAcOutApparentPowerMin(float acOutApparentPowerMin);

	float getAcOutActivePowerMin();

	void setAcOutActivePowerMin(float acOutActivePowerMin);

	float getAcOutLoadPercentageMin();

	void setAcOutLoadPercentageMin(float acOutLoadPercentageMin);

	float getInternalBusVoltageMin();

	void setInternalBusVoltageMin(float internalBusVoltageMin);

	float getBatteryVoltageAtInverterMin();

	void setBatteryVoltageAtInverterMin(float batteryVoltageAtInverterMin);

	float getBatteryChargeCurrentMin();

	void setBatteryChargeCurrentMin(float batteryChargeCurrentMin);

	float getBatteryCapacityPercentageMin();

	void setBatteryCapacityPercentageMin(float batteryCapacityPercentageMin);

	float getHeatSinkTemperatureMin();

	void setHeatSinkTemperatureMin(float heatSinkTemperatureMin);

	float getPvToBatteryCurrentMin();

	void setPvToBatteryCurrentMin(float pvToBatteryCurrentMin);

	float getPvVoltageMin();

	void setPvVoltageMin(float pvVoltageMin);

	float getBatteryVoltageAtChargerMin();

	void setBatteryVoltageAtChargerMin(float batteryVoltageAtChargerMin);

	float getBatteryDischargeCurrentMin();

	void setBatteryDischargeCurrentMin(float batteryDischargeCurrentMin);

	float getPvPowerMin();

	void setPvPowerMin(float pvPowerMin);

	float getAcInVoltageMax();

	void setAcInVoltageMax(float acInVoltageMax);

	float getAcInFrequencyMax();

	void setAcInFrequencyMax(float acInFrequencyMax);

	float getAcOutVoltageMax();

	void setAcOutVoltageMax(float acOutVoltageMax);

	float getAcOutFrequencyMax();

	void setAcOutFrequencyMax(float acOutFrequencyMax);

	float getAcOutApparentPowerMax();

	void setAcOutApparentPowerMax(float acOutApparentPowerMax);

	float getAcOutActivePowerMax();

	void setAcOutActivePowerMax(float acOutActivePowerMax);

	float getAcOutLoadPercentageMax();

	void setAcOutLoadPercentageMax(float acOutLoadPercentageMax);

	float getInternalBusVoltageMax();

	void setInternalBusVoltageMax(float internalBusVoltageMax);

	float getBatteryVoltageAtInverterMax();

	void setBatteryVoltageAtInverterMax(float batteryVoltageAtInverterMax);

	float getBatteryChargeCurrentMax();

	void setBatteryChargeCurrentMax(float batteryChargeCurrentMax);

	float getBatteryCapacityPercentageMax();

	void setBatteryCapacityPercentageMax(float batteryCapacityPercentageMax);

	float getHeatSinkTemperatureMax();

	void setHeatSinkTemperatureMax(float heatSinkTemperatureMax);

	float getPvToBatteryCurrentMax();

	void setPvToBatteryCurrentMax(float pvToBatteryCurrentMax);

	float getPvVoltageMax();

	void setPvVoltageMax(float pvVoltageMax);

	float getBatteryVoltageAtChargerMax();

	void setBatteryVoltageAtChargerMax(float batteryVoltageAtChargerMax);

	float getBatteryDischargeCurrentMax();

	void setBatteryDischargeCurrentMax(float batteryDischargeCurrentMax);

	float getPvPowerMax();

	void setPvPowerMax(float pvPowerMax);
}
