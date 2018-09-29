package house.intelli.pgp.rpc;

import static java.util.Objects.*;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.WeakHashMap;

import org.bouncycastle.crypto.BlockCipher;
import org.bouncycastle.crypto.StreamCipher;
import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.engines.TwofishEngine;
import org.bouncycastle.crypto.modes.CFBBlockCipher;

public class CipherManager {

	private static final CipherManager instance = new CipherManager();

	private final Map<SymmetricCryptoType, LinkedList<StreamCipher>> symmetricCryptoType2Ciphers = new HashMap<>();
	private final WeakHashMap<StreamCipher, SymmetricCryptoType> cipher2SymmetricCryptoType = new WeakHashMap<>();

	public static CipherManager getInstance() {
		return instance;
	}

	protected CipherManager() {
	}

	public synchronized StreamCipher acquireCipher(final SymmetricCryptoType symmetricCryptoType) {
		requireNonNull(symmetricCryptoType, "symmetricCryptoType");
		LinkedList<StreamCipher> ciphers = symmetricCryptoType2Ciphers.get(symmetricCryptoType);
		if (ciphers == null) {
			ciphers = new LinkedList<>();
			symmetricCryptoType2Ciphers.put(symmetricCryptoType, ciphers);
		}
		StreamCipher cipher = ciphers.poll();
		if (cipher == null) {
			cipher = createCipher(symmetricCryptoType);
			cipher2SymmetricCryptoType.put(cipher, symmetricCryptoType);
		}
		return cipher;
	}

	public synchronized void releaseCipher(final StreamCipher cipher) {
		requireNonNull(cipher, "cipher");
		SymmetricCryptoType symmetricCryptoType = cipher2SymmetricCryptoType.get(cipher);
		if (symmetricCryptoType == null)
			throw new IllegalArgumentException("cipher unknown! Instance was not obtained from this manager via acquireCipher(...), before!");

		LinkedList<StreamCipher> ciphers = symmetricCryptoType2Ciphers.get(symmetricCryptoType);
		requireNonNull(ciphers, "ciphers");
		ciphers.add(cipher);
	}

	private StreamCipher createCipher(SymmetricCryptoType symmetricCryptoType) {
		BlockCipher engine;
		switch (symmetricCryptoType) {
			case AES_CFB_NOPADDING:
				engine = new AESEngine();
				break;
			case TWOFISH_CFB_NOPADDING:
				engine = new TwofishEngine();
				break;
			default:
				throw new IllegalStateException("Unknown symmetricCryptoType: " + symmetricCryptoType);
		}

		// We use "NOPADDING", which is fine with CFB => no need to set up any padding.

		StreamCipher streamCipher = new CFBBlockCipher(engine, 8 * engine.getBlockSize());
		return streamCipher;
	}
}
