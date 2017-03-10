package house.intelli.core.rpc;

import static house.intelli.core.util.AssertUtil.*;

public abstract class AbstractRpcClientTransportProvider implements RpcClientTransportProvider {

	private RpcContext rpcContext;

	@Override
	public RpcContext getRpcContext() {
		return rpcContext;
	}

	@Override
	public void setRpcContext(RpcContext rpcContext) {
		if (this.rpcContext != null && this.rpcContext != rpcContext)
			throw new IllegalStateException("rpcContext already assigned!");

		this.rpcContext = rpcContext;
	}

	@Override
	public final RpcClientTransport createRpcClientTransport() {
		final RpcContext rpcContext = assertNotNull(getRpcContext(), "rpcContext");
		RpcClientTransport rpcClientTransport = _createRpcClientTransport();
		rpcClientTransport.setRpcContext(rpcContext);
		return rpcClientTransport;
	}

	protected abstract RpcClientTransport _createRpcClientTransport();

}
