package house.intelli.pgp.rpc;

import static house.intelli.core.util.AssertUtil.*;

import org.bouncycastle.crypto.StreamCipher;

public class CipherWithIv {

	public final StreamCipher cipher;
	public final byte[] iv;

	public CipherWithIv(StreamCipher cipher, byte[] iv) {
		this.cipher = assertNotNull(cipher, "cipher");
		this.iv = assertNotNull(iv, "iv");
	}

}
