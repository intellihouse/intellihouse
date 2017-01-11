package co.codewizards.raspi1;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinAnalogOutput;
import com.pi4j.io.gpio.RaspiPin;

public class LedAnalogDemo implements Runnable {
	private final Logger logger = LoggerFactory.getLogger(LedAnalogDemo.class);
	
	private String[] args;
	
	public static final long PROGRAM_RUN_TIME_MILLIS = 60L * 60L * 1000L;
	public static final long SWITCH_PERIOD_MILLIS = 10_000L;

	private static final double VALUE_INCREMENT = 0.1d;
	private static final double VALUE_MIN = 0d;
	private static final double VALUE_MAX = 100d;
	private double value = VALUE_MIN;
	
	private static String getTimestamp() {
		return new SimpleDateFormat("YYYY-MM-dd HH:mm:ss.S").format(new Date());
	}
	
	public LedAnalogDemo(String[] args) {
		this.args = args;
	}
	
	@Override
	public void run() {
		final GpioController gpio = GpioFactory.getInstance();
		GpioPinAnalogOutput led = gpio.provisionAnalogOutputPin(RaspiPin.GPIO_06);

		long start = System.currentTimeMillis();
		
		while (System.currentTimeMillis() - start <= PROGRAM_RUN_TIME_MILLIS) {
			led.setValue(value);
			System.out.println(String.format("%s led.value=%s value=%s", getTimestamp(), led.getValue(), value));

			sleep(SWITCH_PERIOD_MILLIS);

			value += VALUE_INCREMENT;
			if (value > VALUE_MAX)
				value = VALUE_MIN;
		}
		
		// finally switch off.
		led.setValue(0d);
	}
	
	private final void sleep(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			logger.error("sleep: " + e, e);
		}
	}
}
