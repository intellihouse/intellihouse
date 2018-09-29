package house.intelli.raspi.pv.steca;

import java.io.IOException;

public class CrcException extends IOException {

	private static final long serialVersionUID = 1L;

	public CrcException() {
	}

	public CrcException(String message) {
		super(message);
	}

	public CrcException(Throwable cause) {
		super(cause);
	}

	public CrcException(String message, Throwable cause) {
		super(message, cause);
	}
}
