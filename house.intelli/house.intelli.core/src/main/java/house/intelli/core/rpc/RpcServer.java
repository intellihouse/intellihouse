package house.intelli.core.rpc;

import static house.intelli.core.rpc.RpcConst.*;
import static house.intelli.core.util.AssertUtil.*;

import house.intelli.core.Uid;

public class RpcServer implements AutoCloseable {

	private final RpcContext rpcContext;

	protected RpcServer(final RpcContext rpcContext) {
		this.rpcContext = assertNotNull(rpcContext, "rpcContext");
	}

	public void receiveAndProcessRequest(final RpcServerTransport rpcServerTransport) throws RpcException {
		assertNotNull(rpcServerTransport, "rpcServerTransport");
		if (rpcServerTransport.getRpcContext() != this.rpcContext)
			throw new IllegalArgumentException("rpcServerTransport.rpcContext != this.rpcContext");

		try {
			Request request = null;
			Response response = null;
			try {
				request = rpcServerTransport.receiveRequest();
				response = process(request);
			} catch (Exception x) {
				Error error = RemoteExceptionUtil.createError(x);
				response = new ErrorResponse(error);

				if (request != null)
					response.copyRequestCoordinates(request);
			}
			assertNotNull(response, "response");

			rpcServerTransport.sendResponse(response);
		} catch (RpcException x) {
			throw x;
		} catch (RuntimeException x) {
			throw x;
		} catch (Exception x) {
			throw new RpcException(x);
		}
	}

	protected Response process(final Request request) {
		assertNotNull(request, "request");
		if (request.getTimeout() == Request.TIMEOUT_UNDEFINED || request.getTimeout() < 0)
			request.setTimeout(RpcConst.DEFAULT_REQUEST_TIMEOUT);

		final Uid requestId = assertNotNull(request.getRequestId(), "request.requestId");

		final RpcServiceExecutor rpcServiceExecutor = rpcContext.getRpcServiceExecutor();
		if (! (request instanceof DeferredResponseRequest)) // not putting this! we're fetching a response for an old request.
			rpcServiceExecutor.putRequest(request);

		long timeout = Math.min(LOW_LEVEL_TIMEOUT, request.getTimeout());
		Response response = rpcServiceExecutor.pollResponse(requestId, timeout);
		if (response == null) {
			response = new DeferringResponse();
			response.copyRequestCoordinates(request); // warning! this might be a DeferredResponseRequest -- not the original request! but currently, this does not matter as the data copied is the same.
		}

		return response;
	}

	@Override
	public void close() {
	}
}
