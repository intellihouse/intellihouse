package house.intelli.core.rpc;

import static house.intelli.core.util.AssertUtil.*;

public class RpcContext {

	private final HostId localHostId;

	private RpcClientTransportProvider rpcClientTransportProvider;

	private RpcServiceExecutor rpcServiceExecutor;

	private RpcServiceRegistry rpcServiceRegistry;

	public RpcContext(HostId localHostId) {
		this.localHostId = assertNotNull(localHostId, "localHostId");
	}

	public RpcContext() {
		this(HostId.getLocalHostId());
	}

	public HostId getLocalHostId() {
		return localHostId;
	}

	public RpcClientTransportProvider getRpcClientTransportProvider() {
		return rpcClientTransportProvider;
	}
	public void setRpcClientTransportProvider(RpcClientTransportProvider rpcClientTransportProvider) {
		if (rpcClientTransportProvider != null) {
			if (rpcClientTransportProvider.getRpcContext() != null && rpcClientTransportProvider.getRpcContext() != this)
				throw new IllegalArgumentException("rpcClientTransportProvider is already bound to different RpcContext!");

			rpcClientTransportProvider.setRpcContext(this);
		}
		this.rpcClientTransportProvider = rpcClientTransportProvider;
	}

	public RpcClient createRpcClient() {
		return new RpcClient(this);
	}

	public RpcServer createRpcServer() {
		return new RpcServer(this);
	}

	public synchronized RpcServiceExecutor getRpcServiceExecutor() {
		if (rpcServiceExecutor == null)
			rpcServiceExecutor = new RpcServiceExecutor(this);

		return rpcServiceExecutor;
	}

	public synchronized RpcServiceRegistry getRpcServiceRegistry() {
		if (rpcServiceRegistry == null)
			rpcServiceRegistry = new RpcServiceRegistry(this);

		return rpcServiceRegistry;
	}
}
