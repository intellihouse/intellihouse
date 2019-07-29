package house.intelli.jdo.model;

import java.util.Date;

import javax.jdo.annotations.Inheritance;
import javax.jdo.annotations.InheritanceStrategy;
import javax.jdo.annotations.NullValue;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.Queries;
import javax.jdo.annotations.Query;
import javax.jdo.annotations.Unique;

import house.intelli.core.pv.AggregatedPvStatus;
import house.intelli.jdo.Entity;

@PersistenceCapable(table = "PvStatusMinute")
@Inheritance(strategy = InheritanceStrategy.NEW_TABLE)
@Unique(name = "PvStatusMinuteEntity_deviceName_measured", members = {"deviceName", "measured"})
@Queries({
	@Query(name = "getPvStatusMinuteEntity_deviceName_measured", value = "SELECT UNIQUE WHERE this.deviceName == :deviceName && this.measured == :measured")
})
public class PvStatusMinuteEntity extends Entity implements AggregatedPvStatus {

	public static final int AGGREGATE_PERIOD_MILLIS = 60_000;

	@Persistent(nullValue = NullValue.EXCEPTION)
	private String deviceName;

	@Persistent(nullValue = NullValue.EXCEPTION)
	private Date measured;

	@Persistent(nullValue = NullValue.EXCEPTION)
	private String deviceMode;

	private float acInVoltage;
	private float acInFrequency;
	private float acOutVoltage;
	private float acOutFrequency;
	private float acOutApparentPower; // Scheinleistung
	private float acOutActivePower; // Wirkleistung
	private float acOutLoadPercentage; // based on apparent power
	private float internalBusVoltage;

	/**
	 * Batteriespannung vom Wechselrichter gemessen
	 */
	private float batteryVoltageAtInverter;

	/**
	 * Batterie-Ladestrom
	 */
	private float batteryChargeCurrent;

	/**
	 * Batterie-Kapazität (ca.)
	 */
	private float batteryCapacityPercentage;

	/**
	 * Kühlkörper-Temperatur
	 */
	private float heatSinkTemperature;

	/**
	 * PV Eingangsstrom (batterieseitig)
	 */
	private float pvToBatteryCurrent;

	/**
	 * PV Spannung
	 */
	private float pvVoltage;

	/**
	 * Batteriespannung vom Laderegler gemessen
	 */
	private float batteryVoltageAtCharger;

	/**
	 * Batterie-Entladestrom
	 */
	private float batteryDischargeCurrent;

	/**
	 * Gerätestatus
	 */
	private int statusBitmask;

	/**
	 * EEPROM Version
	 */
	private int eepromVersion;

	/**
	 * PV Ladeleistung
	 */
	private float pvPower;

	public PvStatusMinuteEntity() {
	}

	@Override
	public String getDeviceName() {
		return deviceName;
	}
	@Override
	public void setDeviceName(String deviceName) {
		this.deviceName = deviceName;
	}

	@Override
	public Date getMeasured() {
		return measured;
	}
	@Override
	public void setMeasured(Date measured) {
		this.measured = measured;
	}

	/**
	 * How many milliseconds was measured? This instance is an aggregate
	 * @return
	 */
	@Override
	public int getAggregatePeriodMillis() {
		return AGGREGATE_PERIOD_MILLIS;
	}

	@Override
	public String getDeviceMode() {
		return deviceMode;
	}
	@Override
	public void setDeviceMode(String deviceMode) {
		this.deviceMode = deviceMode;
	}

	@Override
	public float getAcInVoltage() {
		return acInVoltage;
	}
	@Override
	public void setAcInVoltage(float acInVoltage) {
		this.acInVoltage = acInVoltage;
	}

	@Override
	public float getAcInFrequency() {
		return acInFrequency;
	}
	@Override
	public void setAcInFrequency(float acInFrequency) {
		this.acInFrequency = acInFrequency;
	}

	@Override
	public float getAcOutVoltage() {
		return acOutVoltage;
	}
	@Override
	public void setAcOutVoltage(float acOutVoltage) {
		this.acOutVoltage = acOutVoltage;
	}

	@Override
	public float getAcOutFrequency() {
		return acOutFrequency;
	}
	@Override
	public void setAcOutFrequency(float acOutFrequency) {
		this.acOutFrequency = acOutFrequency;
	}

	@Override
	public float getAcOutApparentPower() {
		return acOutApparentPower;
	}
	@Override
	public void setAcOutApparentPower(float acOutApparentPower) {
		this.acOutApparentPower = acOutApparentPower;
	}

