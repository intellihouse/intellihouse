package house.intelli.raspi;

import static house.intelli.core.util.AssertUtil.*;

import house.intelli.core.bean.AbstractBean;
import house.intelli.core.rpc.RemoteBeanRef;

public class KeyButtonSensorRemote extends AbstractBean<KeyButtonSensor.Property> implements KeyButtonSensor, Remote {

	private String beanName;
	private RemoteBeanRef remoteBeanRef;
	private boolean down;

	public static enum PropertyEnum implements KeyButtonSensor.Property {
		remoteBeanRef
	}

	@Override
	public String getBeanName() {
		return beanName;
	}
	@Override
	public void setBeanName(String beanName) {
		setPropertyValue(KeyButtonSensor.PropertyEnum.beanName, beanName);
	}

	@Override
	public RemoteBeanRef getRemoteBeanRef() {
		return remoteBeanRef;
	}

	@Override
	public void setRemoteBeanRef(RemoteBeanRef remoteBeanRef) {
		setPropertyValue(PropertyEnum.remoteBeanRef, remoteBeanRef);
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

	@Override
	public String toString() {
		return getClass().getSimpleName() + '[' + toString_getProperties() + ']';
	}

	protected String toString_getProperties() {
		return "beanName=" + beanName;
	}
}
