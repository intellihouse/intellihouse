package house.intelli.raspi;

import house.intelli.core.bean.Bean;
import house.intelli.core.bean.PropertyBase;

public interface DimmerActor extends Bean<DimmerActor.Property> {

	int MIN_DIMMER_VALUE = 0;
	int MAX_DIMMER_VALUE = 100;

	int getDimmerValue();

	void setDimmerValue(int dimmerValue);

	static interface Property extends PropertyBase { }

	static enum PropertyEnum implements Property {
		dimmerValue
	}
}
