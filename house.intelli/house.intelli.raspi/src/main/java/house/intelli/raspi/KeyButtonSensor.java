package house.intelli.raspi;

import org.springframework.beans.factory.BeanNameAware;

import house.intelli.core.bean.Bean;
import house.intelli.core.bean.PropertyBase;

public interface KeyButtonSensor extends Bean<KeyButtonSensor.Property>, BeanNameAware {

	static interface Property extends PropertyBase { }

	static enum PropertyEnum implements Property {
		beanName,
		down
	}

	String getBeanName();

	@Override
	void setBeanName(String beanName);

	boolean isDown();

	void setDown(boolean down);

}
