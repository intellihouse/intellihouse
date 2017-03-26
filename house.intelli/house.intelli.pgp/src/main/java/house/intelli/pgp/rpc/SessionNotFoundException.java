package house.intelli.pgp.rpc;

import house.intelli.core.rpc.RetriableError;

@RetriableError
public class SessionNotFoundException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public SessionNotFoundException() {
	}

	public SessionNotFoundException(String message) {
		super(message);
	}

	public SessionNotFoundException(Throwable cause) {
		super(cause);
	}

	public SessionNotFoundException(String message, Throwable cause) {
		super(message, cause);
	}

}
