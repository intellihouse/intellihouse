package house.intelli.core.rpc.mocktransport;

import static java.util.Objects.*;

import java.io.IOException;

import house.intelli.core.rpc.AbstractRpcServerTransport;
import house.intelli.core.rpc.Request;
import house.intelli.core.rpc.Response;
import house.intelli.core.rpc.RpcServer;

public class MockRpcServerTransport extends AbstractRpcServerTransport {

	private Request request;
	private Response response;

	public void putRequest(Request request) throws IOException {
		this.request = requireNonNull(request, "request");
		RpcServer rpcServer = getRpcContext().createRpcServer();
		rpcServer.receiveAndProcessRequest(this);
	}

	public Response fetchResponse() throws IOException {
		Response r = requireNonNull(response, "response");
		response = null;
		return r;
	}

	@Override
	public Request receiveRequest() throws IOException {
		Request r = requireNonNull(request, "request");
		request = null;
		return r;
	}

	@Override
	public void sendResponse(Response response) throws IOException {
		this.response = response;
	}

}
