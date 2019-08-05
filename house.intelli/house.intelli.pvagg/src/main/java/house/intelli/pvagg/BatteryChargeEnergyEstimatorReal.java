package house.intelli.pvagg;

import static java.util.Objects.requireNonNull;

import house.intelli.core.pv.DeviceMode;
import house.intelli.jdo.model.PvStatusEntity;

public class BatteryChargeEnergyEstimatorReal extends BatteryChargeEnergyEstimator {

	// TODO are these constant?

	private static final double CHARGE_LOSS_PERCENT = 10;
	private static final double DISCHARGE_LOSS_PERCENT = 5;

	/**
	 * How many W does the inverter/charger consume itself, even if no charging happens and there's no energy-consumption in the house.
	 *
	 * TODO make this configurable?
	 */
	private static final double SELF_CONSUMPTION_POWER = 20;

	@Override
	protected double estimateBatteryChargeEnergy(PvStatusEntity pvStatusEntity) {
		requireNonNull(pvStatusEntity, "pvStatusEntity");
		final DeviceMode deviceMode = DeviceMode.from(pvStatusEntity.getDeviceMode());
		if (DeviceMode.FAILURE == deviceMode)
			return 0;

		double result = super.estimateBatteryChargeEnergy(pvStatusEntity);

		final double selfConsumptionEnergy = powerToEnergyWh(SELF_CONSUMPTION_POWER, pvStatusEntity.getCoveredPeriodMillis());
		result -= selfConsumptionEnergy;

		if (result >= 0) {
			// charging
			final double chargeLossEnergy = result * CHARGE_LOSS_PERCENT / 100;

			// chargeLossEnergy is positive => we need to subtract it.
			result -= chargeLossEnergy;
		}
		else {
			// DIScharging
			final double dischargeLossEnergy = result * DISCHARGE_LOSS_PERCENT / 100;

			// dischargeLossEnergy is already negative => we need to add it.
			result += dischargeLossEnergy;
		}
		return result;
	}

}
