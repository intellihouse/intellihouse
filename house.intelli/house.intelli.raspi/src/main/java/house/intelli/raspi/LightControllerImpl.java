package house.intelli.raspi;

import static house.intelli.core.event.EventQueue.*;
import static house.intelli.core.util.AssertUtil.*;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import house.intelli.core.bean.AbstractBean;

public class LightControllerImpl extends AbstractBean<DimmerActor.Property> implements DimmerActor, AutoCloseable {
	private final Logger logger = LoggerFactory.getLogger(LightControllerImpl.class);

	public static enum PropertyEnum implements DimmerActor.Property {
		lightDimmerValuesIndex,
		lightOn,
		switchOffOnKeyButtonUp,
		dimDirection
	}

	protected static enum DimDirection {
		DOWN,
		UP
	}

	public static final int[] LIGHT_DIMMER_VALUES = { // percentages
			12, //   1
			25, //   2
			37, //   4
			50, //   8
			62, //  16
			75, //  32
			87, //  64
			100 // 100
	};

	private int lightDimmerValuesIndex = -1;
	private Boolean lightOn;

	private List<KeyButtonSensorImpl> keyButtons = new ArrayList<>();
	private List<DimmerActorImpl> lights = new ArrayList<>();
	private List<RelayActorImpl> powerSupplies = new ArrayList<>();
	private boolean switchOffOnKeyButtonUp;
	private DimDirection dimDirection = DimDirection.DOWN;
	private int dimmerValue;

	private static final Timer timer = new Timer("LightControllerImpl.timer", true);
	private TimerTask timerTask;

	public List<KeyButtonSensorImpl> getKeyButtons() {
		return keyButtons;
	}
	public void setKeyButtons(List<KeyButtonSensorImpl> keyButtonSensorImpls) {
		this.keyButtons = keyButtonSensorImpls == null ? new ArrayList<>() : keyButtonSensorImpls;
	}

	public List<DimmerActorImpl> getLights() {
		return lights;
	}
	public void setLights(List<DimmerActorImpl> lights) {
		this.lights = lights == null ? new ArrayList<>() : lights;
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

	private final PropertyChangeListener dimmerValueListener = new PropertyChangeListener() {
		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			onDimmerValueChange();
		}
	};

	protected boolean isSwitchOffOnKeyButtonUp() {
		assertEventThread();
		return switchOffOnKeyButtonUp;
	}
	protected void setSwitchOffOnKeyButtonUp(boolean switchOffOnKeyButtonUp) {
		assertEventThread();
		setPropertyValue(PropertyEnum.switchOffOnKeyButtonUp, switchOffOnKeyButtonUp);
	}

	protected DimDirection getDimDirection() {
		return dimDirection;
	}
	protected void setDimDirection(DimDirection dimDirection) {
		assertNotNull(dimDirection, "dimDirection");
		setPropertyValue(PropertyEnum.dimDirection, dimDirection);
	}

	private void onKeyButtonDown() {
		assertEventThread();
		logger.debug("onKeyButtonDown");

		if (isLightOn()) {
			setSwitchOffOnKeyButtonUp(true); // switching off deferred!
		}
		else {
			setLightOn(true); // immediate
		}

		if (timerTask != null) {
			timerTask.cancel();
			timerTask = null;
		}

		timerTask = new TimerTask() {
			@Override
			public void run() {
				invokeLater(new Runnable() {
					@Override
					public void run() {
						onTimerTaskRun();
					}
				});
			}
		};
		timer.schedule(timerTask, 1000L, 1000L);
	}

	private void onTimerTaskRun() {
		assertEventThread();
		logger.debug("onTimerTaskRun");
		setSwitchOffOnKeyButtonUp(false);
		int lightDimmerValuesIndex = getLightDimmerValuesIndex();
		if (getDimDirection() == DimDirection.DOWN) {
			if (--lightDimmerValuesIndex < 0) {
				setDimDirection(DimDirection.UP);
				++lightDimmerValuesIndex;
			}
		}
		else {
			if (++lightDimmerValuesIndex >= LIGHT_DIMMER_VALUES.length) {
				setDimDirection(DimDirection.DOWN);
				--lightDimmerValuesIndex;
			}
		}
		setLightDimmerValuesIndex(lightDimmerValuesIndex);
	}

	private void onKeyButtonUp() {
		assertEventThread();
		logger.debug("onKeyButtonUp");

		if (timerTask != null) {
			timerTask.cancel();
			timerTask = null;
		}

		if (isSwitchOffOnKeyButtonUp()) {
			setSwitchOffOnKeyButtonUp(false);
			setLightOn(false);
		}
		else {
			if (getDimDirection() == DimDirection.DOWN)
				setDimDirection(DimDirection.UP);
			else
				setDimDirection(DimDirection.DOWN);
		}
	}

