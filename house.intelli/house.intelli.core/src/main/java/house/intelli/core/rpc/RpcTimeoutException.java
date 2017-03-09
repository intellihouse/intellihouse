package house.intelli.core.rpc;

@SuppressWarnings("serial")
public class RpcTimeoutException extends RpcException {

	public RpcTimeoutException() {
	}

	public RpcTimeoutException(String message) {
		super(message);
	}

	public RpcTimeoutException(Throwable cause) {
		super(cause);
	}

	public RpcTimeoutException(String message, Throwable cause) {
		super(message, cause);
	}

}
