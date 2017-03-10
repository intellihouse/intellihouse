package house.intelli.core.rpc.mocktransport;

import static house.intelli.core.util.AssertUtil.*;

import java.io.IOException;

import house.intelli.core.rpc.AbstractRpcServerTransport;
import house.intelli.core.rpc.Request;
import house.intelli.core.rpc.Response;
import house.intelli.core.rpc.RpcServer;

public class MockRpcServerTransport extends AbstractRpcServerTransport {

	private Request request;
	private Response response;

	public void putRequest(Request request) throws IOException {
		this.request = assertNotNull(request, "request");
		RpcServer rpcServer = getRpcContext().createRpcServer();
		rpcServer.receiveAndProcessRequest(this);
	}

	public Response fetchResponse() throws IOException {
		Response r = assertNotNull(response, "response");
		response = null;
		return r;
	}

	@Override
	public Request receiveRequest() throws IOException {
		Request r = assertNotNull(request, "request");
		request = null;
		return r;
	}

	@Override
	public void sendResponse(Response response) throws IOException {
		this.response = response;
	}

}
