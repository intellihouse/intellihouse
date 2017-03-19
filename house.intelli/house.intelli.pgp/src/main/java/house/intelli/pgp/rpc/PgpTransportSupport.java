package house.intelli.pgp.rpc;

import static house.intelli.core.util.AssertUtil.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import house.intelli.core.auth.SignatureException;
import house.intelli.core.jaxb.IntelliHouseJaxbContext;
import house.intelli.core.rpc.HostId;
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

	private final Pgp pgp = PgpRegistry.getInstance().getPgpOrFail();
	private final Map<String, PgpKey> hostIdStr2PgpKey = new HashMap<>();

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

		final HostId serverHostId = assertNotNull(getServerHostId(), "serverHostId");
		if (HostId.SERVER.equals(hostId))
			return serverHostId;
		else
			return hostId;
	}

	public PgpKey getMasterKeyOrFail(final HostId hostId) {
		assertNotNull(hostId, "hostId");
		PgpKey masterKey = getMasterKey(hostId);
		if (masterKey == null)
			throw new IllegalArgumentException(String.format("No PGP key found for hostId='%s'!", hostId));

		return masterKey;
	}

	public PgpKey getMasterKey(final HostId hostId) {
		assertNotNull(hostId, "hostId");
		if (hostIdStr2PgpKey.isEmpty()) {
			for (PgpKey pgpKey : pgp.getMasterKeys()) {
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
		assertNotNull(rpcMessage, "rpcMessage");
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
		assertNotNull(serialized, "serialized");
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
		assertNotNull(plainData, "plainData");
		assertNotNull(senderHostId, "senderHostId");
		assertNotNull(recipientHostId, "recipientHostId");
		final long startTimestampTotal = System.currentTimeMillis();

		final long startTimestampLookupPgpKeyForSenderHostId = System.currentTimeMillis();
		PgpKey senderKey = getMasterKeyOrFail(senderHostId);
		final long stopTimestampLookupPgpKeyForSenderHostId = System.currentTimeMillis();

		final long startTimestampLookupPgpKeyForRecipientHostId = System.currentTimeMillis();
		PgpKey recipientKey = getMasterKeyOrFail(recipientHostId);
		final long stopTimestampLookupPgpKeyForRecipientHostId = System.currentTimeMillis();

		ByteArrayOutputStream bout = new ByteArrayOutputStream();

		PgpEncoder encoder = pgp.createEncoder(new ByteArrayInputStream(plainData), bout);
		encoder.setSignPgpKey(senderKey);
		encoder.getEncryptPgpKeys().add(recipientKey);

		final long startTimestampEncode = System.currentTimeMillis();
		encoder.encode();
		final long stopTimestampEncode = System.currentTimeMillis();

		if (logger.isDebugEnabled()) {
			logger.debug("encryptAndSign: lookupSenderKeyDuration={}ms, lookupRecipientKeyDuration={}ms, encodeDuration={}ms, totalDuration={}ms",
					stopTimestampLookupPgpKeyForSenderHostId - startTimestampLookupPgpKeyForSenderHostId,
					stopTimestampLookupPgpKeyForRecipientHostId - startTimestampLookupPgpKeyForRecipientHostId,
					stopTimestampEncode - startTimestampEncode,
					System.currentTimeMillis() - startTimestampTotal);
		}
		return bout.toByteArray();
	}

	public byte[] decryptAndVerifySignature(final byte[] encryptedData, final HostId senderHostId) throws IOException {
		assertNotNull(encryptedData, "encryptedData");
		assertNotNull(senderHostId, "senderHostId");
		final long startTimestampTotal = System.currentTimeMillis();

		ByteArrayOutputStream bout = new ByteArrayOutputStream();

		PgpDecoder decoder = pgp.createDecoder(new ByteArrayInputStream(encryptedData), bout);
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
			logger.debug("decryptAndVerifySignature: decodeDuration={}ms, lookupKeyDuration={}ms, totalDuration={}ms",
					stopTimestampDecode - startTimestampDecode,
					stopTimestampLookupPgpKey - startTimestampLookupPgpKey,
					System.currentTimeMillis() - startTimestampTotal);
		}
		return bout.toByteArray();
	}

	protected JAXBContext getJaxbContext() {
		if (jaxbContext == null)
			jaxbContext = IntelliHouseJaxbContext.getJaxbContext();

		return jaxbContext;
	}
}
