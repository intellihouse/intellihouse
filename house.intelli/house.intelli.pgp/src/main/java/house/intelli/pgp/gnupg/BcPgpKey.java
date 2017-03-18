package house.intelli.pgp.gnupg;

import static house.intelli.core.util.AssertUtil.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.bouncycastle.bcpg.PublicKeyAlgorithmTags;
import org.bouncycastle.bcpg.sig.KeyFlags;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.bouncycastle.openpgp.PGPSecretKeyRing;
import org.bouncycastle.openpgp.PGPSignature;
import org.bouncycastle.openpgp.PGPSignatureSubpacketVector;
import org.bouncycastle.openpgp.wot.TrustDb;

import house.intelli.pgp.PgpKey;
import house.intelli.pgp.PgpKeyAlgorithm;
import house.intelli.pgp.PgpKeyFingerprint;
import house.intelli.pgp.PgpKeyFlag;
import house.intelli.pgp.PgpKeyId;

public class BcPgpKey {

	private final BcWithLocalGnuPgPgp pgp;

	private final PgpKeyId pgpKeyId;

	private PGPPublicKeyRing publicKeyRing;

	private PGPSecretKeyRing secretKeyRing;

	private PGPPublicKey publicKey;

	private PGPSecretKey secretKey;

	private BcPgpKey masterKey;

	// A sub-key may be added twice, because we enlist from both the secret *and* public key ring
	// collection. Therefore, we now use a LinkedHashSet (instead of an ArrayList).
	private Set<PgpKeyId> subKeyIds;

	private PgpKey pgpKey;

	public BcPgpKey(final BcWithLocalGnuPgPgp pgp, final PgpKeyId pgpKeyId) {
		this.pgp = pgp;
		this.pgpKeyId = assertNotNull(pgpKeyId, "pgpKeyId");
	}

	public PgpKeyId getPgpKeyId() {
		return pgpKeyId;
	}

	public PGPPublicKeyRing getPublicKeyRing() {
		return publicKeyRing;
	}
	public void setPublicKeyRing(PGPPublicKeyRing publicKeyRing) {
		this.publicKeyRing = publicKeyRing;
	}

	public PGPSecretKeyRing getSecretKeyRing() {
		return secretKeyRing;
	}
	public void setSecretKeyRing(PGPSecretKeyRing secretKeyRing) {
		this.secretKeyRing = secretKeyRing;
	}

	public PGPPublicKey getPublicKey() {
		return publicKey;
	}
	public void setPublicKey(final PGPPublicKey publicKey) {
		this.publicKey = publicKey;
	}

	public PGPSecretKey getSecretKey() {
		return secretKey;
	}
	public void setSecretKey(final PGPSecretKey secretKey) {
		this.secretKey = secretKey;
	}

	public BcPgpKey getMasterKey() {
		return masterKey;
	}
	public void setMasterKey(BcPgpKey masterKey) {
		this.masterKey = masterKey;
	}

	public Set<PgpKeyId> getSubKeyIds() {
		if (subKeyIds == null && masterKey == null) // only a master-key can have sub-keys! hence we keep it null, if this is not a master-key!
			subKeyIds = new LinkedHashSet<>();

		return subKeyIds;
	}

	public synchronized PgpKey getPgpKey() {
		// We make sure, masterKey.pgpKey is initialised, before we do anything in sub-keys!
		final PgpKey masterPgpKey = masterKey == null ? null : masterKey.getPgpKey();

		if (pgpKey == null) {
			final byte[] fingerprint = assertNotNull(publicKey, "publicKey").getFingerprint();
			final boolean secretKeyAvailable = secretKey != null && ! secretKey.isPrivateKeyEmpty();

			final List<String> userIds = new ArrayList<String>();
			for (final Iterator<?> itUserIDs = publicKey.getUserIDs(); itUserIDs.hasNext(); )
				userIds.add((String) itUserIDs.next());

			final long validSeconds = publicKey.getValidSeconds();
			final Date created = publicKey.getCreationTime();
			final Date validTo = validSeconds < 1 ? null : new Date(created.getTime() + (validSeconds * 1000));
			final boolean disabled;
			try (TrustDb trustDb = pgp.createTrustDb()) {
				disabled = trustDb.isDisabled(
						masterPgpKey == null ? publicKey : masterKey.getPublicKey());
			}
			this.pgpKey = new PgpKey(
					pgpKeyId, new PgpKeyFingerprint(fingerprint), masterPgpKey, created, validTo,
					getPgpKeyAlgorithm(publicKey.getAlgorithm()), publicKey.getBitStrength(),
					secretKeyAvailable, userIds, getPgpKeyFlags(), publicKey.isRevoked(), disabled);

			getSubKeyIds();
			if (this.subKeyIds == null)
			    this.pgpKey.setSubKeys(null);
			else {
			    this.subKeyIds = Collections.unmodifiableSet(new LinkedHashSet<>(this.subKeyIds)); // turn read-only!

    			final List<PgpKey> subKeys = new ArrayList<PgpKey>(this.subKeyIds.size());
    			for (final PgpKeyId subKeyId : this.subKeyIds) {
    				final BcPgpKey subKey = pgp.getBcPgpKey(subKeyId);
    				if (subKey == null)
    					throw new IllegalStateException("Key not found: " + subKeyId);

    				subKeys.add(subKey.getPgpKey());
    			}
    			this.pgpKey.setSubKeys(subKeys);
			}
		}
		return pgpKey;
	}

