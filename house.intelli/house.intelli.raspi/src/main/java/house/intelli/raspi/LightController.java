package house.intelli.raspi;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.wiringpi.Gpio;

public class LightController implements AutoCloseable {
	private final Logger logger = LoggerFactory.getLogger(LightController.class);

	private static final int[] LED_DIMMER_PWM = {
			1,
			2,
			4,
			8,
			16,
			32,
			64,
			100
	};

	static { // to ensure thread-safe initialisation, we do this here, already (class-loading+init is thread-safe)
		GpioFactory.getInstance();

		Gpio.pwmSetMode(Gpio.PWM_MODE_BAL); // balanced works better, because the pulses are shorter and thus the power supply doesn't "swing"
//		Gpio.pwmSetClock(divisor); // defaults work fine and I don't know what they are => just leave it unchanged.
//		Gpio.pwmSetRange(range); // default works fine don't change it now.
	}

	private List<LightDriver> lightDrivers = new ArrayList<>();
	private List<PowerSupplyDriver> powerSupplyDrivers = new ArrayList<>();

	@Override
	public synchronized void close() throws Exception {
		// TODO Auto-generated method stub

	}
}
