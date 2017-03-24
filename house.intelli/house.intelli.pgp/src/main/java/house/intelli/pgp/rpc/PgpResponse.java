package house.intelli.pgp.rpc;

import javax.xml.bind.annotation.XmlRootElement;

import house.intelli.core.Uid;
import house.intelli.core.rpc.Response;

@XmlRootElement
public class PgpResponse extends Response {

	private Uid sessionId;
	private byte[] encryptedSessionRequest;
	private byte[] encryptedResponse;

	public PgpResponse() {
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
	public void setEncryptedSessionRequest(byte[] encryptedSessionRequest) {
		this.encryptedSessionRequest = encryptedSessionRequest;
	}

	public byte[] getEncryptedResponse() {
		return encryptedResponse;
	}
	public void setEncryptedResponse(byte[] encryptedResponse) {
		this.encryptedResponse = encryptedResponse;
	}
}
