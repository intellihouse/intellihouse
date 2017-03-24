package house.intelli.pgp.rpc;

import javax.xml.bind.annotation.XmlRootElement;

import house.intelli.core.Uid;
import house.intelli.core.rpc.Request;

@XmlRootElement
public class PgpRequest extends Request<PgpResponse> {

	private Uid sessionId;
	private byte[] encryptedSessionRequest;
	private byte[] encryptedRequest;

	public PgpRequest() {
	}

	public Uid getSessionId() {
		return sessionId;
	}
	public void setSessionId(Uid sessionId) {
		this.sessionId = sessionId;
	}

	public byte[] getEncryptedSessionRequest() {
		return encryptedSessionRequest;
	}
	public void setEncryptedSessionRequest(byte[] encryptedSessionHandshake) {
		this.encryptedSessionRequest = encryptedSessionHandshake;
	}

	public byte[] getEncryptedRequest() {
		return encryptedRequest;
	}
	public void setEncryptedRequest(byte[] encryptedRequest) {
		this.encryptedRequest = encryptedRequest;
	}

}
