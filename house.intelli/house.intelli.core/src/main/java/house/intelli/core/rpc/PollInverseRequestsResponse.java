package house.intelli.core.rpc;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class PollInverseRequestsResponse extends Response {

	private List<Request<?>> inverseRequests;

	public List<Request<?>> getInverseRequests() {
		if (inverseRequests == null)
			inverseRequests = new ArrayList<>();

		return inverseRequests;
	}
	public void setInverseRequests(List<Request<?>> requests) {
		this.inverseRequests = requests;
	}

	@Override
	protected String toString_getProperties() {
		return super.toString_getProperties() + ", inverseRequests=" + inverseRequests;
	}
}
