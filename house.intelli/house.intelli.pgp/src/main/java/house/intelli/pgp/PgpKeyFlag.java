package house.intelli.pgp;

/**
 * Flags indicating the intended usage of a PGP key.
 * <p>
 * These flags are mostly standardised in OpenPGP (see {@link org.bouncycastle.openpgp.PGPKeyFlags}).
 * However, at least one flag {@link #CAN_AUTHENTICATE} is missing there. It is declared in
 * {@link org.bouncycastle.bcpg.sig.KeyFlags}, though.
 * @author Marco หงุ่ยตระกูล-Schulze - marco at codewizards dot co
 */
public enum PgpKeyFlag {

	/**
	 * The PGP key may be used to certify other keys.
	 */
	CAN_CERTIFY,

	/**
	 * The PGP key may be used to sign data.
	 */
	CAN_SIGN,

	CAN_AUTHENTICATE,


	/**
	 * The PGP key may be used to encrypt communications.
	 */
	CAN_ENCRYPT_COMMS,

	/**
	 * The PGP key may be used to encrypt storage.
	 */
	CAN_ENCRYPT_STORAGE,


	/**
	 * The private component of the PGP key may have been split by a secret-sharing mechanism.
	 */
	MAYBE_SPLIT,

	/**
	 * The private component of the PGP key may be in the possession of more than one person.
	 */
	MAYBE_SHARED
}
