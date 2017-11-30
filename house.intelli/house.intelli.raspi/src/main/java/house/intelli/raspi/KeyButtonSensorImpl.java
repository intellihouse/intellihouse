package house.intelli.raspi;

import static house.intelli.core.event.EventQueue.*;
import static house.intelli.core.util.AssertUtil.*;

import java.util.Timer;
import java.util.TimerTask;

import org.springframework.beans.factory.BeanNameAware;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalInput;
import com.pi4j.io.gpio.Pin;
import com.pi4j.io.gpio.PinPullResistance;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;

import house.intelli.core.bean.AbstractBean;

public class KeyButtonSensorImpl extends AbstractBean<KeyButtonSensor.Property> implements KeyButtonSensor, AutoCloseable, BeanNameAware {

	public static enum PropertyEnum implements KeyButtonSensor.Property {
		pin,
		inverse
	}

	private static final long DEBOUNCE_PERIOD = 100;

	private String beanName;
	private Pin pin;
	private boolean inverse;
	private GpioPinDigitalInput digitalInput;

	private boolean down;
	private PinState debounceCandidatePinState;
	private final Timer debounceTimer = new Timer("KeyButtonSensorImpl.debounceTimer@" + Integer.toHexString(System.identityHashCode(this)), true);
	private TimerTask debounceTimerTask;

	private GpioPinListenerDigital listener = new GpioPinListenerDigital() {
		@Override
		public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) {
			final PinState state = event.getState();
			invokeLater(() -> setDebounceCandidatePinState(state));
		}
	};

	private void setDebounceCandidatePinState(final PinState state) {
		assertNotNull(state, "state");
		assertEventThread();
		if (debounceCandidatePinState == state)
			return;

		debounceCandidatePinState = state;
		scheduleDebounceTimerTask();
	}

	private void scheduleDebounceTimerTask() {
		assertEventThread();
		if (debounceTimerTask != null) {
			debounceTimerTask.cancel();
			debounceTimerTask = null;
		}

		debounceTimerTask = new TimerTask() {
			@Override
			public void run() {
				invokeLater(() -> {
					debounceTimerTask = null;
					PinState state = digitalInput.getState();
					if (state == debounceCandidatePinState)
						_setDown(state);
					else
						setDebounceCandidatePinState(state);
				});
			}
		};
		debounceTimer.schedule(debounceTimerTask, DEBOUNCE_PERIOD);
	}

	@Override
	public String getBeanName() {
		return beanName;
	}
	@Override
	public void setBeanName(String beanName) {
		setPropertyValue(KeyButtonSensor.PropertyEnum.beanName, beanName);
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

	public void init() {
		assertEventThread();
		assertNotNull(pin, "pin");
		openDigitalInput();
	}

	@Override
	public boolean isDown() {
		assertEventThread();
		return down;
	}

	@Override
	public void setDown(boolean down) {
		assertEventThread();
		setPropertyValue(KeyButtonSensor.PropertyEnum.down, down);
	}

	protected void _setDown(PinState pinState) {
		if (isInverse())
			setDown(PinState.LOW == pinState);
		else
			setDown(PinState.HIGH == pinState);
	}

	/**
	 * Invert the mapping from the input-pin's electronic state to the logical {@link #isDown() down} state?
	 * <p>
	 * If this is <code>false</code> (the default), {@link PinState#HIGH} is mapped to
	 * {@link #isDown() down}{@code == true} and {@link PinState#LOW} is mapped to {@link #isDown() down} {@code == false}.
	 * <p>
	 * If this is <code>true</code>, the mapping is inverted, i.e. {@link PinState#LOW} is mapped to
	 * {@link #isDown() down}{@code == true} and {@link PinState#HIGH} is mapped to {@link #isDown() down} {@code == false}.
	 *
	 * @return whether to invert the mapping from {@link PinState#HIGH} / {@link PinState#LOW}
	 * to {@link #isDown() down} being <code>true</code> / <code>false</code>.
	 */
	public boolean isInverse() {
		assertEventThread();
		return inverse;
	}
	public void setInverse(boolean inverse) {
		assertEventThread();
		setPropertyValue(PropertyEnum.inverse, inverse);
	}

	private void openDigitalInput() {
		if (digitalInput != null)
			return;

		assertEventThread();
		assertNotNull(pin, "pin");

		GpioController gpioController = GpioFactory.getInstance();
		digitalInput = gpioController.provisionDigitalInputPin(pin);

		// The lib's debounce implementation does not filter false positives. It seems to react immediately, hence
		// we implement our own debounce -- see above.
		digitalInput.setDebounce(0);
		digitalInput.setPullResistance(PinPullResistance.OFF);
		digitalInput.addListener(listener);
		_setDown(digitalInput.getState());
	}

	private void closeDigitalInput() {
		assertEventThread();
		if (digitalInput == null)
			return;

		digitalInput.removeListener(listener);
		GpioController gpioController = GpioFactory.getInstance();
		gpioController.unprovisionPin(digitalInput);
		digitalInput = null;
	}

	@Override
	public void close() {
		invokeAndWait(new Runnable() {
			@Override
			public void run() {
				closeDigitalInput();
			}
		});
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + '[' + toString_getProperties() + ']';
	}

	protected String toString_getProperties() {
		return "beanName=" + beanName;
	}
}
