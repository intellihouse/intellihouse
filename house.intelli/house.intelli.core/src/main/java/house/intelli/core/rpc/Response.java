package house.intelli.core.rpc;

import static house.intelli.core.util.AssertUtil.*;

public abstract class Response extends RpcMessage {

	public void copyRequestCoordinates(Request request) {
		assertNotNull(request, "request");
		this.setClientHostId(request.getClientHostId());
		this.setRequestId(request.getRequestId());
		this.setServerHostId(request.getServerHostId());
	}

}
