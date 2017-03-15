package house.intelli.core.rpc.relay;

import javax.xml.bind.annotation.XmlRootElement;

import house.intelli.core.rpc.channel.ChannelResponse;

@XmlRootElement
public class RelayActorReadResponse extends ChannelResponse {
	private boolean energized;

	public boolean isEnergized() {
		return energized;
	}
	public void setEnergized(boolean energized) {
		this.energized = energized;
	}
}
