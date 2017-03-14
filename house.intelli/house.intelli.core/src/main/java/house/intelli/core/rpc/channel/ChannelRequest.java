package house.intelli.core.rpc.channel;

import house.intelli.core.rpc.Request;

public abstract class ChannelRequest extends Request {

	private String channelId;

	public String getChannelId() {
		return channelId;
	}
	public void setChannelId(String channelId) {
		this.channelId = channelId;
	}

	@Override
	public void copyRequestCoordinates(Request request) {
		super.copyRequestCoordinates(request);
		ChannelRequest cr = (ChannelRequest) request;
		this.setChannelId(cr.getChannelId());
	}
}
