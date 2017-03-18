package house.intelli.pgp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.SignatureException;
import java.util.Set;

public interface PgpDecoder {

	InputStream getInputStream();
	void setInputStream(InputStream in);

	OutputStream getOutputStream();
	void setOutputStream(OutputStream out);

	/**
	 * Input of a detached signature.
	 * @return input of a detached signature, or <code>null</code>.
	 */
	InputStream getSignInputStream();
	void setSignInputStream(InputStream in);

	void decode() throws SignatureException, IOException;

	/**
	 * Gets the key that was used to decrypt the data in the last {@link #decode()} invocation.
	 * <p>
	 * There might be multiple encryption keys used - this is just one of them (the first one available
	 * in our local PGP key ring).
	 * @return the key that decrypted the data in the last call of {@link #decode()}. <code>null</code>, before
	 * {@code decode()} was invoked.
	 */
	PgpKey getDecryptPgpKey();

//	/**
//	 * Gets the key that signed the data, if it was signed.
//	 * <p>
//	 * This property is <code>null</code>, before {@link #decode()} was called. It is only assigned to a non-<code>null</code>
//	 * value, if the data was signed and the signature is correct (not broken).
//	 * @return the key that signed the data or <code>null</code>.
//	 */
//	PgpKey getSignPgpKey();

	/**
	 * Gets the signature, if it was signed.
	 * <p>
	 * This property is <code>null</code>, before {@link #decode()} was called. It is only assigned to a non-<code>null</code>
	 * value, if the data was signed and the signature is correct (not broken).
	 * @return the signature or <code>null</code>.
	 */
	PgpSignature getPgpSignature();

	/**
	 * Gets all the PGP key IDs with which the data was signed.
	 * <p>
	 * In contrast to {@link #getPgpSignature()}, this might be populated, even if none of the signing keys is
	 * available in our key ring. By default, decoding fails, if no signature can be verified, but
	 * {@link #setFailOnMissingSignPgpKey(boolean)} can be invoked with <code>false</code> before calling {@link #decode()}.
	 * <p>
	 * Please note, that usually only one of the signatures is
	 * verified (and in most cases, there's only one signature, anyway).
	 * @return the key IDs having been used for signing. Never <code>null</code>, but maybe empty.
	 */
	Set<PgpKeyId> getSignPgpKeyIds();

	/**
	 * Whether decoding fails, if no signature can be verified, because the PGP keys are missing in the local key-ring.
	 * <p>
	 * This is <code>true</code> by default.
	 * <p>
	 * If <code>false</code>, signed and encrypted data still can be decrypted, even if the signing key is not known. In this
	 * case, {@link #getPgpSignature()} will still return <code>null</code>, but the IDs of the PGP keys used for signing can
	 * be queried using {@link #getSignPgpKeyIds()}.
	 * @return whether decoding should fail, if all PGP keys used for signing are unknown.
	 */
	boolean isFailOnMissingSignPgpKey();

	void setFailOnMissingSignPgpKey(boolean failOnMissingSignPgpKey);

}
