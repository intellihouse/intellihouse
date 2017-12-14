package house.intelli.raspi;

import static house.intelli.core.util.AssertUtil.*;

import house.intelli.core.bean.AbstractBean;

public class RelayActorVirtual extends AbstractBean<RelayActor.Property> implements RelayActor {

	private String beanName;
	private boolean energized;

	@Override
	public String getBeanName() {
		return beanName;
	}

	@Override
	public void setBeanName(String beanName) {
		this.beanName = beanName;
	}

	@Override
	public boolean isEnergized() {
		assertEventThread();
		return energized;
	}

  @Override
	public void setEnergized(boolean energized) {
		assertEventThread();
		setPropertyValue(RelayActor.PropertyEnum.energized, energized);
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + '[' + toString_getProperties() + ']';
	}

	protected String toString_getProperties() {
		return "beanName=" + beanName;
	}
}
