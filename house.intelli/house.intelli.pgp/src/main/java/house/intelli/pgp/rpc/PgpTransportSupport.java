package house.intelli.pgp.rpc;

import static java.util.Objects.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.bouncycastle.crypto.BlockCipher;
import org.bouncycastle.crypto.StreamCipher;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import house.intelli.core.Uid;
import house.intelli.core.auth.SignatureException;
import house.intelli.core.jaxb.IntelliHouseJaxbContext;
import house.intelli.core.rpc.Error;
import house.intelli.core.rpc.ErrorResponse;
import house.intelli.core.rpc.HostId;
import house.intelli.core.rpc.Response;
import house.intelli.core.rpc.RpcMessage;
import house.intelli.pgp.Pgp;
import house.intelli.pgp.PgpDecoder;
import house.intelli.pgp.PgpEncoder;
import house.intelli.pgp.PgpKey;
import house.intelli.pgp.PgpKeyValidity;
import house.intelli.pgp.PgpRegistry;
import house.intelli.pgp.PgpSignature;

public class PgpTransportSupport {

	private static final Logger logger = LoggerFactory.getLogger(PgpTransportSupport.class);

	private HostId serverHostId;
	private JAXBContext jaxbContext;

	private static final SymmetricCryptoType symmetricCryptoType = SymmetricCryptoType.TWOFISH_CFB_NOPADDING; // maybe we make this configurable later...
	private static final HashType hashType = HashType.SHA256; // maybe we make this configurable later...

	private final Pgp pgp = PgpRegistry.getInstance().getPgpOrFail();
	private final Map<String, PgpKey> hostIdStr2PgpKey = new HashMap<>();
	private static final SecureRandom random = Session.random;

	public static final byte[] ENCRYPTED_DATA_HEADER = new byte[] {
		'i', 't', 'l', 'i', 'h', 's'
	};

	public static final byte ENCRYPTED_DATA_VERSION = 0;

	public static final byte ENCRYPTED_DATA_MODE_PGP = 0;

	public static final byte ENCRYPTED_DATA_MODE_SYMMETRIC = 1;

	public PgpTransportSupport() {
	}

	public HostId getServerHostId() {
		return serverHostId;
	}
	public void setServerHostId(HostId serverHostId) {
		this.serverHostId = serverHostId;
	}

	public HostId resolveRealServerHostId(final HostId hostId) {
		if (hostId == null)
			return null;

		final HostId serverHostId = requireNonNull(getServerHostId(), "serverHostId");
		if (HostId.SERVER.equals(hostId))
			return serverHostId;
		else
			return hostId;
	}

	public HostId resolveAliasHostId(final HostId hostId) {
		if (hostId == null)
			return null;

		final HostId serverHostId = requireNonNull(getServerHostId(), "serverHostId");
		if (serverHostId.equals(hostId))
			return HostId.SERVER;
		else
			return hostId;
	}

	public PgpKey getMasterKeyOrFail(final HostId hostId) {
		requireNonNull(hostId, "hostId");
		PgpKey masterKey = getMasterKey(hostId);
		if (masterKey == null)
			throw new IllegalArgumentException(String.format("No PGP key found for hostId='%s'!", hostId));

		return masterKey;
	}

	public PgpKey getMasterKey(final HostId hostId) {
		requireNonNull(hostId, "hostId");
		final Date now = new Date();
		if (hostIdStr2PgpKey.isEmpty()) {
			for (PgpKey pgpKey : pgp.getMasterKeys()) {
				if (! pgpKey.isValid(now)) {
					logger.info("getMasterKey: Ignoring non-valid key: {}", pgpKey);
					continue;
				}
				for (String userId : pgpKey.getUserIds()) {
					final String hostIdStr = userId.trim();
					hostIdStr2PgpKey.put(hostIdStr, pgpKey);
				}
			}
		}
		final String hostIdStr = hostId.toString();
		return hostIdStr2PgpKey.get(hostIdStr);
	}

