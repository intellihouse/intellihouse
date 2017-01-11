package co.codewizards.raspi1;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;

public class SwitchOutDemo implements Runnable {
	private final Logger logger = LoggerFactory.getLogger(SwitchOutDemo.class);
	
	public static final long PROGRAM_RUN_TIME_MILLIS = 5L * 60L * 1000L;
	public static final long SWITCH_PERIOD_MILLIS = 2_000L;

	private String[] args;
	
//	public static void main(String[] args) {
//		new SwitchOutDemo(args).run();
//	}

	public SwitchOutDemo(String[] args) {
		this.args = args;
	}

	@Override
	public void run() {
		final GpioController gpio = GpioFactory.getInstance();
		final GpioPinDigitalOutput pinOut = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_01);

		long start = System.currentTimeMillis();

		while (System.currentTimeMillis() - start <= PROGRAM_RUN_TIME_MILLIS) {
			if (pinOut.getState() == PinState.HIGH)
				pinOut.setState(PinState.LOW);
			else
				pinOut.setState(PinState.HIGH);

			sleep(SWITCH_PERIOD_MILLIS);
		}
		
		// finally switch off.
		pinOut.setState(PinState.LOW);
	}
	
	private final void sleep(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			logger.error("sleep: " + e, e);
		}
	}
}
