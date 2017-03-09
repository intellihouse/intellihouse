package house.intelli.core.rpc.echo;

import javax.xml.bind.annotation.XmlRootElement;

import house.intelli.core.rpc.Response;

@XmlRootElement
public class EchoResponse extends Response {

	private String payload;

	public String getPayload() {
		return payload;
	}
	public void setPayload(String payload) {
		this.payload = payload;
	}

}
