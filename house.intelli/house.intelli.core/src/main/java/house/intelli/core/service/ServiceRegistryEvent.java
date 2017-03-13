package house.intelli.core.service;

import java.util.EventObject;

@SuppressWarnings("serial")
public class ServiceRegistryEvent<S> extends EventObject {

	public ServiceRegistryEvent(ServiceRegistry<S> serviceRegistry) {
		super(serviceRegistry);
	}

}
