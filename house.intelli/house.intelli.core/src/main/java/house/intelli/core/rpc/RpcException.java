package house.intelli.core.rpc;

import java.io.IOException;

@SuppressWarnings("serial")
public class RpcException extends IOException {

	public RpcException() {
	}

	public RpcException(String message) {
		super(message);
	}

	public RpcException(Throwable cause) {
		super(cause);
	}

	public RpcException(String message, Throwable cause) {
		super(message, cause);
	}

}
