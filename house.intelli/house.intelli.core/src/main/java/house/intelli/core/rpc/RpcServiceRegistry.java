package house.intelli.core.rpc;

import static house.intelli.core.util.AssertUtil.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

public class RpcServiceRegistry {

	private final RpcContext rpcContext;

	private final Object mutex = new Object();
	private final Map<Class<?>, List<Class<RpcService<?, ?>>>> requestType2RpcServiceClasses = new HashMap<>();

	protected RpcServiceRegistry(RpcContext rpcContext) {
		this.rpcContext = assertNotNull(rpcContext, "rpcContext");
	}

	public RpcContext getRpcContext() {
		return rpcContext;
	}

	public <REQ extends Request, RES extends Response> RpcService<REQ, RES> createRpcService(final Class<? extends REQ> requestType) {
		Class<?> rt = assertNotNull(requestType, "requestType");
		synchronized (mutex) {
			if (requestType2RpcServiceClasses.isEmpty())
				load();

			while (rt != Object.class) {
				List<Class<RpcService<?, ?>>> list = requestType2RpcServiceClasses.get(rt);
				if (list != null && ! list.isEmpty()) {
					Class<RpcService<?, ?>> rpcServiceClass = list.get(0);
					try {
						@SuppressWarnings("unchecked")
						RpcService<REQ, RES> rpcService = (RpcService<REQ, RES>) rpcServiceClass.newInstance();
						rpcService.setRpcContext(rpcContext);
						return rpcService;
					} catch (RuntimeException e) {
						throw e;
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
				}

				rt = rt.getSuperclass();
			}
		}
		return null;
	}

	protected void load() {
		Map<Class<?>, List<RpcService<?, ?>>> requestType2RpcServices = new HashMap<>();

		@SuppressWarnings("rawtypes")
		Iterator<RpcService> it = ServiceLoader.load(RpcService.class).iterator();
		while (it.hasNext()) {
			RpcService<?, ?> rpcService = it.next();
			Class<?> requestType = rpcService.getRequestType();
			List<RpcService<?, ?>> rpcServices = requestType2RpcServices.get(requestType);
			if (rpcServices == null) {
				rpcServices = new ArrayList<>();
				requestType2RpcServices.put(requestType, rpcServices);
			}
			rpcServices.add(rpcService);
		}

		Map<Class<?>, List<Class<RpcService<?, ?>>>> requestType2RpcServiceClasses = new HashMap<>();
		for (Map.Entry<Class<?>, List<RpcService<?, ?>>> me : requestType2RpcServices.entrySet()) {
			List<RpcService<?, ?>> rpcServices = me.getValue();
			Collections.sort(rpcServices, rpcServiceComparator);
			List<Class<RpcService<?, ?>>> rpcServiceClasses = new ArrayList<>(rpcServices.size());
			for (RpcService<?,?> rpcService : rpcServices) {
				@SuppressWarnings("unchecked")
				Class<RpcService<?, ?>> rpcServiceClass = (Class<RpcService<?, ?>>) rpcService.getClass();
				rpcServiceClasses.add(rpcServiceClass);
			}
			requestType2RpcServiceClasses.put(me.getKey(), rpcServiceClasses);
		}

		synchronized (mutex) {
			this.requestType2RpcServiceClasses.clear();
			this.requestType2RpcServiceClasses.putAll(requestType2RpcServiceClasses);
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
}
