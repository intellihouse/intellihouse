package house.intelli.raspi;

import house.intelli.core.bean.AbstractBean;
import house.intelli.core.rpc.RemoteBeanRef;

public class DimmerActorRemote extends AbstractBean<DimmerActor.Property> implements DimmerActor, Remote {

	public DimmerActorRemote() {
	}

	private String beanName;
	private RemoteBeanRef remoteBeanRef;
	private int dimmerValue;

	public static enum PropertyEnum implements DimmerActor.Property {
		remoteBeanRef
	}

	@Override
	public String getBeanName() {
		return beanName;
	}
	@Override
	public void setBeanName(String beanName) {
		this.beanName = beanName;
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
	public int getDimmerValue() {
		return dimmerValue;
	}

	@Override
	public void setDimmerValue(int dimmerValue) {
		setPropertyValue(DimmerActor.PropertyEnum.dimmerValue, dimmerValue);
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + '[' + toString_getProperties() + ']';
	}

	protected String toString_getProperties() {
		return "beanName=" + beanName;
	}
}