	public byte[] serializeRpcMessage(RpcMessage rpcMessage) throws IOException {
		requireNonNull(rpcMessage, "rpcMessage");
		try {
			ByteArrayOutputStream bout = new ByteArrayOutputStream();
			try (GZIPOutputStream gzOut = new GZIPOutputStream(bout)) {
				Marshaller marshaller = getJaxbContext().createMarshaller();
				marshaller.marshal(rpcMessage, gzOut);
			}
			return bout.toByteArray();
		} catch (JAXBException e) {
			throw new IOException(e);
		}
	}

	public RpcMessage deserializeRpcMessage(byte[] serialized) throws IOException {
		requireNonNull(serialized, "serialized");
		try {
			try (GZIPInputStream gzIn = new GZIPInputStream(new ByteArrayInputStream(serialized))) {
				Unmarshaller unmarshaller = getJaxbContext().createUnmarshaller();
				Object object = unmarshaller.unmarshal(gzIn);
				return (RpcMessage) object;
			}
		} catch (JAXBException e) {
			throw new IOException(e);
		}
	}

	public byte[] encryptAndSign(final byte[] plainData, final HostId senderHostId, final HostId recipientHostId) throws IOException {
		requireNonNull(plainData, "plainData");
		requireNonNull(senderHostId, "senderHostId");
		requireNonNull(recipientHostId, "recipientHostId");
		final long startTimestampTotal = System.currentTimeMillis();

		final SessionHostIdPair sessionHostIdPair = new SessionHostIdPair(senderHostId, recipientHostId);
		final SessionManager sessionManager = SessionManager.getInstance();
		final Session session = sessionManager.getOrCreateSession(sessionHostIdPair);
		session.confirmByHostId(senderHostId);

		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		DataOutputStream dout = new DataOutputStream(bout);
		dout.write(ENCRYPTED_DATA_HEADER);
		dout.write(ENCRYPTED_DATA_VERSION);

		logger.debug("encryptAndSign: session={}", session);
		if (session.isConfirmedCompletely()) {
			dout.writeByte(ENCRYPTED_DATA_MODE_SYMMETRIC);

			dout.writeInt(symmetricCryptoType.ordinal());

			// write sessionId
			writeShortByteArray(dout, session.getSessionId().toBytes());

			CipherWithIv cipherWithIv = acquireInitializedCipherForEncryption(symmetricCryptoType, session.getSessionKey());

			// write IV
			writeShortByteArray(dout, cipherWithIv.iv);

			// There is no need to sign this data, because only the two session partners know the key.
			// But we generate an SHA256 hash to make sure, nobody can manipulate the data.
			final byte[] plainDataWithHash = combinePlainDataWithHash(plainData);

			// encrypt data and write cipher-text to output-stream
			final byte[] enc = new byte[plainDataWithHash.length * 3 / 2];
			final long startTimestampEncode = System.currentTimeMillis();
			int encLen = cipherWithIv.cipher.processBytes(plainDataWithHash, 0, plainDataWithHash.length, enc, 0);
			final long stopTimestampEncode = System.currentTimeMillis();
			writeLongByteArray(dout, enc, 0, encLen);

			releaseCipher(cipherWithIv);

			if (logger.isDebugEnabled()) {
				logger.debug("encryptAndSign: mode=SYMMETRIC, encodeDuration={}ms, totalDuration={}ms",
						stopTimestampEncode - startTimestampEncode,
						System.currentTimeMillis() - startTimestampTotal);
			}
		} else {
			dout.writeByte(ENCRYPTED_DATA_MODE_PGP);

			byte[] sessionRequestBytes = serializeSessionRequest(session);

			ByteArrayOutputStream plainDataBout = new ByteArrayOutputStream();
			DataOutputStream plainDataDout = new DataOutputStream(plainDataBout);

			writeLongByteArray(plainDataDout, sessionRequestBytes);
			writeLongByteArray(plainDataDout, plainData);

			final long startTimestampLookupPgpKeyForSenderHostId = System.currentTimeMillis();
			PgpKey senderKey = getMasterKeyOrFail(senderHostId);
			final long stopTimestampLookupPgpKeyForSenderHostId = System.currentTimeMillis();

			final long startTimestampLookupPgpKeyForRecipientHostId = System.currentTimeMillis();
			PgpKey recipientKey = getMasterKeyOrFail(recipientHostId);
			final long stopTimestampLookupPgpKeyForRecipientHostId = System.currentTimeMillis();

			PgpEncoder encoder = pgp.createEncoder(new ByteArrayInputStream(plainDataBout.toByteArray()), dout);
			encoder.setSignPgpKey(senderKey);
			encoder.getEncryptPgpKeys().add(recipientKey);

			final long startTimestampEncode = System.currentTimeMillis();
			encoder.encode();
			final long stopTimestampEncode = System.currentTimeMillis();

			if (logger.isDebugEnabled()) {
				logger.debug("encryptAndSign: mode=PGP, lookupSenderKeyDuration={}ms, lookupRecipientKeyDuration={}ms, encodeDuration={}ms, totalDuration={}ms",
						stopTimestampLookupPgpKeyForSenderHostId - startTimestampLookupPgpKeyForSenderHostId,
						stopTimestampLookupPgpKeyForRecipientHostId - startTimestampLookupPgpKeyForRecipientHostId,
						stopTimestampEncode - startTimestampEncode,
						System.currentTimeMillis() - startTimestampTotal);
			}
		}
		return bout.toByteArray();
	}

