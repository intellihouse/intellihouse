package house.intelli.core.rpc;

import static house.intelli.core.util.AssertUtil.*;

public class RpcContext implements AutoCloseable {
	private final RpcContextMode mode;

	private final HostId localHostId;

	private RpcClientTransportProvider rpcClientTransportProvider;

	private RpcServiceExecutor rpcServiceExecutor;

	private InverseRequestRegistry inverseRequestRegistry;

	private final PollInverseRequestsThread pollInverseRequestsThread;

	public RpcContext(final RpcContextMode mode, HostId localHostId) {
		this.mode = assertNotNull(mode, "mode");
		this.localHostId = assertNotNull(localHostId, "localHostId");

		if (RpcContextMode.CLIENT == mode) {
			pollInverseRequestsThread = new PollInverseRequestsThread(this);
			pollInverseRequestsThread.start();
		}
		else
			pollInverseRequestsThread = null;
	}

	private volatile boolean closed;

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
		assertNotClosed();
		return rpcClientTransportProvider;
	}
	public void setRpcClientTransportProvider(RpcClientTransportProvider rpcClientTransportProvider) {
		assertNotClosed();
		if (rpcClientTransportProvider != null) {
			if (rpcClientTransportProvider.getRpcContext() != null && rpcClientTransportProvider.getRpcContext() != this)
				throw new IllegalArgumentException("rpcClientTransportProvider is already bound to different RpcContext!");

			rpcClientTransportProvider.setRpcContext(this);
		}
		this.rpcClientTransportProvider = rpcClientTransportProvider;
	}

	public RpcClient createRpcClient() {
		assertNotClosed();
		return new RpcClient(this);
	}

	public RpcServer createRpcServer() {
		assertNotClosed();
		if (RpcContextMode.SERVER != mode)
			throw new IllegalStateException("RpcContextMode.SERVER != mode == " + mode);

		return new RpcServer(this);
	}

	public synchronized RpcServiceExecutor getRpcServiceExecutor() {
		assertNotClosed();
		if (rpcServiceExecutor == null)
			rpcServiceExecutor = new RpcServiceExecutor(this);

		return rpcServiceExecutor;
	}

	public synchronized InverseRequestRegistry getInverseRequestRegistry() {
		assertNotClosed();
		if (inverseRequestRegistry == null)
			inverseRequestRegistry = new InverseRequestRegistry(this);

		return inverseRequestRegistry;
	}

	public boolean isServerLocal(final Request request) {
		assertNotNull(request, "request");
		assertNotClosed();
		final HostId serverHostId = assertNotNull(request.getServerHostId(), "request.serverHostId");
		if (localHostId.equals(serverHostId))
			return true;

		if (RpcContextMode.SERVER == mode && HostId.SERVER.equals(serverHostId))
			return true;

		return false;
	}

	protected void assertNotClosed() {
		if (closed)
			throw new IllegalStateException("This RpcContext instance is already closed!");
	}

	@Override
	public void close() {
		if (pollInverseRequestsThread != null)
			pollInverseRequestsThread.interrupt();

		closed = true;
	}
}
