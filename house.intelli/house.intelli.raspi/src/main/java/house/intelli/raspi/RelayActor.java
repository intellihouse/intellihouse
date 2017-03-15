package house.intelli.raspi;

import house.intelli.core.bean.Bean;
import house.intelli.core.bean.PropertyBase;

public interface RelayActor extends Bean<RelayActor.Property> {
	static interface Property extends PropertyBase { }

	static enum PropertyEnum implements Property {
		energized
	}
}
