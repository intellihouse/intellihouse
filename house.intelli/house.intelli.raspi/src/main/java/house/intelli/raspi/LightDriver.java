package house.intelli.raspi;

import static house.intelli.core.util.AssertUtil.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.GpioPinPwmOutput;
import com.pi4j.io.gpio.Pin;
import com.pi4j.io.gpio.PinMode;
import com.pi4j.io.gpio.PinState;

import house.intelli.core.bean.AbstractBean;
import house.intelli.core.bean.PropertyBase;

public class LightDriver extends AbstractBean<LightDriver.Property> implements AutoCloseable {
	private final Logger logger = LoggerFactory.getLogger(LightDriver.class);

	public static interface Property extends PropertyBase { }

	public static enum PropertyEnum implements Property {
		dimmerValue
	}

	public static final int MIN_DIMMER_VALUE = 0;
	public static final int MAX_DIMMER_VALUE = 100;

	private final Pin pin;
	private GpioPinPwmOutput pwmOutput;
	private GpioPinDigitalOutput digitalOutput;
	private int dimmerValue = MAX_DIMMER_VALUE;

	public LightDriver(Pin pin) {
		this.pin = assertNotNull(pin, "pin");

		if (LightDriver.class == this.getClass())
			init();
	}

	protected void init() {
		applyDimmerValue();
	}

	public Pin getPin() {
		return pin;
	}

	public synchronized int getDimmerValue() {
		return dimmerValue;
	}
	public void setDimmerValue(int dimmerValue) { // not synchronized to prevent deadlocks in listeners
		if (dimmerValue < MIN_DIMMER_VALUE)
			throw new IllegalArgumentException("dimmerValue < MIN_DIMMER_VALUE");

		if (dimmerValue > MAX_DIMMER_VALUE)
			throw new IllegalArgumentException("dimmerValue > MAX_DIMMER_VALUE");

		if (setPropertyValue(PropertyEnum.dimmerValue, dimmerValue))
			applyDimmerValue();
	}

	protected synchronized void applyDimmerValue() {
		if (dimmerValue == MIN_DIMMER_VALUE || dimmerValue == MAX_DIMMER_VALUE) {
			openDigitalOutput();
			if (dimmerValue == MAX_DIMMER_VALUE)
				digitalOutput.setState(PinState.HIGH);
			else
				digitalOutput.setState(PinState.LOW);
		}
		else {
			openPwmOutput();
			pwmOutput.setPwm(getPwm());
		}
	}

	private int getPwm() {
		if (pwmOutput.isMode(PinMode.PWM_OUTPUT))
			return dimmerValue * 1024 / 100; // is this really true? in my tests, it didn't look like the logical pwm value was really this - it looked like being 0 to 100, too, even for hardware-pwm.

		return dimmerValue;
	}

	private void openPwmOutput() {
		if (pwmOutput != null)
			return;

		logger.debug("openPwmOutput");

		closeDigitalOutput();

		GpioController gpioController = GpioFactory.getInstance();

		if (pin.getSupportedPinModes().contains(PinMode.PWM_OUTPUT))
			pwmOutput = gpioController.provisionPwmOutputPin(pin);
		else
			pwmOutput = gpioController.provisionSoftPwmOutputPin(pin);
	}

	private void openDigitalOutput() {
		if (digitalOutput != null)
			return;

		logger.debug("openDigitalOutput");

		closePwmOutput();

		GpioController gpioController = GpioFactory.getInstance();
		digitalOutput = gpioController.provisionDigitalOutputPin(pin);
	}

	private void closePwmOutput() {
		if (pwmOutput == null)
			return;

		logger.debug("closePwmOutput");
		GpioController gpioController = GpioFactory.getInstance();
		gpioController.unprovisionPin(pwmOutput);
		pwmOutput = null;
	}

	private void closeDigitalOutput() {
		if (digitalOutput == null)
			return;

		logger.debug("closeDigitalOutput");
		GpioController gpioController = GpioFactory.getInstance();
		gpioController.unprovisionPin(digitalOutput);
		digitalOutput = null;
	}

	@Override
	public synchronized void close() {
		logger.debug("close");
		closeDigitalOutput();
		closePwmOutput();
	}
}
