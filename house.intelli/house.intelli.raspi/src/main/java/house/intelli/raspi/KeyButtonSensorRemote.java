package house.intelli.raspi;

import static house.intelli.core.util.AssertUtil.*;

import house.intelli.core.bean.AbstractBean;

public class KeyButtonSensorRemote extends AbstractBean<KeyButtonSensor.Property> implements KeyButtonSensor, Remote {

	private RemoteBeanRef remoteBeanRef;
	private boolean down;

	public static enum PropertyEnum implements KeyButtonSensor.Property {
		remoteBeanRef
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

}
