package house.intelli.core.service;

public abstract class AbstractServiceRegistryDelegate<S> implements ServiceRegistryDelegate<S>, AutoCloseable {

	private ServiceRegistry<S> serviceRegistry;

	@Override
	public ServiceRegistry<S> getServiceRegistry() {
		return serviceRegistry;
	}
	@Override
	public void setServiceRegistry(ServiceRegistry<S> serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
	}

	@Override
  public void close() {
      final ServiceRegistry<S> serviceRegistry = getServiceRegistry();
      if (serviceRegistry != null) {
          serviceRegistry.removeDelegate(this);
      }
  }
}
