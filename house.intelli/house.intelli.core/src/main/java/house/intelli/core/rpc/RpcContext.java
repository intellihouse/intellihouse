package house.intelli.core.rpc;

import static house.intelli.core.util.AssertUtil.*;

public class RpcContext {
	private final RpcContextMode mode;

	private final HostId localHostId;

	private RpcClientTransportProvider rpcClientTransportProvider;

	private RpcServiceExecutor rpcServiceExecutor;

//	private RpcServiceRegistry rpcServiceRegistry;

	private InverseRequestRegistry inverseRequestRegistry;

	public RpcContext(final RpcContextMode mode, HostId localHostId) {
		this.mode = assertNotNull(mode, "mode");
		this.localHostId = assertNotNull(localHostId, "localHostId");
	}

	public RpcContext(final RpcContextMode mode) {
		this(mode, HostId.getLocalHostId());
	}

	public RpcContextMode getMode() {
		return mode;
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
		if (RpcContextMode.CLIENT != mode)
			throw new IllegalStateException("RpcContextMode.CLIENT != mode == " + mode);

		return new RpcClient(this);
	}

	public RpcServer createRpcServer() {
		if (RpcContextMode.SERVER != mode)
			throw new IllegalStateException("RpcContextMode.SERVER != mode == " + mode);

		return new RpcServer(this);
	}

	public synchronized RpcServiceExecutor getRpcServiceExecutor() {
		if (rpcServiceExecutor == null)
			rpcServiceExecutor = new RpcServiceExecutor(this);

		return rpcServiceExecutor;
	}

//	public synchronized RpcServiceRegistry getRpcServiceRegistry() {
//		if (rpcServiceRegistry == null)
//			rpcServiceRegistry = new RpcServiceRegistry(this);
//
//		return rpcServiceRegistry;
//	}

	public synchronized InverseRequestRegistry getInverseRequestRegistry() {
		if (inverseRequestRegistry == null)
			inverseRequestRegistry = new InverseRequestRegistry(this);

		return inverseRequestRegistry;
	}
}
