package house.intelli.pgp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;

/**
 * Encoder to encrypt or sign according to the OpenPGP standard.
 * @author Marco หงุ่ยตระกูล-Schulze - marco at codewizards dot co
 */
public interface PgpEncoder {

	InputStream getInputStream();
	void setInputStream(InputStream in);

	OutputStream getOutputStream();
	void setOutputStream(OutputStream out);

	OutputStream getSignOutputStream();
	void setSignOutputStream(OutputStream out);

	/**
	 * Gets the keys of the recipients of the encrypted message.
	 * @return the keys with which the data is encrypted. Never <code>null</code>, but maybe empty.
	 */
	Set<PgpKey> getEncryptPgpKeys();

	PgpKey getSignPgpKey();
	void setSignPgpKey(PgpKey pgpKey);

	boolean isWithIntegrityCheck();
	void setWithIntegrityCheck(boolean withIntegrityCheck);

	SymmetricEncryptionAlgorithm getSymmetricEncryptionAlgorithm();
	void setSymmetricEncryptionAlgorithm(SymmetricEncryptionAlgorithm symmetricEncryptionAlgorithm);

	CompressionAlgorithm getCompressionAlgorithm();
	void setCompressionAlgorithm(CompressionAlgorithm compressionAlgorithm);

	HashAlgorithm getHashAlgorithm();
	void setHashAlgorithm(HashAlgorithm hashAlgorithm);

	/**
	 * Encode the data read from {@link #getInputStream() inputStream} and write the result to {@link #getOutputStream() outputStream}.
	 * <p>
	 * If there is at least one entry in {@link #getEncryptPgpKeys() encryptPgpKeys}, the data is encrypted.
	 * <p>
	 * If there is a {@link #getSignPgpKey() signPgpKey}, the data is signed.
	 * <p>
	 * If there is a {@link #getSignOutputStream() signOutputStream} set, the signature is written separately to this stream, i.e.
	 * as a <i>detached</i> signature. If {@code signOutputStream} is <code>null</code> and signing is enabled (i.e. a
	 * {@code signPgpKey} was set), the signature is written inline into the ordinary output-stream.
	 * <p>
	 * <b>Important:</b> It is not (yet) supported to combine a <i>detached</i> signature with encryption. Currently,
	 * a separate {@code signOutputStream} can thus only be used, when signing is the only operation being done.
	 *
	 * @throws IOException if reading or writing fails - which might be caused indirectly by a failed cipher operation.
	 */
	void encode() throws IOException;
}