	public byte[] decryptAndVerifySignature(final byte[] encryptedData, final HostId senderHostId, final HostId recipientHostId) throws IOException {
		requireNonNull(encryptedData, "encryptedData");
		requireNonNull(senderHostId, "senderHostId");
		requireNonNull(recipientHostId, "recipientHostId");
		final long startTimestampTotal = System.currentTimeMillis();

		final SessionManager sessionManager = SessionManager.getInstance();
		ByteArrayInputStream bin = new ByteArrayInputStream(encryptedData);
		DataInputStream din = new DataInputStream(bin);

		byte[] header = new byte[ENCRYPTED_DATA_HEADER.length];
		din.readFully(header);
		if (! Arrays.equals(ENCRYPTED_DATA_HEADER, header))
			throw new IllegalArgumentException(String.format("Header illegal! expected=%s, found=%s",
					Arrays.toString(ENCRYPTED_DATA_HEADER), Arrays.toString(header)));

		byte version = din.readByte();
		if (ENCRYPTED_DATA_VERSION != version)
			throw new IllegalArgumentException(String.format("Version illegal! expected=%s, found=%s",
					ENCRYPTED_DATA_VERSION, version));

		byte mode = din.readByte();
		if (ENCRYPTED_DATA_MODE_SYMMETRIC == mode) {
			int symmetricCryptoTypeOrdinal = din.readInt();
			SymmetricCryptoType symmetricCryptoType = SymmetricCryptoType.values()[symmetricCryptoTypeOrdinal];

			Uid sessionId = new Uid(readShortByteArray(din));

			Session session = sessionManager.getSessionOrFail(sessionId);
			logger.debug("decryptAndVerifySignature: session={}", session);

			// There might be multiple messages in parallel, hence symmetric encryption might already be used
			// by our peer, before the PGP-encrypted response is processed here. Therefore, we must implicitly confirm.
			// *He* (our communication partner) already uses this, so it is obviously confirmed by him, and we can mark this.
			session.confirmByHostId(senderHostId);

			final byte[] iv = readShortByteArray(din);
			final byte[] enc = readLongByteArray(din);

			final byte[] buf = new byte[enc.length * 3 / 2]; // decrypted data should never be larger than encrypted data, but we're careful
			StreamCipher cipher = acquireInitializedCipherForDecryption(symmetricCryptoType, session.getSessionKey(), iv);
			final long startTimestampDecode = System.currentTimeMillis();
			int plainDataWithHashLength = cipher.processBytes(enc, 0, enc.length, buf, 0);
			final long stopTimestampDecode = System.currentTimeMillis();
			releaseCipher(cipher);

			final byte[] plainDataWithHash = new byte[plainDataWithHashLength];
			System.arraycopy(buf, 0, plainDataWithHash, 0, plainDataWithHashLength);

			final byte[] plainData = splitPlainDataFromHashWithVerification(plainDataWithHash);

			if (logger.isDebugEnabled()) {
				logger.debug("decryptAndVerifySignature: mode=SYMMETRIC, decodeDuration={}ms, totalDuration={}ms",
						stopTimestampDecode - startTimestampDecode,
						System.currentTimeMillis() - startTimestampTotal);
			}

			return plainData;
		}
		else if (ENCRYPTED_DATA_MODE_PGP == mode) {
			ByteArrayOutputStream plainDataBout = new ByteArrayOutputStream();

			PgpDecoder decoder = pgp.createDecoder(din, plainDataBout);
			final long startTimestampDecode = System.currentTimeMillis();
			try {
				decoder.decode();
			} catch (SignatureException e) {
				throw new IOException(e);
			}
			final long stopTimestampDecode = System.currentTimeMillis();

			PgpSignature signature = decoder.getPgpSignature();
			if (signature == null)
				throw new IOException("encryptedData was not signed!");

			final long startTimestampLookupPgpKey = System.currentTimeMillis();
			PgpKey pgpKey = pgp.getPgpKey(signature.getPgpKeyId());
			if (pgpKey == null)
				throw new IOException(String.format("encryptedData was signed by *unknown* key %s!",
						signature.getPgpKeyId().toHumanString()));
			final long stopTimestampLookupPgpKey = System.currentTimeMillis();

			List<String> userIds = pgpKey.getUserIds();
			if (! userIds.contains(senderHostId.toString()))
				throw new IOException(String.format("encryptedData was signed by key '%s' which does not have the userId '%s' associated! userIds of this key are: %s",
						signature.getPgpKeyId().toHumanString(), senderHostId, userIds));

			PgpKeyValidity keyValidity = pgp.getKeyValidity(pgpKey);
			PgpKeyValidity minimumKeyValidity = PgpKeyValidity.FULL;
			if (minimumKeyValidity.compareTo(keyValidity) > 0)
				throw new IOException(String.format("encryptedData was signed by key '%s' (userId '%s'), which is not trusted/valid! minimumKeyValidity=%s, foundKeyValidity=%s",
						signature.getPgpKeyId().toHumanString(), senderHostId, minimumKeyValidity, keyValidity));

			if (logger.isDebugEnabled()) {
				logger.debug("decryptAndVerifySignature: mode=PGP, decodeDuration={}ms, lookupKeyDuration={}ms, totalDuration={}ms",
						stopTimestampDecode - startTimestampDecode,
						stopTimestampLookupPgpKey - startTimestampLookupPgpKey,
						System.currentTimeMillis() - startTimestampTotal);
			}

			ByteArrayInputStream plainDataBin = new ByteArrayInputStream(plainDataBout.toByteArray());
			DataInputStream plainDataDin = new DataInputStream(plainDataBin);

			byte[] sessionRequestBytes = readLongByteArray(plainDataDin);

			SessionRequest sessionRequest = deserializeSessionRequest(sessionRequestBytes);
			Session session = sessionManager.getSession(sessionRequest.getSessionId());
			if (session == null) {
				session = sessionRequest.createSession();
				session.confirmByHostId(senderHostId); // *He* created the session, hence he obviously confirmed it.
				session.confirmByHostId(recipientHostId); // *I* must confirm, too.
				logger.debug("decryptAndVerifySignature: enlisted new session={}", session);
				sessionManager.putSession(session);
			}
			else {
				logger.debug("decryptAndVerifySignature: found and confirmed session={}", session);
				if (! Arrays.equals(session.getSessionKey(), sessionRequest.getSessionKey()))
					throw new IllegalArgumentException("localSession.sessionKey != sessionRequest.sessionKey");

				if (! session.getSessionHostIdPair().equals(sessionRequest.getSessionHostIdPair()))
					throw new IllegalArgumentException(String.format(
							"localSession.sessionHostIdPair != sessionRequest.sessionHostIdPair :: %s != %s",
							session.getSessionHostIdPair(), sessionRequest.getSessionHostIdPair()));

				session.confirmByHostId(senderHostId); // reusing local session => *he* confirmed by sending this to me -- must enlist!
			}

			byte[] plainData = readLongByteArray(plainDataDin);
			return plainData;
		}
		else
			throw new IllegalArgumentException(String.format("Mode illegal! expected=(%s|%s), found=%s",
					ENCRYPTED_DATA_MODE_SYMMETRIC, ENCRYPTED_DATA_MODE_PGP, mode));
	}

