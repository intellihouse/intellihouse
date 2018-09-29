package house.intelli.core.rpc;

import static java.util.Objects.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import house.intelli.core.Uid;

public class InverseRequestRegistry {

	private static final Logger logger = LoggerFactory.getLogger(InverseRequestRegistry.class);

	private static final long EVICT_PERIOD = 60L * 60L * 1000L;

	private final RpcContext rpcContext;
	private final Object mutex = new Object();
	private final Map<HostId, Map<Uid, Request<?>>> serverHostId2RequestId2Request = new HashMap<>();
	private final Map<Uid, EvictDescriptor> requestId2EvictDescriptor = new HashMap<>();

	private final Timer evictTimer;
	private final TimerTask evictTimerTask;

	private static class EvictDescriptor {
		public final HostId serverHostId;
		public final Uid requestId;
		public final long created = System.currentTimeMillis();
		public final long timeout;
		public final long timeoutElapsed;

		public EvictDescriptor(Request<?> request) {
			requireNonNull(request, "request");
			serverHostId = requireNonNull(request.getServerHostId(), "request.serverHostId");
			requestId = requireNonNull(request.getRequestId(), "request.requestId");
			if (request.getTimeout() == Request.TIMEOUT_UNDEFINED)
				timeout = RpcConst.DEFAULT_REQUEST_TIMEOUT;
			else
				timeout = request.getTimeout();

			timeoutElapsed = created + timeout;
		}
	}

	public RpcContext getRpcContext() {
		return rpcContext;
	}

	protected InverseRequestRegistry(RpcContext rpcContext) {
		this.rpcContext = requireNonNull(rpcContext, "rpcContext");
		evictTimer = new Timer(String.format("RpcServiceExecutor[%s].evictTimer", rpcContext.getLocalHostId()), true);
		evictTimerTask = new TimerTask() {
			@Override
			public void run() {
				try {
					evict();
				} catch (Throwable x) {
					logger.error("evictTimerTask.run: " + x + ' ', x);
				}
			}
		};
		evictTimer.schedule(evictTimerTask, EVICT_PERIOD, EVICT_PERIOD);
	}

	public void putRequest(final Request<?> request) {
		requireNonNull(request, "request");
		final HostId serverHostId = requireNonNull(request.getServerHostId(), "request.serverHostId");
		final Uid requestId = requireNonNull(request.getRequestId(), "request.requestId");
		synchronized (mutex) {
			Map<Uid, Request<?>> requestId2Request = serverHostId2RequestId2Request.get(serverHostId);
			if (requestId2Request == null) {
				requestId2Request = new HashMap<>();
				serverHostId2RequestId2Request.put(serverHostId, requestId2Request);
			}
			Request<?> old = requestId2Request.put(requestId, request);
			if (old != null && old != request)
				throw new IllegalArgumentException("There was already another request with the same requestId! WTF?! requestId=" + requestId);

			requestId2EvictDescriptor.put(requestId, new EvictDescriptor(request));

			mutex.notifyAll();
		}
	}

	public List<Request<?>> pollRequests(final HostId serverHostId, final long timeout) {
		requireNonNull(serverHostId, "serverHostId");
		if (timeout < 0)
			throw new IllegalArgumentException("timeout < 0");

		final long startTimestamp = System.currentTimeMillis();
		synchronized (mutex) {
			while (true) {
				Map<Uid, Request<?>> requestId2Request = serverHostId2RequestId2Request.remove(serverHostId);
				if (requestId2Request != null && ! requestId2Request.isEmpty()) {
					List<Request<?>> result = new ArrayList<>(requestId2Request.values());
					requestId2Request.keySet().forEach(requestId -> requestId2EvictDescriptor.remove(requestId));
					return Collections.unmodifiableList(result);
				}

				final long elapsedTime = System.currentTimeMillis() - startTimestamp;
				if (elapsedTime > timeout)
					return Collections.emptyList();

				long remainingTime = timeout - elapsedTime;
				if (remainingTime > 0) {
					try {
						mutex.wait(remainingTime);
					} catch (InterruptedException e) {
						logger.info("pollRequests: " + e, e);
						return Collections.emptyList();
					}
				}
			}
		}
	}

	protected void evict() {
		final long now = System.currentTimeMillis();
		synchronized (mutex) {
			for (Iterator<EvictDescriptor> it = requestId2EvictDescriptor.values().iterator(); it.hasNext(); ) {
				EvictDescriptor evictDescriptor = it.next();
				if (evictDescriptor.timeoutElapsed < now) {
					Map<Uid, Request<?>> requestId2Request = serverHostId2RequestId2Request.get(evictDescriptor.serverHostId);
					if (requestId2Request != null) {
						requestId2Request.remove(evictDescriptor.requestId);

						if (requestId2Request.isEmpty())
							serverHostId2RequestId2Request.remove(evictDescriptor.serverHostId);
					}
					it.remove();
				}
			}
		}
	}
}
