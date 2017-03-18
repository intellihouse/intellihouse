package house.intelli.pgp;

public interface PgpAuthenticationCallback {

	/**
	 * Gets the passphrase to decrypt the private key of the given {@code pgpKey}.
	 * @param pgpKey the key-descriptor whose private key is to be decrypted.
	 * @return the passphrase to decrypt.
	 */
	char[] getPassphrase(PgpKey pgpKey);

}
