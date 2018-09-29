package house.intelli.core.service;

import static java.util.Objects.*;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ServiceLoader;

public class ServiceLoaderServiceRegistryDelegate<S> extends AbstractServiceRegistryDelegate<S> {

	private final Class<S> serviceClass;

	public ServiceLoaderServiceRegistryDelegate(Class<S> serviceClass) {
		this.serviceClass = requireNonNull(serviceClass, "serviceClass");
	}

	public Class<S> getServiceClass() {
		return serviceClass;
	}

	@Override
	public List<S> getServices() {
		// We do *not* use the Thread-context-class-loader, because this causes trouble in OSGi.
		// We do *not* want things to be found in OSGi outside of the current bundle. There
		// are OSGi-specific ServiceLoaderServiceRegistryDelegate implementations!
		Iterator<S> iterator = ServiceLoader.load(serviceClass, serviceClass.getClassLoader()).iterator();
		List<S> result = new LinkedList<>();
		while (iterator.hasNext())
			result.add(iterator.next());

		return result;
	}
}
