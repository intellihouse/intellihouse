package house.intelli.core.rpc;

import static house.intelli.core.util.AssertUtil.*;
import static house.intelli.core.util.Util.*;

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import house.intelli.core.Uid;

/**
 * Client to invoke {@link RpcService}s.
 * <p>
 * Instances of this class are <b>not thread-safe!</b>
 * @author mn
 */
public class RpcClient implements AutoCloseable {
	private static final Logger logger = LoggerFactory.getLogger(RpcClient.class);

	private final RpcContext rpcContext;
	private final RpcClientTransportProvider rpcClientTransportProvider;
	private RpcClientTransport rpcClientTransport;

	protected RpcClient(final RpcContext rpcContext) {
		this.rpcContext = assertNotNull(rpcContext, "rpcContext");
		if (RpcContextMode.CLIENT == rpcContext.getMode())
			this.rpcClientTransportProvider = assertNotNull(this.rpcContext.getRpcClientTransportProvider(), "rpcContext.rpcClientTransportProvider");
		else
			this.rpcClientTransportProvider = null;
	}

	public <REQ extends Request<RES>, RES extends Response> RES invoke(final REQ request) throws RpcException {
		assertNotNull(request, "request");
		int retryCount = 0;
		final int maxRetryCount = 3;
		while (true) {
			try {
				RES response = _invoke(request);
				return response;
			} catch (Throwable x) {
				logger.error("invoke: " + x + ' ', x);

				if (! isRetriableError(x) && ! isRetriableRequest(request))
					throw x;

				if (++retryCount > maxRetryCount)
					throw x;

				logger.info("invoke: RETRYING! retryCount={}", retryCount);
				try {
					Thread.sleep(1000L);
				} catch (InterruptedException e) {
					doNothing();
				}
			}
		}
	}

	private boolean isRetriableRequest(Request<?> request) {
		return request.isIdempotent();
	}

	private boolean isRetriableError(final Throwable x) {
		Throwable t = x;
		while (t != null) {
			RetriableError annotation = t.getClass().getAnnotation(RetriableError.class);
			if (annotation != null) {
				return annotation.value();
			}
			t = t.getCause();
		}
		return false;
	}

	protected <REQ extends Request<RES>, RES extends Response> RES _invoke(final REQ request) throws RpcException {
		assertNotNull(request, "request");
		prepareRequest(request);

		final long timeoutTimestamp = System.currentTimeMillis() + request.getTimeout();
		DeferredResponseRequest deferredResponseRequest = null;
		try {
			while (true) {
				Request<?> req = deferredResponseRequest != null ? deferredResponseRequest : request;
				logger.debug("invoke: Sending request: {}", req);

				Response response;
				if (RpcContextMode.CLIENT == rpcContext.getMode()) {
					final RpcClientTransport rpcClientTransport = getRpcClientTransport();
					rpcClientTransport.sendRequest(req);
					response = rpcClientTransport.receiveResponse();
				}
				else {
					final RpcServiceExecutor rpcServiceExecutor = rpcContext.getRpcServiceExecutor();
					rpcServiceExecutor.putRequest(request);
					response = rpcServiceExecutor.pollResponse(request.getRequestId(), request.getTimeout());
					if (response == null)
						throw new RpcTimeoutException(String.format("Inverse request timed out: %s", request));
				}

				logger.debug("invoke: Received response: {}", response);

				if (response instanceof DeferringResponse) {
					if (System.currentTimeMillis() > timeoutTimestamp)
						throw new RpcTimeoutException(String.format("Request timed out: %s", request));

					deferredResponseRequest = new DeferredResponseRequest();
					deferredResponseRequest.copyRequestCoordinates(request);
					deferredResponseRequest.setCreated(new Date());
					deferredResponseRequest.setTimeout(Math.max(1, timeoutTimestamp - System.currentTimeMillis()));
					continue;
				}

				if (response instanceof ErrorResponse) {
					ErrorResponse errorResponse = (ErrorResponse) response;
					Error error = assertNotNull(errorResponse.getError(), "errorResponse.error");
					RemoteExceptionUtil.throwOriginalExceptionIfPossible(error);
					throw new RemoteException(error);
				}

				if (response instanceof NullResponse) {
					return null;
				}

				@SuppressWarnings("unchecked")
				RES res = (RES) response;
				return res;
			}
		} catch (RpcException x) {
			throw x;
		} catch (RuntimeException x) {
			throw x;
		} catch (Exception x) {
			throw new RpcException(x);
		}
	}

	protected void prepareRequest(Request<?> request) {
		assertNotNull(request, "request");
		assertNotNull(request.getServerHostId(), "request.serverHostId");

		if (request.getClientHostId() == null)
			request.setClientHostId(rpcContext.getLocalHostId());

		if (request.getRequestId() == null)
			request.setRequestId(new Uid());

		if (request.getCreated() == null)
			request.setCreated(new Date());

		if (request.getTimeout() == Request.TIMEOUT_UNDEFINED || request.getTimeout() < 0)
			request.setTimeout(RpcConst.DEFAULT_REQUEST_TIMEOUT);
	}

	public RpcClientTransport getRpcClientTransport() {
		if (rpcClientTransport == null)
			rpcClientTransport = assertNotNull(rpcClientTransportProvider, "rpcClientTransportProvider").createRpcClientTransport();

		return rpcClientTransport;
	}

	@Override
	public void close() {
		final RpcClientTransport rct = rpcClientTransport;
		if (rct != null) {
			rct.close();
			rpcClientTransport = null;
		}
	}
}