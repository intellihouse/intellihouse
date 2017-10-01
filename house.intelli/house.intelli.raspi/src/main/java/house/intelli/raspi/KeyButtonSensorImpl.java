package house.intelli.raspi;

import static house.intelli.core.event.EventQueue.*;
import static house.intelli.core.util.AssertUtil.*;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalInput;
import com.pi4j.io.gpio.Pin;
import com.pi4j.io.gpio.PinPullResistance;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;

import house.intelli.core.bean.AbstractBean;

public class KeyButtonSensorImpl extends AbstractBean<KeyButtonSensor.Property> implements KeyButtonSensor, AutoCloseable {

	public static enum PropertyEnum implements KeyButtonSensor.Property {
		pin,
		inverse
	}

	private Pin pin;
	private boolean inverse;
	private GpioPinDigitalInput digitalInput;

	private boolean down;

	private GpioPinListenerDigital listener = new GpioPinListenerDigital() {
		@Override
		public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) {
			final PinState state = event.getState();
			invokeLater(new Runnable() {
				@Override
				public void run() {
					_setDown(state);
				}
			});
		}
	};

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
		if (isInverse()) {
			digitalInput.setDebounce(100, PinState.LOW); // logical 'down'
			digitalInput.setDebounce(200, PinState.HIGH);
			digitalInput.setPullResistance(PinPullResistance.PULL_UP);
		}
		else {
			digitalInput.setDebounce(100, PinState.HIGH); // logical 'down'
			digitalInput.setDebounce(200, PinState.LOW);
			digitalInput.setPullResistance(PinPullResistance.PULL_DOWN);
		}
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
}
