package house.intelli.pgp.rpc;

import static java.util.Objects.*;

import org.bouncycastle.crypto.StreamCipher;

public class CipherWithIv {

	public final StreamCipher cipher;
	public final byte[] iv;

	public CipherWithIv(StreamCipher cipher, byte[] iv) {
		this.cipher = requireNonNull(cipher, "cipher");
		this.iv = requireNonNull(iv, "iv");
	}

}
