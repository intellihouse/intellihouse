package house.intelli.core.rpc.inverse;

import java.util.ArrayList;
import java.util.List;

import house.intelli.core.rpc.Request;
import house.intelli.core.rpc.Response;

public class PollInverseRequestsResponse extends Response {

	private List<Request> requests;

	public List<Request> getRequests() {
		if (requests == null)
			requests = new ArrayList<>();

		return requests;
	}
	public void setRequests(List<Request> requests) {
		this.requests = requests;
	}

}