	public void handleSessionNotFoundException(Response response) {
		if (! (response instanceof ErrorResponse))
			return;

		final ErrorResponse errorResponse = (ErrorResponse) response;
		final Uid sessionId = getSessionNotFoundExceptionSessionId(errorResponse.getError());
		if (sessionId == null)
				return;

		final SessionManager sessionManager = SessionManager.getInstance();
		sessionManager.removeSession(sessionId);
	}

	private Uid getSessionNotFoundExceptionSessionId(final Error error) {
		if (error == null)
			return null;

		Class<?> clazz;
		try {
			clazz = Class.forName(error.getClassName());
		} catch (final ClassNotFoundException e) {
			clazz = null;
		}

		if (clazz != null && SessionNotFoundException.class.isAssignableFrom(clazz))
			return SessionManager.getSessionIdFromSessionNotFoundExceptionMessage(error.getMessage());

		return getSessionNotFoundExceptionSessionId(error.getCause());
	}

	private byte[] serializeSessionRequest(final Session session) throws IOException {
		requireNonNull(session, "session");
		try {
			ByteArrayOutputStream sessionRequestOut = new ByteArrayOutputStream();
			getJaxbContext().createMarshaller().marshal(new SessionRequest(session), sessionRequestOut);
			return sessionRequestOut.toByteArray();
		} catch (JAXBException e) {
			throw new IOException(e);
		}
	}

