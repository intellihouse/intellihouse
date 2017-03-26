package house.intelli.pgp.rpc;

import javax.xml.bind.annotation.XmlRootElement;

import house.intelli.core.rpc.Request;

@XmlRootElement
public class PgpRequest extends Request<PgpResponse> {

	private byte[] encryptedRequest;

	public PgpRequest() {
	}

	public byte[] getEncryptedRequest() {
		return encryptedRequest;
	}
	public void setEncryptedRequest(byte[] encryptedRequest) {
		this.encryptedRequest = encryptedRequest;
	}

}
