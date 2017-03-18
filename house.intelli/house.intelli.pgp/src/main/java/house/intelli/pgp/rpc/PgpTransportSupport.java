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
		assertNotNull(hostId, "hostId");
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

		PgpKey senderKey = getMasterKeyOrFail(senderHostId);
		PgpKey recipientKey = getMasterKeyOrFail(recipientHostId);

		ByteArrayOutputStream bout = new ByteArrayOutputStream();

		PgpEncoder encoder = pgp.createEncoder(new ByteArrayInputStream(plainData), bout);
		encoder.setSignPgpKey(senderKey);
		encoder.getEncryptPgpKeys().add(recipientKey);
		encoder.encode();

		return bout.toByteArray();
	}

	public byte[] decryptAndVerifySignature(final byte[] encryptedData, final HostId senderHostId) throws IOException {
		assertNotNull(encryptedData, "encryptedData");
		assertNotNull(senderHostId, "senderHostId");

		ByteArrayOutputStream bout = new ByteArrayOutputStream();

		PgpDecoder decoder = pgp.createDecoder(new ByteArrayInputStream(encryptedData), bout);
		try {
			decoder.decode();
		} catch (SignatureException e) {
			throw new IOException(e);
		}
		PgpSignature signature = decoder.getPgpSignature();
		if (signature == null)
			throw new IOException("encryptedData was not signed!");

		PgpKey pgpKey = pgp.getPgpKey(signature.getPgpKeyId());
		if (pgpKey == null)
			throw new IOException(String.format("encryptedData was signed by *unknown* key %s!",
					signature.getPgpKeyId().toHumanString()));

		List<String> userIds = pgpKey.getUserIds();
		if (! userIds.contains(encryptedData.toString()))
			throw new IOException(String.format("encryptedData was signed by key '%s' which does not have the userId '%s' associated! userIds of this key are: %s",
					signature.getPgpKeyId().toHumanString(), encryptedData, userIds));

		PgpKeyValidity keyValidity = pgp.getKeyValidity(pgpKey);
		PgpKeyValidity minimumKeyValidity = PgpKeyValidity.FULL;
		if (minimumKeyValidity.compareTo(keyValidity) > 0)
			throw new IOException(String.format("encryptedData was signed by key '%s' (userId '%s'), which is not trusted/valid! minimumKeyValidity=%s, foundKeyValidity=%s",
					signature.getPgpKeyId().toHumanString(), encryptedData, minimumKeyValidity, keyValidity));

		return bout.toByteArray();
	}

	protected JAXBContext getJaxbContext() {
		if (jaxbContext == null)
			jaxbContext = IntelliHouseJaxbContext.getJaxbContext();

		return jaxbContext;
	}
}
