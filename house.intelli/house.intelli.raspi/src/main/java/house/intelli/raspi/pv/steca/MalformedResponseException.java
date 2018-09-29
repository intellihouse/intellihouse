package house.intelli.raspi.pv.steca;

import java.io.IOException;

public class MalformedResponseException extends IOException {

	private static final long serialVersionUID = 1L;

	public MalformedResponseException() {
	}

	public MalformedResponseException(String message) {
		super(message);
	}

	public MalformedResponseException(Throwable cause) {
		super(cause);
	}

	public MalformedResponseException(String message, Throwable cause) {
		super(message, cause);
	}
}
