package co.codewizards.raspi1;

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalInput;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.GpioPinPwmOutput;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;

public class SwitchInDemo implements Runnable {
	private final Logger logger = LoggerFactory.getLogger(SwitchInDemo.class);
	
	public static final long PROGRAM_RUN_TIME_MILLIS = 5L * 60L * 1000L;
//	public static final long SWITCH_PERIOD_MILLIS = 10_000L;
	
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

	private String[] args;
	private volatile int ledDimmerIndex = LED_DIMMER_PWM.length -1; // max value
	private volatile boolean dimDirectionDown = true;
	private long lastDimmerChangeTimestamp;

	private final GpioController gpio;
	private final GpioPinDigitalOutput bedRelay;
	private final GpioPinDigitalOutput dressingRelay;
	private final GpioPinDigitalOutput bathRelay;
	private final GpioPinPwmOutput bathDimmer1;
	private final GpioPinPwmOutput bathDimmer2;
	
	private volatile boolean switchOffOnKeyUp;
	private volatile long keyDownStartTimestamp = Long.MIN_VALUE;
	
	public SwitchInDemo(String[] args) {
		this.args = args;
		gpio = GpioFactory.getInstance();
		bedRelay = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_07);
		dressingRelay = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_08);
		bathRelay = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_09);
		bathDimmer1 = gpio.provisionSoftPwmOutputPin(RaspiPin.GPIO_04);
		bathDimmer2 = gpio.provisionSoftPwmOutputPin(RaspiPin.GPIO_05);
	}

	public void run() {
		// initially switch all off.
		bedRelay.setState(PinState.LOW);
		dressingRelay.setState(PinState.LOW);
		bathRelay.setState(PinState.LOW);

		final GpioPinDigitalInput key = gpio.provisionDigitalInputPin(RaspiPin.GPIO_10); // phys. pin 24

		GpioPinListenerDigital listener = new GpioPinListenerDigital() {
			@Override
			public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) {
				PinState keyPinState = event.getState();
				System.out.println(new Date() + " " + Thread.currentThread().getName() + " keyPinState=" + keyPinState + " bathRelayState=" + bathRelay.getState());

				if (keyPinState.isHigh()) {
					keyDownStartTimestamp = System.currentTimeMillis();
					if (bathRelay.getState() == PinState.HIGH)
						switchOffOnKeyUp = true;
					else
						switchOn();
				}
				else {
					keyDownStartTimestamp = Long.MIN_VALUE;
					if (switchOffOnKeyUp) {
						switchOffOnKeyUp = false;
						switchOff();
					} else
						dimDirectionDown = ! dimDirectionDown;
				}

				System.out.println(new Date() + " " + Thread.currentThread().getName() + " keyDown=" + isKeyDown() + " keyDownStartTimestamp=" + keyDownStartTimestamp + " switchOffOnKeyUp=" + switchOffOnKeyUp);
				System.out.println();
			}
		};
		key.addListener(listener);

//		long start = System.currentTimeMillis();
//
//		while (System.currentTimeMillis() - start <= PROGRAM_RUN_TIME_MILLIS) {
//			sleep(SWITCH_PERIOD_MILLIS);
//		}
//		
//		// finally switch off.
//		bathRelay.setState(PinState.LOW);
		while (true) {
			boolean keyDown = isKeyDown();
			long sleepTime = keyDown ? 100L : 1000L;
			System.out.println(new Date() + " " + Thread.currentThread().getName() + " keyDown = " + keyDown + " switchOffOnKeyUp=" + switchOffOnKeyUp + " sleepTime=" + sleepTime + " lastDimmerChangeTimestamp=" + lastDimmerChangeTimestamp);
			if (keyDown && getKeyDownDuration() > 1000L) {
				switchOffOnKeyUp = false;
				if (System.currentTimeMillis() - lastDimmerChangeTimestamp >= 3000L) {
					lastDimmerChangeTimestamp = System.currentTimeMillis();
					changeDimmer();
				}
			}
			sleep(sleepTime);
		}
	}

	private void changeDimmer() {
		int ldi = ledDimmerIndex;
		if (dimDirectionDown)
			--ldi;
		else
			++ldi;
		
		if (ldi < 0) {
			dimDirectionDown = ! dimDirectionDown;
			ldi += 2;
		}
		else if (ldi >= LED_DIMMER_PWM.length) {
			dimDirectionDown = ! dimDirectionDown;
			ldi -= 2;
		}
		ledDimmerIndex = ldi;
		
		System.out.println("changeDimmer: ledDimmerIndex=" + ldi);
		
		bathDimmer1.setPwm(LED_DIMMER_PWM[ldi]);
		bathDimmer2.setPwm(LED_DIMMER_PWM[ldi]);
	}
	
	private long getKeyDownDuration() {
		long kdst = keyDownStartTimestamp;
		if (kdst == Long.MIN_VALUE)
			return 0;

		return System.currentTimeMillis() - kdst;
	}
	
	private boolean isKeyDown() {
		return keyDownStartTimestamp != Long.MIN_VALUE;
	}

	protected void switchOff() {
		bathRelay.setState(PinState.LOW);
		bathDimmer1.setPwm(0);
		bathDimmer2.setPwm(0);
	}
	
	protected void switchOn() {
		if (bathRelay.getState().isHigh())
			return; // already switched on.
		
		bathRelay.setState(PinState.HIGH);
		int ldi = ledDimmerIndex;
		bathDimmer1.setPwm(LED_DIMMER_PWM[ldi]);
		bathDimmer2.setPwm(LED_DIMMER_PWM[ldi]);
	}
	
	private final void sleep(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			logger.error("sleep: " + e, e);
		}
	}
}
