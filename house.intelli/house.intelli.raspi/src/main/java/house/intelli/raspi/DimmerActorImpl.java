package house.intelli.raspi;

import static house.intelli.core.event.EventQueue.*;
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
import com.pi4j.wiringpi.Gpio;

import house.intelli.core.bean.AbstractBean;

public class DimmerActorImpl extends AbstractBean<DimmerActor.Property> implements DimmerActor, AutoCloseable {
	private final Logger logger = LoggerFactory.getLogger(DimmerActorImpl.class);

	public static enum PropertyEnum implements DimmerActor.Property {
		pin
	}

	private static final boolean USE_DIGITAL_OUTPUT_FOR_EXTREME_VALUES = false;

	private Pin pin;
	private GpioPinPwmOutput pwmOutput;
	private GpioPinDigitalOutput digitalOutput;
	private int dimmerValue = MIN_DIMMER_VALUE;

	public static final int[] PWM_VALUES = {
			0,
			1,
			2,
			4,
			8,
			16,
			32,
			64,
			100
	};

	public void init() {
		assertEventThread();
		assertNotNull(pin, "pin");
		applyDimmerValue();
	}

	public Pin getPin() {
		return pin;
	}
	public void setPin(Pin pin) {
		if (this.pin != null)
			throw new IllegalStateException("pin already assigned!");

		assertEventThread();
		setPropertyValue(PropertyEnum.pin, pin);
	}

	@Override
	public int getDimmerValue() {
		assertEventThread();
		return dimmerValue;
	}
	@Override
	public void setDimmerValue(int dimmerValue) { // not synchronized to prevent deadlocks in listeners
		if (dimmerValue < MIN_DIMMER_VALUE)
			throw new IllegalArgumentException("dimmerValue < MIN_DIMMER_VALUE");

		if (dimmerValue > MAX_DIMMER_VALUE)
			throw new IllegalArgumentException("dimmerValue > MAX_DIMMER_VALUE");

		assertEventThread();

		int pwmValueIndex = getPwmValueIndex(dimmerValue);
		dimmerValue = pwmValueIndex * 100 / (PWM_VALUES.length - 1);

		if (setPropertyValue(DimmerActor.PropertyEnum.dimmerValue, dimmerValue))
			applyDimmerValue();
	}

	protected void applyDimmerValue() {
		if (USE_DIGITAL_OUTPUT_FOR_EXTREME_VALUES && (dimmerValue == MIN_DIMMER_VALUE || dimmerValue == MAX_DIMMER_VALUE)) {
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

	protected int getPwm() {
////		if (pwmOutput.isMode(PinMode.PWM_OUTPUT))
////			return dimmerValue * 512 / 100; // is this really true? in my tests, it didn't look like the logical pwm value was really this - it looked like being 0 to 100, too, even for hardware-pwm.
//		return dimmerValue;
		return PWM_VALUES[getPwmValueIndex(dimmerValue)];
	}

	private int getPwmValueIndex(int dimmerValue) {
		int pwmIndex = Math.round((float) dimmerValue * (PWM_VALUES.length - 1) / 100);
		return pwmIndex;
	}

	private void openPwmOutput() {
		assertEventThread();
		if (pwmOutput != null)
			return;

		assertNotNull(pin, "pin");

		final boolean hardPwm = pin.getSupportedPinModes().contains(PinMode.PWM_OUTPUT);
		logger.debug("openPwmOutput: hardPwm={}", hardPwm);

		closeDigitalOutput();

		GpioController gpioController = GpioFactory.getInstance();

		if (hardPwm) {
			pwmOutput = gpioController.provisionPwmOutputPin(pin);

			// The PWM properties must be set *after* the output-pin was provisioned. It doesn't
			// work before.
			//
			// The *balanced* mode is required because it causes less fluctuation. The light looks
			// really nice and uniform. With mark:space, the (cheap, bad) power supply causes swinging
			// fluctuations despite our 5 * 4700 uF capacitors :-(
			//
			// The clock of 1920 and range of 100 was determined experimentically and with the help
			// of: http://raspberrypi.stackexchange.com/questions/4906/control-hardware-pwm-frequency#9725

			Gpio.pwmSetMode(Gpio.PWM_MODE_BAL);
			Gpio.pwmSetClock(1920);
			Gpio.pwmSetRange(100);
		}
		else {
			pwmOutput = gpioController.provisionSoftPwmOutputPin(pin);
		}
	}

	private void openDigitalOutput() {
		assertEventThread();
		if (digitalOutput != null)
			return;

		assertNotNull(pin, "pin");

		logger.debug("openDigitalOutput");

		closePwmOutput();

		GpioController gpioController = GpioFactory.getInstance();
		digitalOutput = gpioController.provisionDigitalOutputPin(pin);
	}

	private void closePwmOutput() {
		assertEventThread();
		if (pwmOutput == null)
			return;

		logger.debug("closePwmOutput");
		GpioController gpioController = GpioFactory.getInstance();
		gpioController.unprovisionPin(pwmOutput);
		pwmOutput = null;
	}

	private void closeDigitalOutput() {
		assertEventThread();
		if (digitalOutput == null)
			return;

		logger.debug("closeDigitalOutput");
		GpioController gpioController = GpioFactory.getInstance();
		gpioController.unprovisionPin(digitalOutput);
		digitalOutput = null;
	}

	@Override
	public void close() {
		logger.debug("close");
		invokeAndWait(new Runnable() {
			@Override
			public void run() {
				closeDigitalOutput();
				closePwmOutput();
			}
		});
	}
}
