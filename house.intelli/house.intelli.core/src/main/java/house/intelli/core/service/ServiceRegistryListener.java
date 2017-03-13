package house.intelli.core.service;

import java.util.EventListener;

public interface ServiceRegistryListener<S> extends EventListener {

	void onServiceRegistryChanged(ServiceRegistryEvent<S> event);

}