	private PgpKeyAlgorithm getPgpKeyAlgorithm(int algorithm) {
		switch (algorithm) {
			case PublicKeyAlgorithmTags.RSA_ENCRYPT:
			case PublicKeyAlgorithmTags.RSA_GENERAL:
			case PublicKeyAlgorithmTags.RSA_SIGN:
				return PgpKeyAlgorithm.RSA;

			case PublicKeyAlgorithmTags.DSA:
				return PgpKeyAlgorithm.DSA;

			case PublicKeyAlgorithmTags.ECDH:
				return PgpKeyAlgorithm.ECDH;

			case PublicKeyAlgorithmTags.ECDSA:
				return PgpKeyAlgorithm.ECDSA;

			case PublicKeyAlgorithmTags.ELGAMAL_ENCRYPT:
			case PublicKeyAlgorithmTags.ELGAMAL_GENERAL:
				return PgpKeyAlgorithm.EL_GAMAL;

			case PublicKeyAlgorithmTags.DIFFIE_HELLMAN:
				return PgpKeyAlgorithm.DIFFIE_HELLMAN;

			default:
				throw new IllegalArgumentException("Unknown algorithm: " + algorithm);
		}
	}

	private Set<PgpKeyFlag> getPgpKeyFlags() {
		final EnumSet<PgpKeyFlag> result = EnumSet.noneOf(PgpKeyFlag.class);

		final PgpKeyId masterKeyId = masterKey == null ? getPgpKeyId() : masterKey.getPgpKeyId();

		final Iterator<?> sigIt = publicKey.getSignatures();
		while (sigIt.hasNext()) {
			final PGPSignature signature = (PGPSignature) sigIt.next();
			if (signature.getKeyID() != masterKeyId.longValue())
				continue;

			// It seems, the signature type is not always the way it should be (to my understanding).
			// To look for PGPSignature.SUBKEY_BINDING works fine, but PGPSignature.PRIMARYKEY_BINDING seems to
			// never be used. I thus do not filter by signature-type at all, anymore, but collect all keyFlags
			// from all signatures made by the master-key.

			final PGPSignatureSubpacketVector hashedSubPackets = signature.getHashedSubPackets();
			if (hashedSubPackets != null) {
				final int keyFlags = hashedSubPackets.getKeyFlags();
				result.addAll(getPgpKeyFlags(keyFlags));
			}
		}
		return result;
	}

	private static Set<PgpKeyFlag> getPgpKeyFlags(int keyFlags) {
		final EnumSet<PgpKeyFlag> result = EnumSet.noneOf(PgpKeyFlag.class);

		// Using org.bouncycastle.bcpg.sig.KeyFlags instead of org.bouncycastle.openpgp.PGPKeyFlags, because
		// it seems more complete. Seems OpenPGP did not yet standardize all (CAN_AUTHENTICATE / AUTHENTICATION is missing).

		if ((keyFlags & KeyFlags.CERTIFY_OTHER) != 0)
			result.add(PgpKeyFlag.CAN_CERTIFY);

		if ((keyFlags & KeyFlags.SIGN_DATA) != 0)
			result.add(PgpKeyFlag.CAN_SIGN);

		if ((keyFlags & KeyFlags.AUTHENTICATION) != 0)
			result.add(PgpKeyFlag.CAN_AUTHENTICATE);

		if ((keyFlags & KeyFlags.ENCRYPT_COMMS) != 0)
			result.add(PgpKeyFlag.CAN_ENCRYPT_COMMS);

		if ((keyFlags & KeyFlags.ENCRYPT_STORAGE) != 0)
			result.add(PgpKeyFlag.CAN_ENCRYPT_STORAGE);

		if ((keyFlags & KeyFlags.SPLIT) != 0)
			result.add(PgpKeyFlag.MAYBE_SPLIT);

		if ((keyFlags & KeyFlags.SHARED) != 0)
			result.add(PgpKeyFlag.MAYBE_SHARED);

		return result;
	}
}
