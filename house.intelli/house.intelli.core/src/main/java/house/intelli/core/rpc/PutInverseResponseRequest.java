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

	@Override
	public boolean isIdempotent() { // It's not really idempotent, but the worst-case-scenario is leaving garbage that isn't fetched anymore.
		return true;
	}
}
