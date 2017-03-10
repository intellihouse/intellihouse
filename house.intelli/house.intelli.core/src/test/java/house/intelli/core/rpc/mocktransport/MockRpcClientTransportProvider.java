package house.intelli.core.rpc.mocktransport;

import house.intelli.core.rpc.AbstractRpcClientTransportProvider;
import house.intelli.core.rpc.RpcClientTransport;
import house.intelli.core.rpc.RpcContext;

public class MockRpcClientTransportProvider extends AbstractRpcClientTransportProvider {

	private RpcContext serverRpcContext;

	@Override
	public RpcClientTransport _createRpcClientTransport() {
		MockRpcClientTransport rpcClientTransport = new MockRpcClientTransport();
		rpcClientTransport.setServerRpcContext(serverRpcContext);
		return rpcClientTransport;
	}

	public RpcContext getServerRpcContext() {
		return serverRpcContext;
	}
	public void setServerRpcContext(RpcContext serverRpcContext) {
		this.serverRpcContext = serverRpcContext;
	}

}
