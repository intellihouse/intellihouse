package house.intelli.core.rpc;

import static house.intelli.core.util.AssertUtil.*;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import house.intelli.core.Uid;

public class RpcServiceExecutor {

	private static final Logger logger = LoggerFactory.getLogger(RpcServiceExecutor.class);

	private static final long EVICT_PERIOD = 60L * 60L * 1000L;

	private final RpcContext rpcContext;
	private final ExecutorService executorService = Executors.newCachedThreadPool(); // TODO use fixed-size and make size configurable?!

	private final Object mutex = new Object();
	private final Map<Uid, Request> requestId2Request = new HashMap<>();
	private final Map<Uid, Response> requestId2Response = new HashMap<>();
	private final Map<Uid, EvictDescriptor> requestId2EvictDescriptor = new HashMap<>();

	private final Timer evictTimer;
	private final TimerTask evictTimerTask;

	private static class EvictDescriptor {
		public final Uid requestId;
		public final long created = System.currentTimeMillis();
		public final long timeout;
		public final long timeoutElapsed;

		public EvictDescriptor(Request request) {
			assertNotNull(request, "request");
			requestId = assertNotNull(request.getRequestId(), "request.requestId");
			if (request.getTimeout() == Request.TIMEOUT_UNDEFINED)
				timeout = RpcConst.DEFAULT_REQUEST_TIMEOUT;
			else
				timeout = request.getTimeout();

			timeoutElapsed = created + timeout;
		}
	}

	protected RpcServiceExecutor(RpcContext rpcContext) {
		this.rpcContext = assertNotNull(rpcContext, "rpcContext");
		evictTimer = new Timer(String.format("RpcServiceExecutor[%s].evictTimer", rpcContext.getLocalHostId()), true);
		evictTimerTask = new TimerTask() {
			@Override
			public void run() {
				try {
					evict();
				} catch (Throwable x) {
					logger.error("evictTimerTask.run: " + x, x);
				}
			}
		};
		evictTimer.schedule(evictTimerTask, EVICT_PERIOD, EVICT_PERIOD);
	}

	public void putRequest(final Request request) {
		assertNotNull(request, "request");
		final Uid requestId = assertNotNull(request.getRequestId(), "request.requestId");
		synchronized (mutex) {
			if (requestId2Response.containsKey(requestId))
				throw new IllegalArgumentException("There is already a response with the same requestId! WTF?! requestId=" + requestId);

			Request old = requestId2Request.put(requestId, request);
			if (old != null && old != request)
				throw new IllegalArgumentException("There was already another request with the same requestId! WTF?! requestId=" + requestId);

			requestId2EvictDescriptor.put(requestId, new EvictDescriptor(request));

			mutex.notifyAll();
		}

		executorService.submit(new Runnable() {
			@Override
			public void run() {
				try {
					RpcService<Request, Response> rpcService = rpcContext.getRpcServiceRegistry().createRpcService(request.getClass());
					if (rpcService == null)
						throw new IllegalArgumentException("There is no RpcService registered for this requestType: " + request.getClass().getName());

					Response response = rpcService.process(request);
					if (response == null)
						response = new VoidResponse();

					response.copyRequestCoordinates(request);
					putResponse(response);
				} catch (Throwable x) {
					Error error = RemoteExceptionUtil.createError(x);
					ErrorResponse errorResponse = new ErrorResponse(error);
					errorResponse.copyRequestCoordinates(request);
					putResponse(errorResponse);
				}
			}
		});
	}

	public Response pollResponse(final Uid requestId, final long timeout) {
		assertNotNull(requestId, "requestId");
		final long startTimestamp = System.currentTimeMillis();
		synchronized (mutex) {
			while (true) {
				final Response response = requestId2Response.remove(requestId);
				if (response != null) {
					requestId2EvictDescriptor.remove(requestId);
					return response;
				}

				final long elapsedTime = System.currentTimeMillis() - startTimestamp;
				if (elapsedTime > timeout)
					return null;

				long remainingTime = timeout - elapsedTime;
				if (remainingTime > 0) {
					try {
						mutex.wait(remainingTime);
					} catch (InterruptedException e) {
						logger.warn(e.toString(), e);
						return null;
					}
				}
			}
		}
	}

	protected void putResponse(final Response response) {
		assertNotNull(response, "response");
		final Uid requestId = assertNotNull(response.getRequestId(), "response.requestId");

		synchronized (mutex) {
			final Request request = requestId2Request.remove(requestId);
			if (request == null)
				throw new IllegalArgumentException("There is no request waiting with requestId=" + requestId);

			Response old = requestId2Response.put(requestId, response);
			if (old != null && old != response)
				throw new IllegalArgumentException("There was already another response with the same requestId! WTF?! requestId=" + requestId);

			mutex.notifyAll();
		}
	}

	protected void evict() {
		final long now = System.currentTimeMillis();
		synchronized (mutex) {
			for (Iterator<EvictDescriptor> it = requestId2EvictDescriptor.values().iterator(); it.hasNext(); ) {
				EvictDescriptor evictDescriptor = it.next();
				if (evictDescriptor.timeoutElapsed < now) {
					requestId2Request.remove(evictDescriptor.requestId);
					requestId2Response.remove(evictDescriptor.requestId);
					it.remove();
				}
			}
		}
	}
}
