package house.intelli.raspi;

import static house.intelli.core.util.AssertUtil.*;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.Pin;
import com.pi4j.io.gpio.PinState;

import house.intelli.core.bean.AbstractBean;
import house.intelli.core.bean.PropertyBase;

public class PowerSupplyDriver extends AbstractBean<PowerSupplyDriver.Property> implements AutoCloseable {

	public static interface Property extends PropertyBase { }

	public static enum PropertyEnum implements Property {
		energized
	}

	private final Pin pin;
	private GpioPinDigitalOutput digitalOutput;

	private boolean energized;

	public PowerSupplyDriver(Pin pin) {
		this.pin = assertNotNull(pin, "pin");

		if (PowerSupplyDriver.class == this.getClass())
			init();
	}

	protected void init() {
		applyEnergized();
	}

	public synchronized boolean isEnergized() {
		return energized;
	}

	public void setEnergized(boolean energized) { // not synchronized to prevent deadlocks in listeners
		if (setPropertyValue(PropertyEnum.energized, energized))
			applyEnergized();
	}

	protected synchronized void applyEnergized() {
		openDigitalOutput();

		if (energized)
			digitalOutput.setState(PinState.HIGH);
		else
			digitalOutput.setState(PinState.LOW);
	}

	private void openDigitalOutput() {
		if (digitalOutput != null)
			return;

		GpioController gpioController = GpioFactory.getInstance();
		digitalOutput = gpioController.provisionDigitalOutputPin(pin);
	}

	private void closeDigitalOutput() {
		if (digitalOutput == null)
			return;

		GpioController gpioController = GpioFactory.getInstance();
		gpioController.unprovisionPin(digitalOutput);
		digitalOutput = null;
	}

	@Override
	public synchronized void close() {
		closeDigitalOutput();
	}
}
