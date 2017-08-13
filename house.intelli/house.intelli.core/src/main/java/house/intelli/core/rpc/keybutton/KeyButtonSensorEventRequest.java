package house.intelli.core.rpc.keybutton;

import javax.xml.bind.annotation.XmlRootElement;

import house.intelli.core.rpc.VoidResponse;
import house.intelli.core.rpc.channel.ChannelRequest;

@XmlRootElement
public class KeyButtonSensorEventRequest extends ChannelRequest<VoidResponse> {
	private boolean down;

	public boolean isDown() {
		return down;
	}
	public void setDown(boolean down) {
		this.down = down;
	}
}
