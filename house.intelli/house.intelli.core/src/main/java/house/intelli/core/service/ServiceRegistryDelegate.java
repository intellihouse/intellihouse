package house.intelli.core.service;

import java.util.List;

public interface ServiceRegistryDelegate<S> {

	ServiceRegistry<S> getServiceRegistry();

	void setServiceRegistry(ServiceRegistry<S> serviceRegistry);

	List<S> getServices();

}
