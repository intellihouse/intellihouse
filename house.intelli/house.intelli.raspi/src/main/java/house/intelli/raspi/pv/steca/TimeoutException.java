package house.intelli.raspi.pv.steca;

import java.io.IOException;

public class TimeoutException extends IOException {

	private static final long serialVersionUID = 1L;

	public TimeoutException() {
	}

	public TimeoutException(String message) {
		super(message);
	}

	public TimeoutException(Throwable cause) {
		super(cause);
	}

	public TimeoutException(String message, Throwable cause) {
		super(message, cause);
	}

}
