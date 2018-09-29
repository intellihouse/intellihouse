package house.intelli.core.rpc;

import static java.util.Objects.*;

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
		final RpcContext rpcContext = requireNonNull(getRpcContext(), "rpcContext");
		RpcClientTransport rpcClientTransport = _createRpcClientTransport();
		rpcClientTransport.setRpcContext(rpcContext);
		return rpcClientTransport;
	}

	protected abstract RpcClientTransport _createRpcClientTransport();

	@Override
	public RpcClientTransportProvider clone() {
		RpcClientTransportProvider clone;
		try {
			clone = (RpcClientTransportProvider) super.clone();
		} catch (CloneNotSupportedException e) { // should really never happen!
			throw new RuntimeException(e);
		}
		return clone;
	}
}
