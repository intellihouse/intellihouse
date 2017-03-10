package house.intelli.core.rpc;

import static house.intelli.core.util.AssertUtil.*;

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import house.intelli.core.Uid;

public class RpcClient implements AutoCloseable {
	private static final Logger logger = LoggerFactory.getLogger(RpcClient.class);

	private final RpcContext rpcContext;
	private final RpcClientTransportProvider rpcClientTransportProvider;

	protected RpcClient(final RpcContext rpcContext) {
		this.rpcContext = assertNotNull(rpcContext, "rpcContext");
		this.rpcClientTransportProvider = assertNotNull(this.rpcContext.getRpcClientTransportProvider(), "rpcContext.rpcClientTransportProvider");
	}

	public <REQ extends Request, RES extends Response> RES invoke(REQ request) throws RpcException {
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

		final long timeoutTimestamp = System.currentTimeMillis() + request.getTimeout();
		DeferredResponseRequest deferredResponseRequest = null;
		try {
			try (RpcClientTransport rpcClientTransport = rpcClientTransportProvider.createRpcClientTransport()) {
				while (true) {
					Request req = deferredResponseRequest != null ? deferredResponseRequest : request;
					logger.debug("invoke: Sending request: {}", req);

					rpcClientTransport.sendRequest(req);
					Response response = rpcClientTransport.receiveResponse();

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
			}
		} catch (RpcException x) {
			throw x;
		} catch (RuntimeException x) {
			throw x;
		} catch (Exception x) {
			throw new RpcException(x);
		}
	}

	@Override
	public void close() {
	}
}