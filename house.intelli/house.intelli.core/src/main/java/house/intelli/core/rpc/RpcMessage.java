package house.intelli.core.rpc;

import static house.intelli.core.util.AssertUtil.*;

import javax.xml.bind.annotation.XmlRootElement;

import house.intelli.core.Uid;

@XmlRootElement
public abstract class RpcMessage {

	private Uid requestId;

	private HostId clientHostId;

	private HostId serverHostId;

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
	 * Gets the host-id of the client who sent this request.
	 * <p>
	 * Important: This is totally independent from the physical transport layer. Thus an HTTP-client may
	 * very well act as server. The high-level-protocol is entirely bidirectional and every host may
	 * act as client invoking a {@link RpcService} anywhere on every host in the network.
	 * <p>
	 * It is also possible that client and server are the same.
	 * @return the host-id of the client who sent this request.
	 * Must not be <code>null</code> for a request to be processed.
	 */
	public HostId getClientHostId() {
		return clientHostId;
	}
	public void setClientHostId(HostId channelId) {
		this.clientHostId = channelId;
	}

	/**
	 * Gets the host-id of the server who is supposed to process this request and then send a response.
	 * <p>
	 * Important: This is totally independent from the physical transport layer. Thus an HTTP-client may
	 * very well act as server. The high-level-protocol is entirely bidirectional and every host may
	 * act as client invoking a {@link RpcService} anywhere on every host in the network.
	 * <p>
	 * It is also possible that client and server are the same.
	 * @return the host-id of the server who is supposed to process this request and then send a response.
	 * Must not be <code>null</code> for a request to be processed.
	 */
	public HostId getServerHostId() {
		return serverHostId;
	}
	public void setServerHostId(HostId channelId) {
		this.serverHostId = channelId;
	}

	public void copyRequestCoordinates(Request request) {
		assertNotNull(request, "request");
		this.setRequestId(request.getRequestId());
		this.setClientHostId(request.getClientHostId());
		this.setServerHostId(request.getServerHostId());
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + '[' + toString_getProperties() + ']';
	}

	protected String toString_getProperties() {
		return "requestId=" + requestId + ", clientHostId=" + clientHostId + ", serverHostId=" + serverHostId;
	}
}
