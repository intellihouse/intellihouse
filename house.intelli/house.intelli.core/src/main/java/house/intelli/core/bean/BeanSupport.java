package house.intelli.core.bean;

import static house.intelli.core.util.ReflectionUtil.*;
import static java.util.Objects.*;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import house.intelli.core.util.ReflectionUtil;

public class BeanSupport<B, P extends PropertyBase> {
	private static final Logger logger = LoggerFactory.getLogger(BeanSupport.class);

	private final B bean;
	private final PropertyChangeSupport propertyChangeSupport;

	private boolean getBeanNameMethodDoesNotExist;
	private Method getBeanNameMethod;

	public BeanSupport(final B bean) {
		this.bean = requireNonNull(bean, "bean");
		propertyChangeSupport = new PropertyChangeSupport(bean);
	}

	public B getBean() {
		return bean;
	}

	public boolean setPropertyValue(final P property, final Object value) {
		requireNonNull(property, "property");

		final Object old;
		synchronized (getMutex()) {
			old = getFieldValue(bean, property.name());
			if (isEqual(property, old, value)) {
				if (logger.isTraceEnabled())
					logger.trace("setPropertyValue: ignoring: bean={} property={} old=value={}",
							getBeanInstanceName(), property, value);

				return false;
			}

			if (logger.isDebugEnabled())
				logger.debug("setPropertyValue: setting: bean={} property={} old={} value={}",
						getBeanInstanceName(), property, old, value);

			setFieldValue(bean, property.name(), value);
		}

		// We *must* fire the event *outside* of the *synchronized* block to make sure the listeners
		// do not run into a dead-lock!
		firePropertyChange(property, old, value);
		return true;
	}

	public String getBeanInstanceName() {
		final String beanName = getBeanName();

		if (beanName == null)
			return bean.getClass().getSimpleName() + '@' + Integer.toHexString(System.identityHashCode(bean));
		else
			return bean.getClass().getSimpleName() + '[' + beanName + ']' + '@' + Integer.toHexString(System.identityHashCode(bean));
	}

	/**
	 * Gets the result of the method {@code getBeanName()} of the {@link #getBean() bean}, if such method
	 * exists -- otherwise <code>null</code>.
	 * @return the result of the bean's method {@code getBeanName()} or <code>null</code>.
	 */
	public String getBeanName() {
		final Method getBeanNameMethod = getGetBeanNameMethod();
		if (getBeanNameMethod == null)
			return null;

		try {
			return (String) getBeanNameMethod.invoke(bean);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | ClassCastException e) {
			return null;
		}
	}

	private Method getGetBeanNameMethod() {
		if (getBeanNameMethodDoesNotExist)
			return null;

		Method getBeanNameMethod = this.getBeanNameMethod;
		if (getBeanNameMethod == null) {
			getBeanNameMethod = ReflectionUtil.getDeclaredMethod(bean.getClass(), "getBeanName", null);
			if (getBeanNameMethod == null) {
				getBeanNameMethodDoesNotExist = true;
				return null;
			}
			if (String.class != getBeanNameMethod.getReturnType()) {
				getBeanNameMethodDoesNotExist = true;
				return null;
			}
			this.getBeanNameMethod = getBeanNameMethod;
		}
		return getBeanNameMethod;
	}

	public <V> V getPropertyValue(P property) {
		synchronized (getMutex()) {
			return getFieldValue(bean, property.name());
		}
	}

	protected Object getMutex() {
		return bean;
	}

	protected boolean isEqual(final P property, final Object oldValue, final Object newValue) {
		return oldValue == null ? newValue == null : oldValue.equals(newValue);
	}

	public void addPropertyChangeListener(final PropertyChangeListener listener) {
		requireNonNull(listener, "listener");
		propertyChangeSupport.addPropertyChangeListener(listener);
	}

	public void removePropertyChangeListener(final PropertyChangeListener listener) {
		requireNonNull(listener, "listener");
		propertyChangeSupport.removePropertyChangeListener(listener);
	}

	public void addPropertyChangeListener(final P property, final PropertyChangeListener listener) {
		requireNonNull(property, "property");
		requireNonNull(listener, "listener");
		propertyChangeSupport.addPropertyChangeListener(property.name(), listener);
	}

	public void removePropertyChangeListener(final P property, final PropertyChangeListener listener) {
		requireNonNull(property, "property");
		requireNonNull(listener, "listener");
		propertyChangeSupport.removePropertyChangeListener(property.name(), listener);
	}

	public void firePropertyChange(final P property, Object oldValue, Object newValue) {
		requireNonNull(property, "property");
		propertyChangeSupport.firePropertyChange(property.name(), oldValue, newValue);
	}
}
