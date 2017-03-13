package house.intelli.core.service;

import static house.intelli.core.util.AssertUtil.*;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ServiceLoader;

public class ServiceLoaderServiceRegistryDelegate<S> extends AbstractServiceRegistryDelegate<S> {

	private final Class<S> serviceClass;

	public ServiceLoaderServiceRegistryDelegate(Class<S> serviceClass) {
		this.serviceClass = assertNotNull(serviceClass, "serviceClass");
	}

	public Class<S> getServiceClass() {
		return serviceClass;
	}

	@Override
	public List<S> getServices() {
		Iterator<S> iterator = ServiceLoader.load(serviceClass).iterator();
		List<S> result = new LinkedList<>();
		while (iterator.hasNext())
			result.add(iterator.next());

		return result;
	}
}
