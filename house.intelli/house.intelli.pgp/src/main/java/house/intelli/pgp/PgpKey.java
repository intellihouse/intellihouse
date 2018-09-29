package house.intelli.pgp;

import static java.util.Objects.*;
import static house.intelli.core.util.Util.*;
import static house.intelli.pgp.PgpKeyFlag.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import house.intelli.core.collection.ReverseListView;

public class PgpKey implements Serializable {
	private static final long serialVersionUID = 1L;

	public static final PgpKeyId TEST_DUMMY_PGP_KEY_ID = new PgpKeyId(0);

	public static final PgpKey TEST_DUMMY_PGP_KEY = new PgpKey(
			TEST_DUMMY_PGP_KEY_ID,
			new PgpKeyFingerprint(new byte[0]),
			null,
			new Date(), // created
			null, // validTo
			PgpKeyAlgorithm.RSA,
			4096,
			true,
			Collections.<String>emptyList(),
			EnumSet.of(PgpKeyFlag.CAN_AUTHENTICATE, PgpKeyFlag.CAN_CERTIFY, PgpKeyFlag.CAN_SIGN, PgpKeyFlag.CAN_ENCRYPT_COMMS, PgpKeyFlag.CAN_ENCRYPT_STORAGE),
			false,
			false
			);
	static {
		TEST_DUMMY_PGP_KEY.setSubKeys(Collections.<PgpKey>emptyList());
	}

	private final PgpKeyId pgpKeyId;

	private final PgpKeyFingerprint fingerprint;

	private final Date created;

	private final Date validTo;

	private final PgpKeyAlgorithm algorithm;

	private final int strength;

	private final boolean secretKeyAvailable;

	private final List<String> userIds;

	private final Set<PgpKeyFlag> pgpKeyFlags;

	private final boolean revoked;

	private final boolean disabled;

	private final PgpKey masterKey;

	private List<PgpKey> subKeys;

	private static final List<PgpKey> NULL_SUB_KEYS = Collections.unmodifiableList(new ArrayList<PgpKey>(0));

	public PgpKey(
			final PgpKeyId pgpKeyId,
			final PgpKeyFingerprint fingerprint,
			final PgpKey masterKey,
			final Date created,
			final Date validTo,
			final PgpKeyAlgorithm algorithm,
			final int strength,
			final boolean secretKeyAvailable,
			final List<String> userIds,
			final Set<PgpKeyFlag> pgpKeyFlags,
			final boolean revoked,
			final boolean disabled) {
		this.pgpKeyId = requireNonNull(pgpKeyId, "pgpKeyId");
		this.fingerprint = requireNonNull(fingerprint, "fingerprint");
		this.masterKey = masterKey == null ? this : masterKey;
		this.created = requireNonNull(created, "created");
		this.validTo = validTo; // may be null - null means, it does *not* expire.
		this.algorithm = requireNonNull(algorithm, "algorithm");
		this.strength = strength;
		this.secretKeyAvailable = secretKeyAvailable;
		this.userIds = Collections.unmodifiableList(new ArrayList<String>(requireNonNull(userIds, "userIds")));

		final Set<PgpKeyFlag> tmpPgpKeyFlags = EnumSet.noneOf(PgpKeyFlag.class);
		tmpPgpKeyFlags.addAll(requireNonNull(pgpKeyFlags, "pgpKeyFlags"));
		this.pgpKeyFlags = Collections.unmodifiableSet(tmpPgpKeyFlags);

		this.revoked = revoked;
		this.disabled = disabled;
	}

	public PgpKeyId getPgpKeyId() {
		return pgpKeyId;
	}

	public PgpKeyFingerprint getFingerprint() {
		return fingerprint;
	}

	public Date getCreated() {
		return created;
	}

	/**
	 * Gets the date this PGP key expires. The exact timestamp denoted by this date is excluded. It is valid until the
	 * millisecond before this timestamp.
	 *
	 * @return the date this PGP key expires. May be <code>null</code>, which means, it never expires.
	 */
	public Date getValidTo() {
		return validTo;
	}

	/**
	 * A PGP key is valid, if it is neither revoked, nor the given {@code date} after its expiry date, nor
	 * is disabled.
	 *
	 * @param date the date of which to determine the validity.
	 * @return
	 */
	public boolean isValid(Date date) {
		if (isDisabled())
			return false;

		if (isRevoked()) // TODO we should determine, if the revocation happened after the given date! However, we currently *always* use this method with null or now - hence it has very low priority.
			return false;

		if (date == null)
			date = new Date();

		if (date.before(created))
			return false;

		if (validTo == null || date.before(validTo))
			return true;

		return false;
	}

	public PgpKeyAlgorithm getAlgorithm() {
		return algorithm;
	}

	public int getStrength() {
		return strength;
	}

	public boolean isRevoked() {
		return revoked;
	}

	public boolean isDisabled() {
		return disabled;
	}

	/**
	 * Is there a private key available in this key ring, corresponding to the public key?
	 * <p>
	 * Even though it makes no difference for this method, you should note the difference between <i>secret</i> and <i>private</i>:
	 * The <i>private</i> key is the unprotected, decrypted key itself. The <i>secret</i> key, however, is the passphrase-protected
	 * form of the <i>private</i> key. Thus, if you have a secret key available, you also have the private key, if you know the
	 * passphrase.
	 * @return <code>true</code>, if there is a secret key in this key ring. <code>false</code>, if only the public key
	 * is available.
	 */
	public boolean isSecretKeyAvailable() {
		return secretKeyAvailable;
	}

