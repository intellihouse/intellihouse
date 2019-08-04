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

import house.intelli.core.pv.AggregatedPvStatus;
import house.intelli.jdo.Entity;

@PersistenceCapable(table = "PvStatusQuarterHour")
@Inheritance(strategy = InheritanceStrategy.NEW_TABLE)
@Unique(name = "PvStatusQuarterHourEntity_deviceName_measured", members = {"deviceName", "measured"})
@Queries({
	@Query(name = "getPvStatusQuarterHourEntity_deviceName_measured", value = "SELECT UNIQUE WHERE this.deviceName == :deviceName && this.measured == :measured")
})
public class PvStatusQuarterHourEntity extends Entity implements AggregatedPvStatus {

	public static final int COVERED_PERIOD_MILLIS = 15 * 60_000;

	@Persistent(nullValue = NullValue.EXCEPTION)
	private String deviceName;

	@Persistent(nullValue = NullValue.EXCEPTION)
	private Date measured;

	@Column(defaultValue = "-1")
	int inputCountInterpolated;

	@Column(defaultValue = "-1")
	int inputCountMeasured;

	@Persistent(nullValue = NullValue.EXCEPTION)
	private String deviceMode;

	@Column(jdbcType = "real")
	private float acInVoltage;

	@Column(jdbcType = "real")
	private float acInFrequency;

	@Column(jdbcType = "real")
	private float acOutVoltage;

	@Column(jdbcType = "real")
	private float acOutFrequency;

	@Column(jdbcType = "real")
	private float acOutApparentPower; // Scheinleistung

	@Column(jdbcType = "real")
	private float acOutActivePower; // Wirkleistung

	@Column(jdbcType = "real")
	private float acOutLoadPercentage; // based on apparent power

	@Column(jdbcType = "real")
	private float internalBusVoltage;

	/**
	 * Batteriespannung vom Wechselrichter gemessen
	 */
	@Column(jdbcType = "real")
	private float batteryVoltageAtInverter;

	/**
	 * Batterie-Ladestrom
	 */
	@Column(jdbcType = "real")
	private float batteryChargeCurrent;

	/**
	 * Batterie-Kapazität (ca.)
	 */
	@Column(jdbcType = "real")
	private float batteryCapacityPercentage;

	/**
	 * Kühlkörper-Temperatur
	 */
	@Column(jdbcType = "real")
	private float heatSinkTemperature;

	/**
	 * PV Eingangsstrom (batterieseitig)
	 */
	@Column(jdbcType = "real")
	private float pvToBatteryCurrent;

	/**
	 * PV Spannung
	 */
	@Column(jdbcType = "real")
	private float pvVoltage;

	/**
	 * Batteriespannung vom Laderegler gemessen
	 */
	@Column(jdbcType = "real")
	private float batteryVoltageAtCharger;

	/**
	 * Batterie-Entladestrom
	 */
	@Column(jdbcType = "real")
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
	@Column(jdbcType = "real")
	private float pvPower;

	@Column(jdbcType = "real")
	private float acInVoltageMin;
	@Column(jdbcType = "real")
	private float acInFrequencyMin;
	@Column(jdbcType = "real")
	private float acOutVoltageMin;
	@Column(jdbcType = "real")
	private float acOutFrequencyMin;
	@Column(jdbcType = "real")
	private float acOutApparentPowerMin;
	@Column(jdbcType = "real")
	private float acOutActivePowerMin;
	@Column(jdbcType = "real")
	private float acOutLoadPercentageMin;
	@Column(jdbcType = "real")
	private float internalBusVoltageMin;
	@Column(jdbcType = "real")
	private float batteryVoltageAtInverterMin;
	@Column(jdbcType = "real")
	private float batteryChargeCurrentMin;
	@Column(jdbcType = "real")
	private float batteryCapacityPercentageMin;
	@Column(jdbcType = "real")
	private float heatSinkTemperatureMin;
	@Column(jdbcType = "real")
	private float pvToBatteryCurrentMin;
	@Column(jdbcType = "real")
	private float pvVoltageMin;
	@Column(jdbcType = "real")
	private float batteryVoltageAtChargerMin;
	@Column(jdbcType = "real")
	private float batteryDischargeCurrentMin;
	@Column(jdbcType = "real")
	private float pvPowerMin;

