package house.intelli.core.rpc.channel;

import house.intelli.core.rpc.Response;
import house.intelli.core.rpc.RpcMessage;

public abstract class ChannelResponse extends Response {

	private String channelId;

	public String getChannelId() {
		return channelId;
	}
	public void setChannelId(String channelId) {
		this.channelId = channelId;
	}

	@Override
	public void copyRequestCoordinates(RpcMessage message) {
		super.copyRequestCoordinates(message);

		if (message instanceof ChannelRequest<?>)
			this.setChannelId(((ChannelRequest<?>) message).getChannelId());
		else if (message instanceof ChannelResponse)
			this.setChannelId(((ChannelResponse) message).getChannelId());
	}

	@Override
	protected String toString_getProperties() {
		return super.toString_getProperties() + ", channelId=" + channelId;
	}
}
