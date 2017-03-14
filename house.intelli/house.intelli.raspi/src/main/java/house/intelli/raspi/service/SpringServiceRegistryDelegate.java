package house.intelli.raspi.service;

import static house.intelli.core.util.AssertUtil.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.context.ApplicationContext;

import house.intelli.core.service.AbstractServiceRegistryDelegate;

public class SpringServiceRegistryDelegate<S> extends AbstractServiceRegistryDelegate<S> {

	private final Class<S> serviceClass;
	private final ApplicationContext applicationContext;

	public SpringServiceRegistryDelegate(Class<S> serviceClass, final ApplicationContext applicationContext) {
		this.serviceClass = assertNotNull(serviceClass, "serviceClass");
		this.applicationContext = assertNotNull(applicationContext, "applicationContext");
	}

	public Class<S> getServiceClass() {
		return serviceClass;
	}

	@Override
	public List<S> getServices() {
		Map<String, S> beansOfType = applicationContext.getBeansOfType(serviceClass);
		return new ArrayList<>(beansOfType.values());
	}
}
