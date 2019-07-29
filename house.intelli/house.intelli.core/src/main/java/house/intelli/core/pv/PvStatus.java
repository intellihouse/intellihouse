package house.intelli.core.pv;

import java.util.Date;

/**
 *
 * @author mn
 */
public interface PvStatus {

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

	void setMeasured(Date measured);

	/**
	 * When was the measurement done?
	 * @return the timestamp of when the data was measured.
	 */
	Date getMeasured();

	void setDeviceName(String deviceName);

	String getDeviceName();

}