	@Override
	public float getAcOutActivePower() {
		return acOutActivePower;
	}
	@Override
	public void setAcOutActivePower(float acOutActivePower) {
		this.acOutActivePower = acOutActivePower;
	}

	@Override
	public float getAcOutLoadPercentage() {
		return acOutLoadPercentage;
	}
	@Override
	public void setAcOutLoadPercentage(float acOutLoadPercentage) {
		this.acOutLoadPercentage = acOutLoadPercentage;
	}

	@Override
	public float getInternalBusVoltage() {
		return internalBusVoltage;
	}
	@Override
	public void setInternalBusVoltage(float internalBusVoltage) {
		this.internalBusVoltage = internalBusVoltage;
	}
	@Override
	public float getBatteryVoltageAtInverter() {
		return batteryVoltageAtInverter;
	}

	@Override
	public void setBatteryVoltageAtInverter(float batteryVoltageAtInverter) {
		this.batteryVoltageAtInverter = batteryVoltageAtInverter;
	}
	@Override
	public float getBatteryChargeCurrent() {
		return batteryChargeCurrent;
	}
	@Override
	public void setBatteryChargeCurrent(float batteryChargeCurrent) {
		this.batteryChargeCurrent = batteryChargeCurrent;
	}

	@Override
	public float getBatteryCapacityPercentage() {
		return batteryCapacityPercentage;
	}
	@Override
	public void setBatteryCapacityPercentage(float batteryCapacityPercentage) {
		this.batteryCapacityPercentage = batteryCapacityPercentage;
	}

	@Override
	public float getHeatSinkTemperature() {
		return heatSinkTemperature;
	}
	@Override
	public void setHeatSinkTemperature(float heatSinkTemperature) {
		this.heatSinkTemperature = heatSinkTemperature;
	}

	@Override
	public float getPvToBatteryCurrent() {
		return pvToBatteryCurrent;
	}
	@Override
	public void setPvToBatteryCurrent(float pvToBatteryCurrent) {
		this.pvToBatteryCurrent = pvToBatteryCurrent;
	}

	@Override
	public float getPvVoltage() {
		return pvVoltage;
	}
	@Override
	public void setPvVoltage(float pvVoltage) {
		this.pvVoltage = pvVoltage;
	}

	@Override
	public float getBatteryVoltageAtCharger() {
		return batteryVoltageAtCharger;
	}
	@Override
	public void setBatteryVoltageAtCharger(float batteryVoltageAtCharger) {
		this.batteryVoltageAtCharger = batteryVoltageAtCharger;
	}

	@Override
	public float getBatteryDischargeCurrent() {
		return batteryDischargeCurrent;
	}
	@Override
	public void setBatteryDischargeCurrent(float batteryDischargeCurrent) {
		this.batteryDischargeCurrent = batteryDischargeCurrent;
	}

	@Override
	public int getStatusBitmask() {
		return statusBitmask;
	}
	@Override
	public void setStatusBitmask(int statusBitmask) {
		this.statusBitmask = statusBitmask;
	}

	@Override
	public int getEepromVersion() {
		return eepromVersion;
	}
	@Override
	public void setEepromVersion(int eepromVersion) {
		this.eepromVersion = eepromVersion;
	}

	@Override
	public float getPvPower() {
		return pvPower;
	}
	@Override
	public void setPvPower(float pvPower) {
		this.pvPower = pvPower;
	}

	@Override
	public String toString() {
		return "PvStatusMinuteEntity [deviceName=" + deviceName + ", measured=" + measured
				+ ", deviceMode=" + deviceMode + ", acInVoltage=" + acInVoltage + ", acInFrequency=" + acInFrequency
				+ ", acOutVoltage=" + acOutVoltage + ", acOutFrequency=" + acOutFrequency + ", acOutApparentPower=" + acOutApparentPower
				+ ", acOutActivePower=" + acOutActivePower + ", acOutLoadPercentage=" + acOutLoadPercentage
				+ ", internalBusVoltage=" + internalBusVoltage + ", batteryVoltageAtInverter=" + batteryVoltageAtInverter
				+ ", batteryChargeCurrent=" + batteryChargeCurrent + ", batteryCapacityPercentage=" + batteryCapacityPercentage
				+ ", heatSinkTemperature=" + heatSinkTemperature + ", pvToBatteryCurrent=" + pvToBatteryCurrent + ", pvVoltage="
				+ pvVoltage + ", batteryVoltageAtCharger=" + batteryVoltageAtCharger + ", batteryDischargeCurrent="
				+ batteryDischargeCurrent + ", statusBitmask=" + statusBitmask + ", eepromVersion=" + eepromVersion
				+ ", pvPower=" + pvPower + "]";
	}

}
