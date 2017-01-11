package co.codewizards.raspi1;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinPwmOutput;
import com.pi4j.io.gpio.RaspiPin;
import com.pi4j.wiringpi.Gpio;

public class LedDimmerDemo implements Runnable {
	private final Logger logger = LoggerFactory.getLogger(LedDimmerDemo.class);
	
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
	
	public LedDimmerDemo(String[] args) {
		this.args = args;
	}
	
	@Override
	public void run() {
//		Gpio.pwmSetMode(Gpio.PWM_MODE_MS);
		final GpioController gpio = GpioFactory.getInstance();
//		final GpioPinDigitalOutput led = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_06);
//		final GpioPinPwmOutput led = gpio.provisionSoftPwmOutputPin(RaspiPin.GPIO_06);
		final GpioPinPwmOutput led = gpio.provisionPwmOutputPin(RaspiPin.GPIO_01);

//		Gpio.pwmSetMode(Gpio.PWM_MODE_MS);
		Gpio.pwmSetMode(Gpio.PWM_MODE_BAL);
		Gpio.pwmSetClock(1_000);
		Gpio.pwmSetRange(100);

		long start = System.currentTimeMillis();

//		while (System.currentTimeMillis() - start <= PROGRAM_RUN_TIME_MILLIS) {
//			led.high();
//			sleep(LED_DIMMER_TIMINGS[ledDimmerIndex].onTimeMillis);
//			
//			led.low();
//			sleep(LED_DIMMER_TIMINGS[ledDimmerIndex].offTimeMillis);
//			
//			if (System.currentTimeMillis() - ledDimmerStart > DIMMER_SWITCH_PERIOD_MILLIS) {
//				if (++ledDimmerTimingIndex >= LED_DIMMER_TIMINGS.length)
//					ledDimmerTimingIndex = 0;
//				
//				ledDimmerStart = System.currentTimeMillis();
//			}
//		}
		
		while (System.currentTimeMillis() - start <= PROGRAM_RUN_TIME_MILLIS) {
			led.setPwm(LED_DIMMER_PWM[ledDimmerIndex]);
			
			sleep(SWITCH_PERIOD_MILLIS);
			
			if (++ledDimmerIndex >= LED_DIMMER_PWM.length)
				ledDimmerIndex = 0;
		}
		
		// finally switch off.
		led.setPwm(0);
	}
	
	private final void sleep(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			logger.error("sleep: " + e, e);
		}
	}
}