	@Column(jdbcType = "real")
	private float acInVoltageMax;
	@Column(jdbcType = "real")
	private float acInFrequencyMax;
	@Column(jdbcType = "real")
	private float acOutVoltageMax;
	@Column(jdbcType = "real")
	private float acOutFrequencyMax;
	@Column(jdbcType = "real")
	private float acOutApparentPowerMax;
	@Column(jdbcType = "real")
	private float acOutActivePowerMax;
	@Column(jdbcType = "real")
	private float acOutLoadPercentageMax;
	@Column(jdbcType = "real")
	private float internalBusVoltageMax;
	@Column(jdbcType = "real")
	private float batteryVoltageAtInverterMax;
	@Column(jdbcType = "real")
	private float batteryChargeCurrentMax;
	@Column(jdbcType = "real")
	private float batteryCapacityPercentageMax;
	@Column(jdbcType = "real")
	private float heatSinkTemperatureMax;
	@Column(jdbcType = "real")
	private float pvToBatteryCurrentMax;
	@Column(jdbcType = "real")
	private float pvVoltageMax;
	@Column(jdbcType = "real")
	private float batteryVoltageAtChargerMax;
	@Column(jdbcType = "real")
	private float batteryDischargeCurrentMax;
	@Column(jdbcType = "real")
	private float pvPowerMax;

