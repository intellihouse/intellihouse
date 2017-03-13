package house.intelli.core.rpc;

public abstract class AbstractRpcServerTransport implements RpcServerTransport {

	private RpcContext rpcContext;

	@Override
	public RpcContext getRpcContext() {
		return rpcContext;
	}

	@Override
	public void setRpcContext(RpcContext rpcContext) {
		this.rpcContext = rpcContext;
	}

	@Override
	public void close() {
	}
}
