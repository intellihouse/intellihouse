package house.intelli.core.rpc.echo;

import javax.xml.bind.annotation.XmlRootElement;

import house.intelli.core.rpc.Request;

@XmlRootElement
public class EchoRequest extends Request<EchoResponse> {

	private String payload;
	private String throwExceptionClassName;
	private long sleep;

	public String getPayload() {
		return payload;
	}
	public void setPayload(String payload) {
		this.payload = payload;
	}

	public String getThrowExceptionClassName() {
		return throwExceptionClassName;
	}
	public void setThrowExceptionClassName(String throwExceptionClassName) {
		this.throwExceptionClassName = throwExceptionClassName;
	}

	public long getSleep() {
		return sleep;
	}
	public void setSleep(long sleep) {
		this.sleep = sleep;
	}

	@Override
	public boolean isIdempotent() {
		return true;
	}

	@Override
	protected String toString_getProperties() {
		return super.toString_getProperties() + ", payload=" + payload + ", throwExceptionClassName=" + throwExceptionClassName + ", sleep=" + sleep;
	}
}
