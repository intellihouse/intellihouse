package house.intelli.jdo.model;

import java.util.Date;

import javax.jdo.annotations.Column;
import javax.jdo.annotations.Inheritance;
import javax.jdo.annotations.InheritanceStrategy;
import javax.jdo.annotations.NullValue;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.Queries;
import javax.jdo.annotations.Query;
import javax.jdo.annotations.Unique;

import house.intelli.core.pv.PvStatus;
import house.intelli.jdo.Entity;

@PersistenceCapable(table = "PvStatus")
@Inheritance(strategy = InheritanceStrategy.NEW_TABLE)
@Unique(name = "PvStatusEntity_deviceName_measured", members = {"deviceName", "measured"})
@Queries({
	@Query(name = "getPvStatusEntity_deviceName_measured",
			value = "SELECT UNIQUE WHERE this.deviceName == :deviceName && this.measured == :measured"),

	@Query(name = "getFirstMeasured", value = "SELECT min(this.measured)"),
	@Query(name = "getFirstMeasuredAfter_id", value = "SELECT min(this.measured) WHERE this.id > :lastAggregatedId"),

	@Query(name = "getPvStatusEntitiesMeasuredBetween_fromIncl_toExcl",
			value = "SELECT WHERE this.measured >= :fromIncl && this.measured < :toExcl ORDER BY this.measured ASC, this.deviceName ASC"),

	@Query(name = "getLastMeasuredBefore_deviceName_measuredToExcl",
			value = "SELECT max(this.measured) WHERE this.deviceName == :deviceName && this.measured < :measuredToExcl"),

	@Query(name = "getFirstMeasuredAfter_deviceName_measuredFromExcl",
			value = "SELECT min(this.measured) WHERE this.deviceName == :deviceName && this.measured > :measuredFromExcl")

})
public class PvStatusEntity extends Entity implements PvStatus {

	public static final int COVERED_PERIOD_MILLIS = house.intelli.core.rpc.pv.PvStatus.COVERED_PERIOD_MILLIS;

	@Persistent(nullValue = NullValue.EXCEPTION)
	private String deviceName;

	@Persistent(nullValue = NullValue.EXCEPTION)
	private Date measured;

	@Persistent(nullValue = NullValue.EXCEPTION)
	private String deviceMode;

	@Column(sqlType = "real")
	private float acInVoltage;
	@Column(sqlType = "real")
	private float acInFrequency;
	@Column(sqlType = "real")
	private float acOutVoltage;
	@Column(sqlType = "real")
	private float acOutFrequency;
	@Column(sqlType = "real")
	private float acOutApparentPower; // Scheinleistung
	@Column(sqlType = "real")
	private float acOutActivePower; // Wirkleistung
	@Column(sqlType = "real")
	private float acOutLoadPercentage; // based on apparent power
	@Column(sqlType = "real")
	private float internalBusVoltage;

	/**
	 * Batteriespannung vom Wechselrichter gemessen
	 */
	@Column(sqlType = "real")
	private float batteryVoltageAtInverter;

	/**
	 * Batterie-Ladestrom
	 */
	@Column(sqlType = "real")
	private float batteryChargeCurrent;

	/**
	 * Batterie-Kapazität (ca.)
	 */
	@Column(sqlType = "real")
	private float batteryCapacityPercentage;

	/**
	 * Kühlkörper-Temperatur
	 */
	@Column(sqlType = "real")
	private float heatSinkTemperature;

	/**
	 * PV Eingangsstrom (batterieseitig)
	 */
	@Column(sqlType = "real")
	private float pvToBatteryCurrent;

	/**
	 * PV Spannung
	 */
	@Column(sqlType = "real")
	private float pvVoltage;

	/**
	 * Batteriespannung vom Laderegler gemessen
	 */
	@Column(sqlType = "real")
	private float batteryVoltageAtCharger;

	/**
	 * Batterie-Entladestrom
	 */
	@Column(sqlType = "real")
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
	@Column(sqlType = "real")
	private float pvPower;

	public PvStatusEntity() {
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
	public int getCoveredPeriodMillis() {
		return COVERED_PERIOD_MILLIS;
	}

	@Override
	public String toString() {
		return "PvStatusEntity [deviceName=" + deviceName + ", measured=" + measured
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
