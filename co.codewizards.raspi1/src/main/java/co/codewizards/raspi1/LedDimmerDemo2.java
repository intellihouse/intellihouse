package co.codewizards.raspi1;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;
import com.pi4j.wiringpi.Gpio;

public class LedDimmerDemo2 implements Runnable {
	private final Logger logger = LoggerFactory.getLogger(LedDimmerDemo2.class);
	
	private String[] args;
	
	public static final long PROGRAM_RUN_TIME_MILLIS = 60L * 60L * 1000L;
	public static final long SWITCH_PERIOD_MILLIS = 10_000L;

	private static final int[] LED_DIMMER_PWM = {
		100, // brightness impression isn't linear but logarithmic!
		10, // => 50% brightness
		1, // => 25% brightness
		0,
		1,
		2,
		4,
		8,
		16,
		32,
		64,
		100,
		0
	};

	private int ledDimmerIndex = 0;
	private int pwmValue = -1;
	private volatile int pwmOnNanos;
	private volatile int pwmOffNanos;
	
	private GpioPinDigitalOutput led;
	
	public LedDimmerDemo2(String[] args) {
		this.args = args;
	}
	
	private Thread manualPwmThread = new Thread() {
		{
			setDaemon(true);
		}
		
		public void run() {
			while (! isInterrupted()) {
				if (pwmOnNanos > 0) {
					if (led.getState() != PinState.HIGH)
						led.setState(PinState.HIGH);
					
					_sleepNanos(pwmOnNanos);
				}

				if (pwmOffNanos > 0) {
					if (led.getState() != PinState.LOW)
						led.setState(PinState.LOW);
					
					_sleepNanos(pwmOffNanos);
				}
			}
		}
	};
	
	public int getPwmValue() {
		return pwmValue;
	}
	public void setPwmValue(int pwmValue) {
		this.pwmValue = pwmValue;
		final int cycleNanos = 100;
		this.pwmOnNanos = pwmValue;
		this.pwmOffNanos = cycleNanos - this.pwmOnNanos;
	}
	
	@Override
	public void run() {
		final GpioController gpio = GpioFactory.getInstance();
		led = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_01);

		setPwmValue(0);
		manualPwmThread.start();

		long start = System.currentTimeMillis();
	
		while (System.currentTimeMillis() - start <= PROGRAM_RUN_TIME_MILLIS) {
			setPwmValue(LED_DIMMER_PWM[ledDimmerIndex]);
			
			_sleep(SWITCH_PERIOD_MILLIS);
			
			if (++ledDimmerIndex >= LED_DIMMER_PWM.length)
				ledDimmerIndex = 0;
		}
		
		// finally switch off.
		setPwmValue(0);
	}

	private final void _sleepNanos(int nanos) {
		try {
			Thread.sleep(0L, nanos);
		} catch (InterruptedException e) {
			logger.error("sleep: " + e, e);
		}
	}
	
	private final void _sleep(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			logger.error("sleep: " + e, e);
		}
	}
}
