package house.intelli.raspi;

import org.springframework.beans.factory.BeanNameAware;

import house.intelli.core.bean.Bean;
import house.intelli.core.bean.PropertyBase;
import house.intelli.raspi.pv.DataCollectorListener;

public interface PvDataCollector extends Bean<PvDataCollector.Property>, BeanNameAware {

	static interface Property extends PropertyBase { }

	static enum PropertyEnum implements Property {
		beanName,
		deviceName,
		interfaceType,
		interfaceAddress,
		collectPeriod
	}

	String getBeanName();

	@Override
	void setBeanName(String beanName);

	/**
	 * Gets the unique name (identifier) of the inverter/charger from which this {@code PvDataCollector} reads data.
	 * @return the unique name of the inverter/charger.
	 */
	String getDeviceName();

	void setDeviceName(String deviceName);

	StecaInterfaceType getInterfaceType();

	void setInterfaceType(StecaInterfaceType interfaceType);

	String getInterfaceAddress();

	void setInterfaceAddress(String interfaceAddress);

	long getCollectPeriod();

	void setCollectPeriod(long collectPeriod);

	void addDataCollectorListener(DataCollectorListener listener);

	void removeDataCollectorListener(DataCollectorListener listener);
}
