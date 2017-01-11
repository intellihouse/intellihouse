package co.codewizards.raspi1;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.GpioPinPwmOutput;
import com.pi4j.io.gpio.PinMode;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;
import com.pi4j.io.gpio.impl.PinImpl;

public class SwitchOutDemo2 implements Runnable {
	private final Logger logger = LoggerFactory.getLogger(SwitchOutDemo2.class);
	
	public static final long PROGRAM_RUN_TIME_MILLIS = 5L * 60L * 1000L;
	public static final long SWITCH_PERIOD_MILLIS = 3_000L;

	private String[] args;
	private int pinOutIndex;
	
//	public static void main(String[] args) {
//		new SwitchOutDemo2(args).run();
//	}

	public SwitchOutDemo2(String[] args) {
		this.args = args;
	}

	@Override
	public void run() {
		final GpioController gpio = GpioFactory.getInstance();
		final GpioPinDigitalOutput[] pinOuts = new GpioPinDigitalOutput[] {
				gpio.provisionDigitalOutputPin(RaspiPin.GPIO_07),
				gpio.provisionDigitalOutputPin(RaspiPin.GPIO_08),
				gpio.provisionDigitalOutputPin(RaspiPin.GPIO_09)
		};

		long start = System.currentTimeMillis();
		
		while (System.currentTimeMillis() - start <= PROGRAM_RUN_TIME_MILLIS) {
			for (GpioPinDigitalOutput pinOut : pinOuts) {
				pinOut.setState(PinState.LOW);
			}

			pinOuts[pinOutIndex].setState(PinState.HIGH);
			if (++pinOutIndex >= pinOuts.length)
				pinOutIndex = 0;
				
			sleep(SWITCH_PERIOD_MILLIS);
		}
				
		start = System.currentTimeMillis();

		while (System.currentTimeMillis() - start <= PROGRAM_RUN_TIME_MILLIS) {
			for (GpioPinDigitalOutput pinOut : pinOuts) {
				if (Math.random() < 0.5d)
					switchPinOut(pinOut);
			}
			sleep(SWITCH_PERIOD_MILLIS);
		}
		
		// finally switch off.
		for (GpioPinDigitalOutput pinOut : pinOuts) {
			pinOut.setState(PinState.LOW);
		}
	}
	
	private final void sleep(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			logger.error("sleep: " + e, e);
		}
	}
	
	private void switchPinOut(GpioPinDigitalOutput pinOut) {
		if (pinOut.getState() == PinState.HIGH)
			pinOut.setState(PinState.LOW);
		else
			pinOut.setState(PinState.HIGH);
	}
}
