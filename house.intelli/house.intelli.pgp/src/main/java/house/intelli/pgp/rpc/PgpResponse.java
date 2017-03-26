package house.intelli.pgp.rpc;

import javax.xml.bind.annotation.XmlRootElement;

import house.intelli.core.rpc.Response;

@XmlRootElement
public class PgpResponse extends Response {

	private byte[] encryptedResponse;

	public PgpResponse() {
	}

	public byte[] getEncryptedResponse() {
		return encryptedResponse;
	}
	public void setEncryptedResponse(byte[] encryptedResponse) {
		this.encryptedResponse = encryptedResponse;
	}
}
