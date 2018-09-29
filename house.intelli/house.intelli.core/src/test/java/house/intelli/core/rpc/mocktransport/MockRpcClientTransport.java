package house.intelli.core.rpc.mocktransport;

import static java.util.Objects.*;

import java.io.IOException;

import house.intelli.core.rpc.AbstractRpcClientTransport;
import house.intelli.core.rpc.Request;
import house.intelli.core.rpc.Response;
import house.intelli.core.rpc.RpcContext;

public class MockRpcClientTransport extends AbstractRpcClientTransport {

	private RpcContext serverRpcContext;

	private MockRpcServerTransport rpcServerTransport;

	@Override
	public void sendRequest(Request request) throws IOException {
		getRpcServerTransport().putRequest(request);
	}

	@Override
	public Response receiveResponse() throws IOException {
		return getRpcServerTransport().fetchResponse();
	}

	public MockRpcServerTransport getRpcServerTransport() {
		if (rpcServerTransport == null) {
			RpcContext src = requireNonNull(serverRpcContext, "serverRpcContext");
			rpcServerTransport = new MockRpcServerTransport();
			rpcServerTransport.setRpcContext(src);
		}
		return rpcServerTransport;
	}

	public RpcContext getServerRpcContext() {
		return serverRpcContext;
	}
	public void setServerRpcContext(RpcContext serverRpcContext) {
		this.serverRpcContext = serverRpcContext;
	}
}
