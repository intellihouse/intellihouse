package house.intelli.core.rpc.lightcontroller;

import static house.intelli.core.util.Util.*;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class LightControllerState {

	private int lightDimmerValuesIndex;
	private int dimmerValue;
	private Boolean lightOn;
	private DimDirection dimDirection = DimDirection.DOWN;
	private int autoOffPeriod;

	public int getLightDimmerValuesIndex() {
		return lightDimmerValuesIndex;
	}
	public void setLightDimmerValuesIndex(int lightDimmerValuesIndex) {
		this.lightDimmerValuesIndex = lightDimmerValuesIndex;
	}
	public int getDimmerValue() {
		return dimmerValue;
	}
	public void setDimmerValue(int dimmerValue) {
		this.dimmerValue = dimmerValue;
	}
	public Boolean getLightOn() {
		return lightOn;
	}
	public void setLightOn(Boolean lightOn) {
		this.lightOn = lightOn;
	}
	public DimDirection getDimDirection() {
		return dimDirection;
	}
	public void setDimDirection(DimDirection dimDirection) {
		this.dimDirection = dimDirection;
	}
	public int getAutoOffPeriod() {
		return autoOffPeriod;
	}
	public void setAutoOffPeriod(int autoOffPeriod) {
		this.autoOffPeriod = autoOffPeriod;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((dimDirection == null) ? 0 : dimDirection.hashCode());
		result = prime * result + dimmerValue;
		result = prime * result + lightDimmerValuesIndex;
		result = prime * result + ((lightOn == null) ? 0 : lightOn.hashCode());
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;

		final LightControllerState other = (LightControllerState) obj;
		return equal(this.dimDirection, other.dimDirection)
				&& equal(this.dimmerValue, other.dimmerValue)
				&& equal(this.lightDimmerValuesIndex, other.lightDimmerValuesIndex)
				&& equal(this.lightOn, other.lightOn);
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + '[' + toString_getProperties() + ']';
	}
	protected String toString_getProperties() {
		return "lightDimmerValuesIndex=" + lightDimmerValuesIndex
				+ ", dimmerValue=" + dimmerValue
				+ ", lightOn=" + lightOn
				+ ", dimDirection=" + dimDirection;
	}
}
