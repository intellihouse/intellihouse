package house.intelli.raspi;

import static house.intelli.core.event.EventQueue.*;
import static house.intelli.core.util.AssertUtil.*;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import house.intelli.core.bean.AbstractBean;

public class RelayControllerImpl extends AbstractBean<RelayActor.Property> implements RelayActor, AutoCloseable {
	private final Logger logger = LoggerFactory.getLogger(RelayControllerImpl.class);

	public static enum PropertyEnum implements RelayActor.Property {
		latching,
		downCount,
		down,
		energized
	}

	private List<KeyButtonSensorImpl> keyButtons = new ArrayList<>();
	private List<RelayActorImpl> powerSupplies = new ArrayList<>();

	private boolean latching;
	private boolean inverse;
	private int downCount;
	private boolean down;
	private boolean energized;

	public RelayControllerImpl() {
	}

	public List<KeyButtonSensorImpl> getKeyButtons() {
		return keyButtons;
	}
	public void setKeyButtons(List<KeyButtonSensorImpl> keyButtonSensorImpls) {
		this.keyButtons = keyButtonSensorImpls == null ? new ArrayList<>() : keyButtonSensorImpls;
	}

	public List<RelayActorImpl> getPowerSupplies() {
		return powerSupplies;
	}
	public void setPowerSupplies(List<RelayActorImpl> powerSupplies) {
		this.powerSupplies = powerSupplies == null ? new ArrayList<>() : powerSupplies;
	}

	private final PropertyChangeListener keyButtonDownListener = new PropertyChangeListener() {
		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			boolean down = (boolean) evt.getNewValue();
			if (down)
				onKeyButtonDown();
			else
				onKeyButtonUp();
		}
	};

	public void init() {
		assertEventThread();
		keyButtons = Collections.unmodifiableList(keyButtons);
		powerSupplies = Collections.unmodifiableList(powerSupplies);

		int downCount = 0;
		for (KeyButtonSensorImpl keyButton : keyButtons) {
			keyButton.addPropertyChangeListener(KeyButtonSensor.PropertyEnum.down, keyButtonDownListener);
			if (keyButton.isDown())
				++downCount;
		}
		setDownCount(downCount);
		// Since downCount is very likely 0 and thus unchanged, no event is triggered and thus
		// nothing applied. We thus call applyDown(...) now.
		applyDown(isDown());
	}

	private void onKeyButtonDown() {
		assertEventThread();
		logger.debug("onKeyButtonDown");
		setDownCount(getDownCount() + 1);
	}

	private void onKeyButtonUp() {
		assertEventThread();
		logger.debug("onKeyButtonUp");
		setDownCount(getDownCount() - 1);
	}

	/**
	 * Is the key latching? Default value: <code>false</code>
	 * <p>
	 * If <code>false</code>, the relay state is changed whenever the key is pushed (and released) once.
	 * Thus pushing and releasing the key twice switches the relay back to its original state. If it
	 * was off (not energized) before, it first turns on and then off again.
	 * <p>
	 * If <code>true</code>, the relay's {@link RelayActor#isEnergized() energized} state is kept
	 * in-sync with the key-button's {@link KeyButtonSensor#isDown() down} state. Depending on
	 * {@link #isInverse() inverse}, both values are either always the same
	 * (<code>down == false =&gt; energized == false</code> and <code>down == true =&gt; energized == true</code>),
	 * or inverted (<code>down == true =&gt; energized == false</code>).
	 * <p>
	 * Note: If there are multiple {@link #getKeyButtons() keyButtons}, the overall
	 * {@link KeyButtonSensor#isDown() down} state is assumed to be
	 * <code>true</code> as soon as at least one key-button's {@code down} is <code>true</code>.
	 *
	 * @return Is the key latching?
	 */
	public boolean isLatching() {
		assertEventThread();
		return latching;
	}
	public void setLatching(boolean fridgeMode) {
		assertEventThread();
		setPropertyValue(PropertyEnum.latching, fridgeMode);
	}

	/**
	 * If the key {@linkplain #isLatching() is latching}, map the key's {@link KeyButtonSensor#isDown() down}
	 * state to the <i>inverted</i> {@link RelayActor#isEnergized() energized} state?
	 * <p>
	 * If the key is non-latching, this property is ignored.
	 *
	 * @return whether to invert the mapping from {@link KeyButtonSensor#isDown() KeyButtonSensor.down}
	 * to {@link RelayActor#isEnergized() RelayActor.energized}.
	 */
	public boolean isInverse() {
		assertEventThread();
		return inverse;
	}
	public void setInverse(boolean inverse) {
		assertEventThread();
		this.inverse = inverse;
	}

	public boolean isDown() {
		assertEventThread();
		return down;
	}
	protected void setDown(boolean down) {
		assertEventThread();
		if (setPropertyValue(PropertyEnum.down, down))
			applyDown(down);
	}

	public int getDownCount() {
		assertEventThread();
		return downCount;
	}
	protected void setDownCount(int downCount) {
		assertEventThread();
		if (downCount < 0)
			throw new IllegalArgumentException("downCount < 0");

		setPropertyValue(PropertyEnum.downCount, downCount);
		setDown(downCount > 0);
	}

	@Override
	public boolean isEnergized() {
		assertEventThread();
		return energized;
	}
	@Override
	public void setEnergized(boolean energized) {
		assertEventThread();
		if (setPropertyValue(PropertyEnum.energized, energized))
			applyEnergized(energized);
	}

	protected void applyDown(boolean down) {
		assertEventThread();
		if (isLatching()) {
			boolean energized = isInverse() ? ! down : down;
			setEnergized(energized);
		}
		else {
			if (down)
				setEnergized(! isEnergized());
		}
	}

	private void applyEnergized(boolean energized) {
		assertEventThread();
		for (RelayActorImpl powerSupply : powerSupplies) {
			powerSupply.setEnergized(energized);
		}
	}

	@Override
	public void close() {
		invokeAndWait(new Runnable() {
			@Override
			public void run() {
				_close();
			}
		});
	}

	protected void _close() {
		assertEventThread();

		for (KeyButtonSensorImpl keyButton : keyButtons) {
			keyButton.removePropertyChangeListener(KeyButtonSensor.PropertyEnum.down, keyButtonDownListener);
		}
		keyButtons = new ArrayList<>(keyButtons);
		powerSupplies = new ArrayList<>(powerSupplies);
	}

}
