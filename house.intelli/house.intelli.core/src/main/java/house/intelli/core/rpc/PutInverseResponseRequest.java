package house.intelli.core.rpc;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class PutInverseResponseRequest extends Request<VoidResponse> {

	private Response inverseResponse;

	public Response getInverseResponse() {
		return inverseResponse;
	}

	public void setInverseResponse(Response inverseResponse) {
		this.inverseResponse = inverseResponse;
	}

}
