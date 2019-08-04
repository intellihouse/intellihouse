package house.intelli.core.pv;

import java.util.Date;

/**
 *
 * @author mn
 */
public interface PvStatus {

	/**
	 * When was the measurement done?
	 * @return the timestamp of when the data was measured.
	 */
	Date getMeasured();

	void setMeasured(Date measured);

	void setDeviceName(String deviceName);

	String getDeviceName();

	/**
	 * How many milliseconds are covered by this instance?
	 * <p>
	 * If this entity originates directly from a measurement, the time covered by this entity is
	 * from >>>{@link #getMeasured() measured}{@code  - }{@code coveredPeriodMillis}<<< <i>excluding</i>
	 * until >>>{@link #getMeasured() measured}<<< <i>including</i>.
	 * <p>
	 * If this method returns 0, it means the time covered is unknown and must be determined from
	 * other data (e.g. the previous entity measured before).
	 *
	 * @return how many milliseconds were aggregated by this instance?
	 */
	int getCoveredPeriodMillis();

	void setPvPower(float pvPower);

	float getPvPower();

	void setEepromVersion(int eepromVersion);

	int getEepromVersion();

	void setStatusBitmask(int statusBitmask);

	int getStatusBitmask();

	void setBatteryDischargeCurrent(float batteryDischargeCurrent);

	float getBatteryDischargeCurrent();

	void setBatteryVoltageAtCharger(float batteryVoltageAtCharger);

	float getBatteryVoltageAtCharger();

	void setPvVoltage(float pvVoltage);

	float getPvVoltage();

	void setPvToBatteryCurrent(float pvToBatteryCurrent);

	float getPvToBatteryCurrent();

	void setHeatSinkTemperature(float heatSinkTemperature);

	float getHeatSinkTemperature();

	void setBatteryCapacityPercentage(float batteryCapacityPercentage);

	float getBatteryCapacityPercentage();

	void setBatteryChargeCurrent(float batteryChargeCurrent);

	float getBatteryChargeCurrent();

	void setBatteryVoltageAtInverter(float batteryVoltageAtInverter);

	float getBatteryVoltageAtInverter();

	void setInternalBusVoltage(float internalBusVoltage);

	float getInternalBusVoltage();

	void setAcOutLoadPercentage(float acOutLoadPercentage);

	float getAcOutLoadPercentage();

	void setAcOutActivePower(float acOutActivePower);

	float getAcOutActivePower();

	void setAcOutApparentPower(float acOutApparentPower);

	float getAcOutApparentPower();

	void setAcOutFrequency(float acOutFrequency);

	float getAcOutFrequency();

	void setAcOutVoltage(float acOutVoltage);

	float getAcOutVoltage();

	void setAcInFrequency(float acInFrequency);

	float getAcInFrequency();

	void setAcInVoltage(float acInVoltage);

	float getAcInVoltage();

	void setDeviceMode(String deviceMode);

	String getDeviceMode();

}
