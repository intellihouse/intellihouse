package house.intelli.core.bean;

import java.beans.PropertyChangeListener;

public interface Bean<P extends PropertyBase> {

	void addPropertyChangeListener(PropertyChangeListener listener);

	void removePropertyChangeListener(PropertyChangeListener listener);

	void addPropertyChangeListener(P property, PropertyChangeListener listener);

	void removePropertyChangeListener(P property, PropertyChangeListener listener);
}
