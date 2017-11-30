package house.intelli.raspi;

import org.springframework.beans.factory.BeanNameAware;

import house.intelli.core.bean.Bean;
import house.intelli.core.bean.PropertyBase;

public interface RelayActor extends Bean<RelayActor.Property>, BeanNameAware {
	static interface Property extends PropertyBase { }

	static enum PropertyEnum implements Property {
		beanName,
		energized
	}

	String getBeanName();

	@Override
	void setBeanName(String beanName);

	boolean isEnergized();

	void setEnergized(boolean energized);
}
