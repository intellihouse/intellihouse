package house.intelli.core.service;

public abstract class AbstractServiceRegistryDelegate<S> implements ServiceRegistryDelegate<S> {

	private ServiceRegistry<S> serviceRegistry;

	@Override
	public ServiceRegistry<S> getServiceRegistry() {
		return serviceRegistry;
	}
	@Override
	public void setServiceRegistry(ServiceRegistry<S> serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
	}

}