	public List<String> getUserIds() {
		return userIds;
	}

	// TODO support user-attributes - e.g. images!

	public Set<PgpKeyFlag> getPgpKeyFlags() {
		return pgpKeyFlags;
	}

	/**
	 * Gets the master-key of this key. If this key is already the master-key, it returns <code>this</code> instead.
	 * @return the master-key of this key or <code>this</code>, if this is already the master-key.
	 */
	public PgpKey getMasterKey() {
		return requireNonNull(masterKey, "masterKey");
	}

	public void setSubKeys(List<PgpKey> subKeys) {
		if (this.subKeys != null)
			throw new IllegalStateException("this.subKeys already assigned!");

		if (subKeys == null)
		    this.subKeys = NULL_SUB_KEYS;
		else
		    this.subKeys = Collections.unmodifiableList(new ArrayList<PgpKey>(requireNonNull(subKeys, "subKeys")));
	}

	/**
	 * Gets a {@code List} containing first the master-key followed by the sub-keys.
	 * @return a {@code List} with master-key and sub-keys. Never <code>null</code>.
	 */
	public List<PgpKey> getMasterKeyAndSubKeys() {
		final PgpKey mk = getMasterKey();
		final List<PgpKey> subKeys = mk.getSubKeys();
		final List<PgpKey> result = new ArrayList<PgpKey>(subKeys.size() + 1);
		result.add(mk);
		result.addAll(subKeys); // keys are not recursive, i.e. a sub-key does not have subs of their own => no need for recursion
		return Collections.unmodifiableList(result);
	}

	public PgpKey getPgpKeyForEncryptionOrFail() {
		final Date now = new Date();
		// Reversing in order to favour a sub-key over the master-key! ...and probably even the last added (?) sub-key, which would be what we likely want.
		final List<PgpKey> allKeys = new ReverseListView<>(getMasterKeyAndSubKeys());

		int keysSupportingEncryptStorage = 0;
		int keysSupportingEncryptComms = 0;

		PgpKey result = null;
		for (final PgpKey key : allKeys) {
			if (key.getPgpKeyFlags().contains(CAN_ENCRYPT_STORAGE)) {
				++keysSupportingEncryptStorage;
				if (! key.isRevoked() && key.isValid(now))
					result = key;
			}
		}

		if (result == null) {
			for (final PgpKey key : allKeys) {
				if (key.getPgpKeyFlags().contains(CAN_ENCRYPT_COMMS)) {
					++keysSupportingEncryptComms;
					if (! key.isRevoked() && key.isValid(now))
						result = key;
				}
			}
		}

		if (result == null) {
			final PgpKey mk = getMasterKey();
			throw new IllegalStateException(String.format(
					"Neither any sub-key nor the master-key %s are suitable for encryption! There are %s keys with flag 'CAN_ENCRYPT_STORAGE' and %s keys with flag 'CAN_ENCRYPT_COMMS' (all of them revoked or expired).",
					mk.getPgpKeyId(), keysSupportingEncryptStorage, keysSupportingEncryptComms));
		}
		return result;
	}

	public PgpKey getPgpKeyForSignatureOrFail() {
		final Date now = new Date();
		// Reversing in order to favour a sub-key over the master-key! ...and probably even the last added (?) sub-key, which would be what we likely want.
		final List<PgpKey> allKeys = new ReverseListView<>(getMasterKeyAndSubKeys());

		int keysSupportingSign = 0;
		int keysSupportingCertify = 0;

		PgpKey result = null;
		for (final PgpKey key : allKeys) {
			if (key.getPgpKeyFlags().contains(CAN_SIGN)) {
				++keysSupportingSign;
				if (! key.isRevoked() && key.isValid(now))
					result = key;
			}
		}

		if (result == null) {
			// if it can certify, it technically can sign, too, hence we use it rather than throwing an exception.
			for (final PgpKey key : allKeys) {
				if (key.getPgpKeyFlags().contains(CAN_CERTIFY)) {
					++keysSupportingCertify;
					if (! key.isRevoked() && key.isValid(now))
						result = key;
				}
			}
		}

		if (result == null) {
			final PgpKey mk = getMasterKey();
			throw new IllegalStateException(String.format(
					"Neither any sub-key nor the master-key %s are suitable for signing! There are %s keys with flag 'CAN_SIGN' and %s keys with flag 'CAN_CERTIFY' (all of them revoked or expired).",
					mk.getPgpKeyId(), keysSupportingSign, keysSupportingCertify));
		}

		return result;
	}

	public List<PgpKey> getSubKeys() {
		return subKeys == NULL_SUB_KEYS ? null : subKeys;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (pgpKeyId == null ? 0 : pgpKeyId.hashCode());
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final PgpKey other = (PgpKey) obj;
		return equal(this.pgpKeyId, other.pgpKeyId);
	}

	@Override
	public String toString() {
		return String.format("%s[%s, %s]", getClass().getSimpleName(), pgpKeyId, userIds);
	}
}
