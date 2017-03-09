package house.intelli.core.rpc;

import java.net.Socket;

public interface RpcConst {

	/**
	 * How long (milliseconds) can the {@link RpcServer} make the {@link RpcClient} wait for a low-level-invocation.
	 * <p>
	 * This must be shorter than the transport's timeout (e.g. {@link Socket#getSoTimeout()})!
	 * <p>
	 * If the real (high-level) invocation did not complete, yet, an intermediate response is sent to the
	 * client, telling the client to retry.
	 */
	int LOW_LEVEL_TIMEOUT = 30 * 1000;

	/**
	 * How long (milliseconds) is a {@link Request} processing allowed to take by default.
	 * {@link Request#getTimeout()} overrides this default.
	 * <p>
	 * If the client does not receive a {@link Response} within this time, an {@link RpcTimeoutException} is thrown.
	 */
	int DEFAULT_REQUEST_TIMEOUT = 10 * 60 * 1000;

	int TRANSPORT_CONNECT_TIMEOUT = 15 * 1000;

	int TRANSPORT_READ_TIMEOUT = LOW_LEVEL_TIMEOUT + (20 * 1000);

}
