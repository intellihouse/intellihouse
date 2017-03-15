package house.intelli.core.rpc.dimmer;

import javax.xml.bind.annotation.XmlRootElement;

import house.intelli.core.rpc.VoidResponse;
import house.intelli.core.rpc.channel.ChannelRequest;

@XmlRootElement
public class DimmerActorEventRequest extends ChannelRequest<VoidResponse> {
	private int dimmerValue;

	public int getDimmerValue() {
		return dimmerValue;
	}
	public void setDimmerValue(int dimmerValue) {
		this.dimmerValue = dimmerValue;
	}
}
