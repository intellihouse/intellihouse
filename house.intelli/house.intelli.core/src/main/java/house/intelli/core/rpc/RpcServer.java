package house.intelli.core.rpc;

import static house.intelli.core.rpc.RpcConst.*;
import static house.intelli.core.util.AssertUtil.*;

import java.util.List;

import house.intelli.core.Uid;

/**
 * Server object to process invocation requests on the server-side.
 * <p>
 * Instances of this class are <b>not thread-safe!</b>
 * @author mn
 */
public class RpcServer implements AutoCloseable {

	private final RpcContext rpcContext;

	private Request<?> request;
	private Response response;

	protected RpcServer(final RpcContext rpcContext) {
		this.rpcContext = assertNotNull(rpcContext, "rpcContext");
	}

	public void receiveAndProcessRequest(final RpcServerTransport rpcServerTransport) throws RpcException {
		assertNotNull(rpcServerTransport, "rpcServerTransport");
		if (rpcServerTransport.getRpcContext() != this.rpcContext)
			throw new IllegalArgumentException("rpcServerTransport.rpcContext != this.rpcContext");

		try {
			request = null;
			response = null;
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

	protected Response process(final Request<?> request) {
		assertNotNull(request, "request");
		if (request.getTimeout() == Request.TIMEOUT_UNDEFINED || request.getTimeout() < 0)
			request.setTimeout(RpcConst.DEFAULT_REQUEST_TIMEOUT);

		final Uid requestId = assertNotNull(request.getRequestId(), "request.requestId");
		final long timeout = Math.min(LOW_LEVEL_TIMEOUT, request.getTimeout());
		final RpcServiceExecutor rpcServiceExecutor = rpcContext.getRpcServiceExecutor();

		if (request instanceof PollInverseRequestsRequest) {
			assertServerLocal(request);
			final List<Request<?>> requests = rpcContext.getInverseRequestRegistry().pollRequests(request.getClientHostId(), timeout);
			PollInverseRequestsResponse response = new PollInverseRequestsResponse();
			response.setInverseRequests(requests);
			response.copyRequestCoordinates(request);
			return response;
		}

		if (request instanceof PutInverseResponseRequest) {
			assertServerLocal(request);
			PutInverseResponseRequest pirRequest = (PutInverseResponseRequest) request;
			Response inverseResponse = assertNotNull(pirRequest.getInverseResponse(), "putInverseResponseRequest.inverseResponse");
			rpcServiceExecutor.putResponse(inverseResponse);
			Response response = new NullResponse();
			response.copyRequestCoordinates(request);
			return response;
		}

		if (request instanceof DeferredResponseRequest)
			assertServerLocal(request); // not putting this request! we're fetching a response for an old request.
		else
			rpcServiceExecutor.putRequest(request);

		Response response = rpcServiceExecutor.pollResponse(requestId, timeout);
		if (response == null) {
			response = new DeferringResponse();
			response.copyRequestCoordinates(request); // warning! this might be a DeferredResponseRequest -- not the original request! but currently, this does not matter as the data copied is the same.
		}

		return response;
	}

	/**
	 * Gets the last request after an invocation of {@link #receiveAndProcessRequest(RpcServerTransport)}.
	 * @return the last request or <code>null</code>.
	 */
	public Request getRequest() {
		return request;
	}
	/**
	 * Gets the last response after an invocation of {@link #receiveAndProcessRequest(RpcServerTransport)}.
	 * @return the last response or <code>null</code>.
	 */
	public Response getResponse() {
		return response;
	}

	@Override
	public void close() {
	}

	protected void assertServerLocal(final Request request) {
		assertNotNull(request, "request");
		if (! rpcContext.isServerLocal(request))
			throw new UnsupportedOperationException("This request's type is only supported for local processing, but its serverHostId references another host!" + request);
	}
}
