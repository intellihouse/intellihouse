package house.intelli.core.rpc;

import java.util.Date;

public abstract class Request extends RpcMessage {

	public static final long TIMEOUT_UNDEFINED = 0;

	private Date created;

	private long timeout = TIMEOUT_UNDEFINED;

	public Date getCreated() {
		return created;
	}
	public void setCreated(Date created) {
		this.created = created;
	}

	/**
	 * Gets the timeout in milliseconds this request is allowed to take. The corresponding {@link Response} is
	 * expected to arrive within this time. If not specified ({@link #TIMEOUT_UNDEFINED}), the
	 * {@link RpcConst#DEFAULT_REQUEST_TIMEOUT DEFAULT_REQUEST_TIMEOUT} is used.
	 * @return the timeout in milliseconds this request is allowed to take.
	 */
	public long getTimeout() {
		return timeout;
	}
	public void setTimeout(long timeout) {
		if (timeout < 0)
			throw new IllegalArgumentException("timeout < 0");

		this.timeout = timeout;
	}

	@Override
	protected String toString_getProperties() {
		return super.toString_getProperties() + ", created=" + created + ", timeout=" + timeout;
	}
}
