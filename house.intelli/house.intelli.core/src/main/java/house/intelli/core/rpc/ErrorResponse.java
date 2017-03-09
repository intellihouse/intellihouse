package house.intelli.core.rpc;

public class ErrorResponse extends Response {

	private Error error;

	public ErrorResponse() {
	}

	public ErrorResponse(Error error) {
		this.error = error;
	}

	public Error getError() {
		return error;
	}
	public void setError(Error error) {
		this.error = error;
	}

}
