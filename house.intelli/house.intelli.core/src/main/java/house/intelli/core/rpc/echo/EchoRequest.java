package house.intelli.core.rpc.echo;

import javax.xml.bind.annotation.XmlRootElement;

import house.intelli.core.rpc.Request;

@XmlRootElement
public class EchoRequest extends Request {

	private String payload;

	public String getPayload() {
		return payload;
	}
	public void setPayload(String payload) {
		this.payload = payload;
	}

}