	private SessionRequest deserializeSessionRequest(final byte[] sessionRequestBytes) throws IOException {
		requireNonNull(sessionRequestBytes, "sessionRequestBytes");
		try {
			Object deserialized = getJaxbContext().createUnmarshaller().unmarshal(new ByteArrayInputStream(sessionRequestBytes));
			return (SessionRequest) deserialized;
		} catch (JAXBException e) {
			throw new IOException(e);
		}
	}

	private static void writeLongByteArray(DataOutputStream dout, byte[] byteArray) throws IOException {
		requireNonNull(dout, "dout");
		requireNonNull(byteArray, "byteArray");
		dout.writeInt(byteArray.length);
		dout.write(byteArray);
	}

	private static void writeLongByteArray(DataOutputStream dout, byte[] byteArray, int offset, int length) throws IOException {
		requireNonNull(dout, "dout");
		requireNonNull(byteArray, "byteArray");
		dout.writeInt(length);
		dout.write(byteArray, offset, length);
	}

	private static void writeShortByteArray(DataOutputStream dout, byte[] byteArray) throws IOException {
		requireNonNull(dout, "dout");
		requireNonNull(byteArray, "byteArray");
		if (byteArray.length > 255)
			throw new IllegalStateException("byteArray.length > 255");

		dout.writeByte(byteArray.length);
		dout.write(byteArray);
	}

	private static byte[] readLongByteArray(DataInputStream din) throws IOException {
		requireNonNull(din, "din");
		int byteArrayLength = din.readInt();
		byte[] byteArray = new byte[byteArrayLength];
		din.readFully(byteArray);
		return byteArray;
	}

	private static byte[] readShortByteArray(DataInputStream din) throws IOException {
		requireNonNull(din, "din");
		int byteArrayLength = din.readByte() & 0xFF;
		byte[] byteArray = new byte[byteArrayLength];
		din.readFully(byteArray);
		return byteArray;
	}

	protected JAXBContext getJaxbContext() {
		if (jaxbContext == null)
			jaxbContext = IntelliHouseJaxbContext.getJaxbContext();

		return jaxbContext;
	}