	public PvStatusQuarterHourEntity() {
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
	public int getCoveredPeriodMillis() {
		return COVERED_PERIOD_MILLIS;
	}

	@Override
	public int getInputCountInterpolated() {
		return inputCountInterpolated;
	}

	@Override
	public void setInputCountInterpolated(int inputCountInterpolated) {
		this.inputCountInterpolated = inputCountInterpolated;
	}

	@Override
	public int getInputCountMeasured() {
		return inputCountMeasured;
	}

	@Override
	public void setInputCountMeasured(int inputCountMeasured) {
		this.inputCountMeasured = inputCountMeasured;
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
		return "PvStatusQuarterHourEntity [deviceName=" + deviceName + ", measured=" + measured
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

	@Override
	public float getAcInVoltageMin() {
		return acInVoltageMin;
	}

	@Override
	public void setAcInVoltageMin(float acInVoltageMin) {
		this.acInVoltageMin = acInVoltageMin;
	}

	@Override
	public float getAcInFrequencyMin() {
		return acInFrequencyMin;
	}

	@Override
	public void setAcInFrequencyMin(float acInFrequencyMin) {
		this.acInFrequencyMin = acInFrequencyMin;
	}

	@Override
	public float getAcOutVoltageMin() {
		return acOutVoltageMin;
	}

	@Override
	public void setAcOutVoltageMin(float acOutVoltageMin) {
		this.acOutVoltageMin = acOutVoltageMin;
	}

	@Override
	public float getAcOutFrequencyMin() {
		return acOutFrequencyMin;
	}

	@Override
	public void setAcOutFrequencyMin(float acOutFrequencyMin) {
		this.acOutFrequencyMin = acOutFrequencyMin;
	}

	@Override
	public float getAcOutApparentPowerMin() {
		return acOutApparentPowerMin;
	}

	@Override
	public void setAcOutApparentPowerMin(float acOutApparentPowerMin) {
		this.acOutApparentPowerMin = acOutApparentPowerMin;
	}

	@Override
	public float getAcOutActivePowerMin() {
		return acOutActivePowerMin;
	}

	@Override
	public void setAcOutActivePowerMin(float acOutActivePowerMin) {
		this.acOutActivePowerMin = acOutActivePowerMin;
	}

	@Override
	public float getAcOutLoadPercentageMin() {
		return acOutLoadPercentageMin;
	}

	@Override
	public void setAcOutLoadPercentageMin(float acOutLoadPercentageMin) {
		this.acOutLoadPercentageMin = acOutLoadPercentageMin;
	}

	@Override
	public float getInternalBusVoltageMin() {
		return internalBusVoltageMin;
	}

	@Override
	public void setInternalBusVoltageMin(float internalBusVoltageMin) {
		this.internalBusVoltageMin = internalBusVoltageMin;
	}

	@Override
	public float getBatteryVoltageAtInverterMin() {
		return batteryVoltageAtInverterMin;
	}

	@Override
	public void setBatteryVoltageAtInverterMin(float batteryVoltageAtInverterMin) {
		this.batteryVoltageAtInverterMin = batteryVoltageAtInverterMin;
	}

	@Override
	public float getBatteryChargeCurrentMin() {
		return batteryChargeCurrentMin;
	}

	@Override
	public void setBatteryChargeCurrentMin(float batteryChargeCurrentMin) {
		this.batteryChargeCurrentMin = batteryChargeCurrentMin;
	}

	@Override
	public float getBatteryCapacityPercentageMin() {
		return batteryCapacityPercentageMin;
	}

	@Override
	public void setBatteryCapacityPercentageMin(float batteryCapacityPercentageMin) {
		this.batteryCapacityPercentageMin = batteryCapacityPercentageMin;
	}

	@Override
	public float getHeatSinkTemperatureMin() {
		return heatSinkTemperatureMin;
	}

	@Override
	public void setHeatSinkTemperatureMin(float heatSinkTemperatureMin) {
		this.heatSinkTemperatureMin = heatSinkTemperatureMin;
	}

	@Override
	public float getPvToBatteryCurrentMin() {
		return pvToBatteryCurrentMin;
	}

	@Override
	public void setPvToBatteryCurrentMin(float pvToBatteryCurrentMin) {
		this.pvToBatteryCurrentMin = pvToBatteryCurrentMin;
	}

	@Override
	public float getPvVoltageMin() {
		return pvVoltageMin;
	}

	@Override
	public void setPvVoltageMin(float pvVoltageMin) {
		this.pvVoltageMin = pvVoltageMin;
	}

	@Override
	public float getBatteryVoltageAtChargerMin() {
		return batteryVoltageAtChargerMin;
	}

	@Override
	public void setBatteryVoltageAtChargerMin(float batteryVoltageAtChargerMin) {
		this.batteryVoltageAtChargerMin = batteryVoltageAtChargerMin;
	}

	@Override
	public float getBatteryDischargeCurrentMin() {
		return batteryDischargeCurrentMin;
	}

	@Override
	public void setBatteryDischargeCurrentMin(float batteryDischargeCurrentMin) {
		this.batteryDischargeCurrentMin = batteryDischargeCurrentMin;
	}

	@Override
	public float getPvPowerMin() {
		return pvPowerMin;
	}

	@Override
	public void setPvPowerMin(float pvPowerMin) {
		this.pvPowerMin = pvPowerMin;
	}

	@Override
	public float getAcInVoltageMax() {
		return acInVoltageMax;
	}

	@Override
	public void setAcInVoltageMax(float acInVoltageMax) {
		this.acInVoltageMax = acInVoltageMax;
	}

	@Override
	public float getAcInFrequencyMax() {
		return acInFrequencyMax;
	}

	@Override
	public void setAcInFrequencyMax(float acInFrequencyMax) {
		this.acInFrequencyMax = acInFrequencyMax;
	}

	@Override
	public float getAcOutVoltageMax() {
		return acOutVoltageMax;
	}

	@Override
	public void setAcOutVoltageMax(float acOutVoltageMax) {
		this.acOutVoltageMax = acOutVoltageMax;
	}

	@Override
	public float getAcOutFrequencyMax() {
		return acOutFrequencyMax;
	}

	@Override
	public void setAcOutFrequencyMax(float acOutFrequencyMax) {
		this.acOutFrequencyMax = acOutFrequencyMax;
	}

	@Override
	public float getAcOutApparentPowerMax() {
		return acOutApparentPowerMax;
	}

	@Override
	public void setAcOutApparentPowerMax(float acOutApparentPowerMax) {
		this.acOutApparentPowerMax = acOutApparentPowerMax;
	}

	@Override
	public float getAcOutActivePowerMax() {
		return acOutActivePowerMax;
	}

	@Override
	public void setAcOutActivePowerMax(float acOutActivePowerMax) {
		this.acOutActivePowerMax = acOutActivePowerMax;
	}

	@Override
	public float getAcOutLoadPercentageMax() {
		return acOutLoadPercentageMax;
	}

	@Override
	public void setAcOutLoadPercentageMax(float acOutLoadPercentageMax) {
		this.acOutLoadPercentageMax = acOutLoadPercentageMax;
	}

	@Override
	public float getInternalBusVoltageMax() {
		return internalBusVoltageMax;
	}

	@Override
	public void setInternalBusVoltageMax(float internalBusVoltageMax) {
		this.internalBusVoltageMax = internalBusVoltageMax;
	}

	@Override
	public float getBatteryVoltageAtInverterMax() {
		return batteryVoltageAtInverterMax;
	}

	@Override
	public void setBatteryVoltageAtInverterMax(float batteryVoltageAtInverterMax) {
		this.batteryVoltageAtInverterMax = batteryVoltageAtInverterMax;
	}

	@Override
	public float getBatteryChargeCurrentMax() {
		return batteryChargeCurrentMax;
	}

	@Override
	public void setBatteryChargeCurrentMax(float batteryChargeCurrentMax) {
		this.batteryChargeCurrentMax = batteryChargeCurrentMax;
	}

	@Override
	public float getBatteryCapacityPercentageMax() {
		return batteryCapacityPercentageMax;
	}

	@Override
	public void setBatteryCapacityPercentageMax(float batteryCapacityPercentageMax) {
		this.batteryCapacityPercentageMax = batteryCapacityPercentageMax;
	}

	@Override
	public float getHeatSinkTemperatureMax() {
		return heatSinkTemperatureMax;
	}

	@Override
	public void setHeatSinkTemperatureMax(float heatSinkTemperatureMax) {
		this.heatSinkTemperatureMax = heatSinkTemperatureMax;
	}

	@Override
	public float getPvToBatteryCurrentMax() {
		return pvToBatteryCurrentMax;
	}

	@Override
	public void setPvToBatteryCurrentMax(float pvToBatteryCurrentMax) {
		this.pvToBatteryCurrentMax = pvToBatteryCurrentMax;
	}

	@Override
	public float getPvVoltageMax() {
		return pvVoltageMax;
	}

	@Override
	public void setPvVoltageMax(float pvVoltageMax) {
		this.pvVoltageMax = pvVoltageMax;
	}

	@Override
	public float getBatteryVoltageAtChargerMax() {
		return batteryVoltageAtChargerMax;
	}

	@Override
	public void setBatteryVoltageAtChargerMax(float batteryVoltageAtChargerMax) {
		this.batteryVoltageAtChargerMax = batteryVoltageAtChargerMax;
	}

	@Override
	public float getBatteryDischargeCurrentMax() {
		return batteryDischargeCurrentMax;
	}

	@Override
	public void setBatteryDischargeCurrentMax(float batteryDischargeCurrentMax) {
		this.batteryDischargeCurrentMax = batteryDischargeCurrentMax;
	}

	@Override
	public float getPvPowerMax() {
		return pvPowerMax;
	}

	@Override
	public void setPvPowerMax(float pvPowerMax) {
		this.pvPowerMax = pvPowerMax;
	}

}
