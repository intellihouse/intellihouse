package house.intelli.core.rpc.channel;

import house.intelli.core.rpc.Request;
import house.intelli.core.rpc.Response;

public abstract class ChannelRequest<RES extends Response> extends Request<RES> {

	private String channelId;

	public String getChannelId() {
		return channelId;
	}
	public void setChannelId(String channelId) {
		this.channelId = channelId;
	}

	@Override
	public void copyRequestCoordinates(Request<?> request) {
		super.copyRequestCoordinates(request);
		ChannelRequest<?> cr = (ChannelRequest<?>) request;
		this.setChannelId(cr.getChannelId());
	}
}
