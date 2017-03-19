package house.intelli.core.rpc;

import static house.intelli.core.util.AssertUtil.*;

public abstract class AbstractRpcServerTransportProvider implements RpcServerTransportProvider {

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
	public final RpcServerTransport createRpcServerTransport() {
		final RpcContext rpcContext = assertNotNull(getRpcContext(), "rpcContext");
		RpcServerTransport rpcServerTransport = _createRpcServerTransport();
		rpcServerTransport.setRpcContext(rpcContext);
		return rpcServerTransport;
	}

	protected abstract RpcServerTransport _createRpcServerTransport();

	@Override
	public RpcServerTransportProvider clone() {
		RpcServerTransportProvider clone;
		try {
			clone = (RpcServerTransportProvider) super.clone();
		} catch (CloneNotSupportedException e) { // should really never happen!
			throw new RuntimeException(e);
		}
		return clone;
	}
}
