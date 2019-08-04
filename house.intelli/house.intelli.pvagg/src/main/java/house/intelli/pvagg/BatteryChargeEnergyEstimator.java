package house.intelli.pvagg;

import static java.util.Objects.*;

import java.util.List;

import house.intelli.core.pv.DeviceMode;
import house.intelli.jdo.IntelliHouseTransaction;
import house.intelli.jdo.model.PvStatusEntity;

public abstract class BatteryChargeEnergyEstimator {

	private static final int HOUR_MILLIS = 60 * 60 * 1000;

	private IntelliHouseTransaction transaction;

	public IntelliHouseTransaction getTransaction() {
		return transaction;
	}

	public void setTransaction(IntelliHouseTransaction transaction) {
		this.transaction = transaction;
	}

	public double estimateBatteryChargeEnergy(final List<PvStatusEntity> pvStatusEntities) {
		requireNonNull(pvStatusEntities, "pvStatusEntities");
		double result = 0;
		for (PvStatusEntity pvStatusEntity : pvStatusEntities) {
			result += estimateBatteryChargeEnergy(pvStatusEntity);
		}
		return result;
	}

	protected double estimateBatteryChargeEnergy(final PvStatusEntity pvStatusEntity) {
		requireNonNull(pvStatusEntity, "pvStatusEntity");
		DeviceMode deviceMode = DeviceMode.from(pvStatusEntity.getDeviceMode());
		final double power; // W (Watt)
		switch (deviceMode) {
			case BATTERY:
				power = (double) pvStatusEntity.getPvPower() - (double) pvStatusEntity.getAcOutActivePower();
				break;
			case LINE:
				power = pvStatusEntity.getPvPower();
				break;
			default:
				throw new IllegalStateException("Unknown deviceMode: " + deviceMode);
		}

		// energy in Wh (Watt-hours)
		final double energy = powerToEnergyWh(power, pvStatusEntity.getCoveredPeriodMillis());
		return energy;
	}

	/**
	 * Converts the given power in W (Watt) with the given time in ms (milliseconds)
	 * to an energy in Wh (Watt-hours).
	 * <p>
	 * 1 W = 1 J / s
	 * <p>
	 * 1 Wh = (1 J / s) * (60 * 60 s) = 3600 J
	 * @param w the power in W (Watt).
	 * @param ms the time in ms (milliseconds).
	 * @return the energy in Wh (Watt-hours).
	 */
	protected static final double powerToEnergyWh(final double w, final int ms) {
		final double wh = w * ms / HOUR_MILLIS;
		return wh;
	}

}
