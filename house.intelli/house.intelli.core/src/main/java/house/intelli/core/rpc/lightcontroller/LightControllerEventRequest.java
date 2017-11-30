package house.intelli.core.rpc.lightcontroller;

import javax.xml.bind.annotation.XmlRootElement;

import house.intelli.core.rpc.VoidResponse;
import house.intelli.core.rpc.channel.ChannelRequest;

@XmlRootElement
public class LightControllerEventRequest extends ChannelRequest<VoidResponse> {

	private LightControllerState lightControllerState;

	public LightControllerState getLightControllerState() {
		return lightControllerState;
	}
	public void setLightControllerState(LightControllerState lightControllerState) {
		this.lightControllerState = lightControllerState;
	}

}
