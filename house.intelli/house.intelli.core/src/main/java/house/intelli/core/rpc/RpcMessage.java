package house.intelli.core.rpc;

import javax.xml.bind.annotation.XmlRootElement;

import house.intelli.core.Uid;

@XmlRootElement
public abstract class RpcMessage {

	/**
	 * Constant identifying the OpenHAB server being the center of the network.
	 * <p>
	 * Used as {@link #getClient() client} or {@link #getServer() server}.
	 */
	public static final String CENTER = "center";

	private Uid requestId;

	private String client;

	private String server;

	/**
	 * Gets a unique identifier for the request and its corresponding response.
	 * @return a unique identifier for the request and its corresponding response. Must not be <code>null</code>
	 * for a request to be processed.
	 */
	public Uid getRequestId() {
		return requestId;
	}
	public void setRequestId(Uid requestId) {
		this.requestId = requestId;
	}

	/**
	 * Gets the client who sent this request.
	 * @return the client who sent this request. Must not be <code>null</code> for a request to be processed.
	 */
	public String getClient() {
		return client;
	}
	public void setClient(String client) {
		this.client = client;
	}

	/**
	 * Gets the server who is supposed to process this request and then send a response.
	 * @return the server who is supposed to process this request and then send a response. Must not be <code>null</code>
	 * for a request to be processed.
	 */
	public String getServer() {
		return server;
	}
	public void setServer(String server) {
		this.server = server;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[requestId=" + requestId + ", client=" + client + ", server=" + server + ']';
	}
}
