package house.intelli.core.rpc.dimmer;

import javax.xml.bind.annotation.XmlRootElement;

import house.intelli.core.rpc.channel.ChannelRequest;

@XmlRootElement
public class DimmerActorWriteRequest extends ChannelRequest<DimmerActorWriteResponse> {
	private int dimmerValue;

	public int getDimmerValue() {
		return dimmerValue;
	}
	public void setDimmerValue(int dimmerValue) {
		this.dimmerValue = dimmerValue;
	}
}
