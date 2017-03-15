package house.intelli.core.rpc.relay;

import javax.xml.bind.annotation.XmlRootElement;

import house.intelli.core.rpc.VoidResponse;
import house.intelli.core.rpc.channel.ChannelRequest;

@XmlRootElement
public class RelayActorEventRequest extends ChannelRequest<VoidResponse> {
	private boolean energized;

	public boolean isEnergized() {
		return energized;
	}
	public void setEnergized(boolean energized) {
		this.energized = energized;
	}
}
