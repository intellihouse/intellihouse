package house.intelli.core.rpc;

import static java.util.Objects.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import house.intelli.core.jaxb.IntelliHouseJaxbContext;
import house.intelli.core.service.ServiceRegistry;
import house.intelli.core.service.ServiceRegistryListener;

public class RpcServiceRegistry {

	private final Object mutex = new Object();
	private final Map<Class<?>, List<RpcService<Request<?>, Response>>> requestType2RpcServices = new HashMap<>();

	@SuppressWarnings("rawtypes")
	private final ServiceRegistryListener<RpcService> serviceRegistryListener = event -> reset();

	private static final RpcServiceRegistry instance = new RpcServiceRegistry();

	public static RpcServiceRegistry getInstance() {
		return instance;
	}

	protected RpcServiceRegistry() {
	}

	public List<RpcService<?, ?>> getRpcServices() {
		List<RpcService<?, ?>> result = new ArrayList<>();
		synchronized (mutex) {
			if (requestType2RpcServices.isEmpty())
				load();

			for (List<RpcService<Request<?>, Response>> services : requestType2RpcServices.values()) {
				for (RpcService<Request<?>, Response> service : services) {
					result.add(service.clone());
				}
			}
		}
		return result;
	}

	public <REQ extends Request<?>, RES extends Response> RpcService<REQ, RES> getRpcService(final Class<? extends REQ> requestType) {
		Class<?> rt = requireNonNull(requestType, "requestType");
		synchronized (mutex) {
			if (requestType2RpcServices.isEmpty())
				load();

			while (rt != Object.class) {
				List<RpcService<Request<?>, Response>> list = requestType2RpcServices.get(rt);
				if (list != null && ! list.isEmpty()) {
					RpcService<Request<?>, Response> rpcService = list.get(0);
					@SuppressWarnings("unchecked")
					RpcService<REQ, RES> result = (RpcService<REQ, RES>) rpcService.clone();
					return result;
				}

				rt = rt.getSuperclass();
			}
		}
		return null;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected void load() {
		Map<Class<?>, List<RpcService<Request<?>, Response>>> requestType2RpcServices = new HashMap<>();

		final ServiceRegistry<RpcService> serviceRegistry = ServiceRegistry.getInstance(RpcService.class);

		serviceRegistry.removeListener(serviceRegistryListener); // in case it already was added before
		serviceRegistry.addListener(serviceRegistryListener);

		for (RpcService<Request<?>, Response> rpcService : serviceRegistry.getServices()) {
			Class<?> requestType = rpcService.getRequestType();
			List<RpcService<Request<?>, Response>> rpcServices = requestType2RpcServices.get(requestType);
			if (rpcServices == null) {
				rpcServices = new ArrayList<>();
				requestType2RpcServices.put(requestType, rpcServices);
			}
			rpcServices.add(rpcService);
		}

		for (List<RpcService<Request<?>, Response>> list : requestType2RpcServices.values())
			Collections.sort(list, rpcServiceComparator);

		synchronized (mutex) {
			this.requestType2RpcServices.clear();
			this.requestType2RpcServices.putAll(requestType2RpcServices);
		}
	}

	private static Comparator<RpcService<?, ?>> rpcServiceComparator = new Comparator<RpcService<?,?>>() {
		@Override
		public int compare(RpcService<?, ?> o1, RpcService<?, ?> o2) {
			int result = -1 * Integer.compare(o1.getPriority(), o2.getPriority());
			if (result != 0)
				return result;

			return o1.getClass().getName().compareTo(o2.getClass().getName());
		}
	};

	public void reset() {
		synchronized (mutex) {
			this.requestType2RpcServices.clear();
		}
		IntelliHouseJaxbContext.reset();
	}
}
