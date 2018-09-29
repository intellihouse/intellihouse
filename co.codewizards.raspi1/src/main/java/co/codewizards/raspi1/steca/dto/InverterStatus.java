package co.codewizards.raspi1.steca.dto;

import java.util.Date;

public class InverterStatus {

	private Date measured;
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

	public InverterStatus() {
	}

	public Date getMeasured() {
		return measured;
	}
	public void setMeasured(Date measured) {
		this.measured = measured;
	}

	public float getAcInVoltage() {
		return acInVoltage;
	}
	public void setAcInVoltage(float acInVoltage) {
		this.acInVoltage = acInVoltage;
	}

	public float getAcInFrequency() {
		return acInFrequency;
	}
	public void setAcInFrequency(float acInFrequency) {
		this.acInFrequency = acInFrequency;
	}

	public float getAcOutVoltage() {
		return acOutVoltage;
	}
	public void setAcOutVoltage(float acOutVoltage) {
		this.acOutVoltage = acOutVoltage;
	}

	public float getAcOutFrequency() {
		return acOutFrequency;
	}
	public void setAcOutFrequency(float acOutFrequency) {
		this.acOutFrequency = acOutFrequency;
	}

	public float getAcOutApparentPower() {
		return acOutApparentPower;
	}
	public void setAcOutApparentPower(float acOutApparentPower) {
		this.acOutApparentPower = acOutApparentPower;
	}

	public float getAcOutActivePower() {
		return acOutActivePower;
	}
	public void setAcOutActivePower(float acOutActivePower) {
		this.acOutActivePower = acOutActivePower;
	}

	public float getAcOutLoadPercentage() {
		return acOutLoadPercentage;
	}
	public void setAcOutLoadPercentage(float acOutLoadPercentage) {
		this.acOutLoadPercentage = acOutLoadPercentage;
	}

	public float getInternalBusVoltage() {
		return internalBusVoltage;
	}
	public void setInternalBusVoltage(float internalBusVoltage) {
		this.internalBusVoltage = internalBusVoltage;
	}
	public float getBatteryVoltageAtInverter() {
		return batteryVoltageAtInverter;
	}

	public void setBatteryVoltageAtInverter(float batteryVoltageAtInverter) {
		this.batteryVoltageAtInverter = batteryVoltageAtInverter;
	}
	public float getBatteryChargeCurrent() {
		return batteryChargeCurrent;
	}
	public void setBatteryChargeCurrent(float batteryChargeCurrent) {
		this.batteryChargeCurrent = batteryChargeCurrent;
	}

	public float getBatteryCapacityPercentage() {
		return batteryCapacityPercentage;
	}
	public void setBatteryCapacityPercentage(float batteryCapacityPercentage) {
		this.batteryCapacityPercentage = batteryCapacityPercentage;
	}

	public float getHeatSinkTemperature() {
		return heatSinkTemperature;
	}
	public void setHeatSinkTemperature(float heatSinkTemperature) {
		this.heatSinkTemperature = heatSinkTemperature;
	}

	public float getPvToBatteryCurrent() {
		return pvToBatteryCurrent;
	}
	public void setPvToBatteryCurrent(float pvToBatteryCurrent) {
		this.pvToBatteryCurrent = pvToBatteryCurrent;
	}

	public float getPvVoltage() {
		return pvVoltage;
	}
	public void setPvVoltage(float pvVoltage) {
		this.pvVoltage = pvVoltage;
	}

	public float getBatteryVoltageAtCharger() {
		return batteryVoltageAtCharger;
	}
	public void setBatteryVoltageAtCharger(float batteryVoltageAtCharger) {
		this.batteryVoltageAtCharger = batteryVoltageAtCharger;
	}

	public float getBatteryDischargeCurrent() {
		return batteryDischargeCurrent;
	}
	public void setBatteryDischargeCurrent(float batteryDischargeCurrent) {
		this.batteryDischargeCurrent = batteryDischargeCurrent;
	}

	public int getStatusBitmask() {
		return statusBitmask;
	}
	public void setStatusBitmask(int statusBitmask) {
		this.statusBitmask = statusBitmask;
	}

	public int getEepromVersion() {
		return eepromVersion;
	}
	public void setEepromVersion(int eepromVersion) {
		this.eepromVersion = eepromVersion;
	}

	public float getPvPower() {
		return pvPower;
	}
	public void setPvPower(float pvChargePower) {
		this.pvPower = pvChargePower;
	}

	@Override
	public String toString() {
		return "InverterStatus [measured=" + measured + ", acInVoltage=" + acInVoltage + ", acInFrequency=" + acInFrequency
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
