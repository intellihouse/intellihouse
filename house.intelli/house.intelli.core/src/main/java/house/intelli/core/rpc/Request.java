package house.intelli.core.rpc;

import javax.xml.bind.annotation.XmlRootElement;

import house.intelli.core.Uid;

@XmlRootElement
public abstract class Request {

	private Uid requestId;

	public Uid getRequestId() {
		return requestId;
	}
	public void setRequestId(Uid requestId) {
		this.requestId = requestId;
	}

}
