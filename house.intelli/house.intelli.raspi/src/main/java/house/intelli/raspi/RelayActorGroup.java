package house.intelli.raspi;

import static house.intelli.core.util.AssertUtil.*;
import static java.util.Objects.*;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.BeanNameAware;

import house.intelli.core.bean.AbstractBean;

public abstract class RelayActorGroup extends AbstractBean<RelayActor.Property> implements BeanNameAware {

	private String beanName;
	private List<RelayActor> input = new ArrayList<>();
	private RelayActor output;

	private final PropertyChangeListener inputEnergizedListener = new PropertyChangeListener() {
		@Override
		public void propertyChange(PropertyChangeEvent event) {
			RelayActor inputRelayActor = (RelayActor) event.getSource();
			Boolean oldEnergized = (Boolean) event.getOldValue();
			Boolean newEnergized = (Boolean) event.getNewValue();
			onInputEnergizedChanged(
					requireNonNull(inputRelayActor, "inputRelayActor"),
					requireNonNull(oldEnergized, "oldEnergized"),
					requireNonNull(newEnergized, "newEnergized"));
		}
	};

	public String getBeanName() {
		return beanName;
	}
	@Override
	public void setBeanName(String beanName) {
		this.beanName = beanName;
	}

  public List<RelayActor> getInput() {
		return input;
	}
	public void setInput(List<RelayActor> input) {
		this.input = input == null ? new ArrayList<>() : input;
	}

	public RelayActor getOutput() {
		return output;
	}
	public void setOutput(RelayActor output) {
		this.output = output;
	}

	public void init() {
		assertEventThread();

		for (RelayActor inputRelayActor : getInput())
			inputRelayActor.addPropertyChangeListener(RelayActor.PropertyEnum.energized, inputEnergizedListener);
	}

	protected abstract void onInputEnergizedChanged(RelayActor inputRelayActor, boolean oldEnergized, boolean newEnergized);

	@Override
	public String toString() {
		return getClass().getSimpleName() + '[' + toString_getProperties() + ']';
	}

	protected String toString_getProperties() {
		return "beanName=" + beanName;
	}
}