	private void onDimmerValueChange() {
		assertEventThread();
		boolean energized = false;
		for (DimmerActorImpl light : lights) {
			int dimmerValue = light.getDimmerValue();
			if (DimmerActorImpl.MIN_DIMMER_VALUE != dimmerValue)
				energized = true;
		}
		for (RelayActorImpl powerSupply : powerSupplies) {
			powerSupply.setEnergized(energized);
		}
	}

	public void init() {
		assertEventThread();
		keyButtons = Collections.unmodifiableList(keyButtons);
		lights = Collections.unmodifiableList(lights);
		powerSupplies = Collections.unmodifiableList(powerSupplies);

		for (KeyButtonSensorImpl keyButton : keyButtons) {
			keyButton.addPropertyChangeListener(KeyButtonSensor.PropertyEnum.down, keyButtonDownListener);
		}
		for (DimmerActorImpl light : lights) {
			light.addPropertyChangeListener(DimmerActor.PropertyEnum.dimmerValue, dimmerValueListener);
		}
		setLightOn(false);
		setLightDimmerValuesIndex(LIGHT_DIMMER_VALUES.length - 1);
	}

	protected int getLightDimmerValuesIndex() {
		assertEventThread();
		return lightDimmerValuesIndex;
	}
	protected void setLightDimmerValuesIndex(int lightDimmerValuesIndex) {
		assertEventThread();
		if (lightDimmerValuesIndex < 0 || lightDimmerValuesIndex >= LIGHT_DIMMER_VALUES.length)
			throw new IllegalArgumentException("lightDimmerValuesIndex out of range!");

		setPropertyValue(PropertyEnum.lightDimmerValuesIndex, lightDimmerValuesIndex);
		setPropertyValue(DimmerActor.PropertyEnum.dimmerValue, LIGHT_DIMMER_VALUES[lightDimmerValuesIndex]);
		applyLightsDimmerValue();
	}

	public boolean isLightOn() {
		assertEventThread();
		return lightOn != null && lightOn.booleanValue();
	}
	public void setLightOn(boolean lightOn) {
		assertEventThread();
		if (setPropertyValue(PropertyEnum.lightOn, lightOn)) {
			_setDimmerValue(lightOn ? LIGHT_DIMMER_VALUES[lightDimmerValuesIndex] : MIN_DIMMER_VALUE);
			applyLightsDimmerValue();
		}
	}

	private void applyLightsDimmerValue() {
		assertEventThread();
		for (DimmerActorImpl light : lights) {
			light.setDimmerValue(isLightOn() ? LIGHT_DIMMER_VALUES[lightDimmerValuesIndex] : DimmerActorImpl.MIN_DIMMER_VALUE);
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
		for (DimmerActorImpl light : lights) {
			light.removePropertyChangeListener(DimmerActor.PropertyEnum.dimmerValue, dimmerValueListener);
		}
		keyButtons = new ArrayList<>(keyButtons);
		lights = new ArrayList<>(lights);
		powerSupplies = new ArrayList<>(powerSupplies);
	}

	@Override
	public int getDimmerValue() {
		return dimmerValue;
	}

	@Override
	public void setDimmerValue(final int dimmerValue) {
		if (getDimmerValue() == dimmerValue) {
			logger.debug("setDimmerValue: ignoring: bean={} old=dimmerValue={}", this, dimmerValue);
			return;
		}
		if (dimmerValue < MIN_DIMMER_VALUE)
			throw new IllegalArgumentException("dimmerValue < MIN_DIMMER_VALUE");

		if (dimmerValue > MAX_DIMMER_VALUE)
			throw new IllegalArgumentException("dimmerValue > MAX_DIMMER_VALUE");

		if (dimmerValue == MIN_DIMMER_VALUE) {
			_setDimmerValue(dimmerValue);
			setLightOn(false);
			return;
		}

		int bestDistance = Integer.MAX_VALUE;
		int bestIndex = -1;
		for (int i = 0; i < LIGHT_DIMMER_VALUES.length; i++) {
			int d = Math.abs(LIGHT_DIMMER_VALUES[i] - dimmerValue);
			if (d < bestDistance) {
				bestDistance = d;
				bestIndex = i;
			}
		}
		_setDimmerValue(dimmerValue);
		setLightDimmerValuesIndex(bestIndex);
		setLightOn(true);
	}

	protected void _setDimmerValue(final int dimmerValue) {
		setPropertyValue(DimmerActor.PropertyEnum.dimmerValue, dimmerValue);
	}
}
