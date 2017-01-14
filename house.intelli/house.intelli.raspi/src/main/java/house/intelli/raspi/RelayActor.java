package house.intelli.raspi;

import static house.intelli.core.event.EventQueue.*;
import static house.intelli.core.util.AssertUtil.*;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.Pin;
import com.pi4j.io.gpio.PinState;

import house.intelli.core.bean.AbstractBean;
import house.intelli.core.bean.PropertyBase;

public class RelayActor extends AbstractBean<RelayActor.Property> implements AutoCloseable {

	public static interface Property extends PropertyBase { }

	public static enum PropertyEnum implements Property {
		pin,
		energized
	}

	private Pin pin;
	private GpioPinDigitalOutput digitalOutput;

	private boolean energized;

	public Pin getPin() {
		return pin;
	}
	public void setPin(Pin pin) {
		if (this.pin != null)
			throw new IllegalStateException("pin already assigned!");

		setPropertyValue(PropertyEnum.pin, pin);
	}

	public void init() {
		assertNotNull(pin, "pin");
		applyEnergized();
	}

	public boolean isEnergized() {
		assertEventThread();
		return energized;
	}

	public void setEnergized(boolean energized) { // not synchronized to prevent deadlocks in listeners
		assertEventThread();
		if (setPropertyValue(PropertyEnum.energized, energized))
			applyEnergized();
	}

	protected void applyEnergized() {
		openDigitalOutput();

		if (energized)
			digitalOutput.setState(PinState.HIGH);
		else
			digitalOutput.setState(PinState.LOW);
	}

	private void openDigitalOutput() {
		assertEventThread();
		if (digitalOutput != null)
			return;

		assertNotNull(pin, "pin");

		GpioController gpioController = GpioFactory.getInstance();
		digitalOutput = gpioController.provisionDigitalOutputPin(pin);
	}

	private void closeDigitalOutput() {
		assertEventThread();
		if (digitalOutput == null)
			return;

		GpioController gpioController = GpioFactory.getInstance();
		gpioController.unprovisionPin(digitalOutput);
		digitalOutput = null;
	}

	@Override
	public void close() {
		invokeAndWait(new Runnable() {
			@Override
			public void run() {
				closeDigitalOutput();
			}
		});
	}
}
