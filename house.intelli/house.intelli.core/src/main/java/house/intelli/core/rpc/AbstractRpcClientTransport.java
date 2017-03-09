package house.intelli.core.rpc;

public abstract class AbstractRpcClientTransport implements RpcClientTransport {

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
	public void close() throws Exception {
	}
}