	private static CipherWithIv acquireInitializedCipherForEncryption(SymmetricCryptoType symmetricCryptoType, byte[] key) {
		requireNonNull(symmetricCryptoType, "symmetricCryptoType");
		requireNonNull(key, "key");
		StreamCipher cipher = CipherManager.getInstance().acquireCipher(symmetricCryptoType);
		byte[] iv = new byte[getIvSize(cipher)];
		random.nextBytes(iv);
		KeyParameter kp = new KeyParameter(key);
		ParametersWithIV params = new ParametersWithIV(kp, iv);
		cipher.init(true, params);
		return new CipherWithIv(cipher, iv);
	}

	private static StreamCipher acquireInitializedCipherForDecryption(SymmetricCryptoType symmetricCryptoType, byte[] key, byte[] iv) {
		requireNonNull(symmetricCryptoType, "symmetricCryptoType");
		requireNonNull(key, "key");
		requireNonNull(iv, "iv");
		StreamCipher cipher = CipherManager.getInstance().acquireCipher(symmetricCryptoType);
		KeyParameter kp = new KeyParameter(key);
		ParametersWithIV params = new ParametersWithIV(kp, iv);
		cipher.init(false, params);
		return cipher;
	}

	private static void releaseCipher(final CipherWithIv cipherWithIv) {
		requireNonNull(cipherWithIv, "cipherWithIv");
		releaseCipher(cipherWithIv.cipher);
	}

	private static void releaseCipher(final StreamCipher cipher) {
		requireNonNull(cipher, "cipher");
		CipherManager.getInstance().releaseCipher(cipher);
	}

	protected static byte[] sha256(final byte[] in) {
		requireNonNull(in, "in");
		final SHA256Digest digest = new SHA256Digest();
		final byte[] out = new byte[digest.getDigestSize()];
		digest.update(in, 0, in.length);
		digest.doFinal(out, 0);
		return out;
	}

	protected static byte[] combinePlainDataWithHash(final byte[] plainData) throws IOException {
		requireNonNull(plainData, "in");
		ByteArrayOutputStream bout = new ByteArrayOutputStream(plainData.length + 128);
		DataOutputStream dout = new DataOutputStream(bout);
		dout.writeInt(hashType.ordinal());
		writeLongByteArray(dout, plainData);
		writeShortByteArray(dout, sha256(plainData));
		return bout.toByteArray();
	}

	protected static byte[] splitPlainDataFromHashWithVerification(final byte[] in) throws IOException {
		requireNonNull(in, "in");
		DataInputStream din = new DataInputStream(new ByteArrayInputStream(in));
		int hashTypeOrdinal = din.readInt();
		if (hashTypeOrdinal < 0)
			throw new IOException("hashTypeOrdinal < 0!!! hashTypeOrdinal=" + hashTypeOrdinal);

		if (hashTypeOrdinal >= HashType.values().length)
			throw new IOException("hashTypeOrdinal >= HashType.values.length!!! hashTypeOrdinal=" + hashTypeOrdinal);

		final HashType hashType = HashType.values()[hashTypeOrdinal];
		final byte[] plainData = readLongByteArray(din);
		final byte[] declaredHash = readShortByteArray(din);

		final byte[] foundHash;
		switch (hashType) {
			case SHA256:
				foundHash = sha256(plainData);
				break;
			default:
				throw new UnsupportedOperationException("hashType: " + hashType);
		}

		if (! Arrays.equals(declaredHash, foundHash))
			throw new IOException("Data corruption: Declared hash does not match found hash!!!");

		return plainData;
	}

	protected static int getIvSize(StreamCipher cipher) {
		requireNonNull(cipher, "cipher");
		if (cipher instanceof BlockCipher) // it's likely a StreamBlockCipher
			return ((BlockCipher) cipher).getBlockSize();
		else
			throw new UnsupportedOperationException("cipher is not an instance of BlockCipher! Not yet supported: " + cipher.getClass().getName());
	}
}
