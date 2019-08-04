package house.intelli.core.pv;

/**
 * {@link PvStatus} with estimated properties. Estimated properties are derived from the measured properties.
 * <p>
 * A class implementing this interface usually also implements {@link AggregatedPvStatus} -- but it does not need to.
 * @author mn
 */
public interface EstimatedPvStatus extends PvStatus {

	/**
	 * Gets the energy in Wh (Watt-hours) that has flown into (> 0) or out of (< 0) the
	 * battery in an ideal world without energy losses.
	 * <p>
	 * If the system is in {@link DeviceMode#BATTERY}, this value is exactly the difference
	 * between the energy coming from the PV system (derived from {@link #getPvPower() pvPower})
	 * and the energy consumed (derived from {@link #getAcOutActivePower() acOutActivePower}).
	 * <p>
	 * If the system is in {@link DeviceMode#LINE}, then this is exactly
	 * the same as the energy coming from the PV system, because the consumption comes from
	 * the line (= city grid).
	 * <p>
	 * The time used to transform power (W = Watt) into energy (Wh = Watt-hours) is
	 * {@link #getCoveredPeriodMillis() coveredPeriodMillis}.
	 *
	 * @return the energy charging (positive) or discharging (negative) the battery in an
	 * ideal world without energy losses.
	 */
	float getEstBatteryChargeEnergyIdeal();

	void setEstBatteryChargeEnergyIdeal(float idealBatteryChargeEnergy);

	/**
	 * Gets the energy in Wh charging (positive) or discharging (negative) the battery in our
	 * <i>estimated</i> real world with <i>estimated</i> energy losses.
	 * <p>
	 * Due to the energy losses, this value is always lower than
	 * {@link #getEstBatteryChargeEnergyIdeal() estBatteryChargeEnergyIdeal}.
	 * Also, this value might be negative, even though {@code estBatteryChargeEnergyIdeal}
	 * is positive, because the system also consumes energy itself.
	 *
	 * @return the energy charging (positive) or discharging (negative) the battery in our
	 * <i>estimated</i> real world with <i>estimated</i> energy losses.
	 */
	float getEstBatteryChargeEnergyReal();

	void setEstBatteryChargeEnergyReal(float estimatedBatteryChargeEnergy);

	/**
	 * @return the capacity in Wh that the battery likely has.
	 */
	float getEstBatteryEnergyCapacity();

	void setEstBatteryEnergyCapacity(float estimatedBatteryCapacity);

	/**
	 * @return the energy level in Wh that is currently stored in the battery.
	 */
	float getEstBatteryEnergyLevel();

	void setEstBatteryEnergyLevel(float estimatedBatteryEnergy);

}
