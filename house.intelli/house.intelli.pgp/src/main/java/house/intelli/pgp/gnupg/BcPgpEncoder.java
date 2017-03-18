package house.intelli.pgp.gnupg;

import static house.intelli.core.util.AssertUtil.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.SecureRandom;
import java.util.Date;
import java.util.Iterator;

import org.bouncycastle.bcpg.SymmetricKeyAlgorithmTags;
import org.bouncycastle.openpgp.PGPCompressedDataGenerator;
import org.bouncycastle.openpgp.PGPEncryptedDataGenerator;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPLiteralData;
import org.bouncycastle.openpgp.PGPLiteralDataGenerator;
import org.bouncycastle.openpgp.PGPPrivateKey;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.bouncycastle.openpgp.PGPSignature;
import org.bouncycastle.openpgp.PGPSignatureGenerator;
import org.bouncycastle.openpgp.PGPSignatureSubpacketGenerator;
import org.bouncycastle.openpgp.operator.bc.BcPBESecretKeyDecryptorBuilder;
import org.bouncycastle.openpgp.operator.bc.BcPGPContentSignerBuilder;
import org.bouncycastle.openpgp.operator.bc.BcPGPDataEncryptorBuilder;
import org.bouncycastle.openpgp.operator.bc.BcPGPDigestCalculatorProvider;
import org.bouncycastle.openpgp.operator.bc.BcPublicKeyKeyEncryptionMethodGenerator;

import house.intelli.pgp.AbstractPgpEncoder;
import house.intelli.pgp.PgpAuthenticationCallback;
import house.intelli.pgp.PgpKey;

public class BcPgpEncoder extends AbstractPgpEncoder {

	private final BcWithLocalGnuPgPgp pgp;

	public BcPgpEncoder(final BcWithLocalGnuPgPgp pgp) {
		this.pgp = assertNotNull(pgp, "pgp");
	}

	@Override
	public void encode() throws IOException {
		final int BUFFER_SIZE = 1024 * 32;

		final InputStream in = getInputStreamOrFail();
		final OutputStream signOut = getSignOutputStream();
		OutputStream out = getOutputStreamOrFail();
		try {
			final PGPEncryptedDataGenerator edGenerator = getEncryptPgpKeys().isEmpty() ? null : createEncryptedDataGenerator();
			try {
				for (final PgpKey encryptPgpKey : getEncryptPgpKeys()) {
					if (signOut != null)
						throw new IllegalStateException("Signature cannot be detached when encryption is used!");

					final PgpKey actualEncryptPgpKey = encryptPgpKey.getPgpKeyForEncryptionOrFail();
					final BcPgpKey bcPgpKey = pgp.getBcPgpKey(actualEncryptPgpKey);
					edGenerator.addMethod(new BcPublicKeyKeyEncryptionMethodGenerator(bcPgpKey.getPublicKey()));
				}

				final OutputStream encryptedOut = edGenerator == null ? null : edGenerator.open(out, new byte[BUFFER_SIZE]);
				try {
					if (encryptedOut != null)
						out = encryptedOut;

					// If we generate a *detached* signature and do *not* encrypt, we generate the same output as we get as input
					// (we might actually allow to omit the output-stream, then).
					final boolean unmodifiedOutput = encryptedOut == null && signOut != null;

					final PGPCompressedDataGenerator cdGenerator = unmodifiedOutput ? null : createCompressedDataGenerator();
					try {
						final OutputStream compressedOut = cdGenerator == null ? null : cdGenerator.open(out);
						try {
							if (compressedOut != null)
								out = compressedOut;

							final PGPSignatureGenerator signatureGenerator = getSignPgpKey() == null ? null : createSignatureGenerator();

							if (signatureGenerator != null)
								signatureGenerator.generateOnePassVersion(false).encode(signOut != null ? signOut : out);

							final PGPLiteralDataGenerator ldGenerator = unmodifiedOutput ? null : new PGPLiteralDataGenerator();
							try {
								try (final OutputStream lOut = ldGenerator == null ? null : ldGenerator.open(out, PGPLiteralData.BINARY, getFileName(), new Date(), new byte[BUFFER_SIZE]);) {
									int bytesRead;
									final byte[] buf = new byte[BUFFER_SIZE];
									while ((bytesRead = in.read(buf, 0, buf.length)) >= 0) {
										if (bytesRead > 0) {
											if (lOut != null)
												lOut.write(buf, 0, bytesRead);
											else
												out.write(buf, 0, bytesRead);

											if (signatureGenerator != null)
												signatureGenerator.update(buf, 0, bytesRead);
										}
									}
								}
							} finally {
								if (ldGenerator != null)
									ldGenerator.close();
							}

							if (signatureGenerator != null)
								signatureGenerator.generate().encode(signOut != null ? signOut : out);
						} finally {
							if (compressedOut != null)
								compressedOut.close();
						}
					} finally {
						if (cdGenerator != null)
							cdGenerator.close();
					}
				} finally {
					if (encryptedOut != null)
						encryptedOut.close();
				}
			} finally {
				if (edGenerator != null)
					edGenerator.close();
			}
		} catch (final PGPException x) {
			throw new IOException(x);
		}
	}

	private PGPSignatureGenerator createSignatureGenerator() throws PGPException {
		final PgpKey signPgpKey = assertNotNull(getSignPgpKey(), "signPgpKey");
		final PgpKey actualSignPgpKey = signPgpKey.getPgpKeyForSignatureOrFail();

		final PGPSecretKey signSecretKey = getPgpSecretKeyOrFail(actualSignPgpKey);

		final char[] signPassphrase = getPassphrase(actualSignPgpKey);
		final PGPPrivateKey pgpPrivKey = signSecretKey.extractPrivateKey(
				new BcPBESecretKeyDecryptorBuilder(new BcPGPDigestCalculatorProvider()).build(signPassphrase));

		final PGPSignatureGenerator signatureGenerator;
		signatureGenerator = new PGPSignatureGenerator(new BcPGPContentSignerBuilder(
				signSecretKey.getPublicKey().getAlgorithm(), getHashAlgorithm().getHashAlgorithmTag()));

		signatureGenerator.init(PGPSignature.BINARY_DOCUMENT, pgpPrivKey);
		final Iterator<?> it = signSecretKey.getPublicKey().getUserIDs();
		if (it.hasNext()) {
			final PGPSignatureSubpacketGenerator spGen = new PGPSignatureSubpacketGenerator();
			spGen.setSignerUserID(false, (String) it.next());
			signatureGenerator.setHashedSubpackets(spGen.generate());
		}
		return signatureGenerator;
	}

	private PGPEncryptedDataGenerator createEncryptedDataGenerator() {
		return new PGPEncryptedDataGenerator(
				new BcPGPDataEncryptorBuilder(getSymmetricEncryptionAlgorithm().getSymmetricKeyAlgorithmTag())
				.setWithIntegrityPacket(isWithIntegrityCheck())
				.setSecureRandom(new SecureRandom()));
	}

	private PGPCompressedDataGenerator createCompressedDataGenerator() {
		return new PGPCompressedDataGenerator(getCompressionAlgorithm().getCompressionAlgorithmTag());
	}

	private char[] getPassphrase(final PgpKey pgpKey) {
		final PGPSecretKey secretKey = getPgpSecretKeyOrFail(pgpKey);
		if (secretKey.getKeyEncryptionAlgorithm() != SymmetricKeyAlgorithmTags.NULL) {
			final PgpAuthenticationCallback callback = getPgpAuthenticationCallbackOrFail();
			return callback.getPassphrase(pgpKey);
		}
		return null;
	}

	private PGPSecretKey getPgpSecretKeyOrFail(final PgpKey pgpKey) {
		assertNotNull(pgpKey, "pgpKey");
		final PGPSecretKey secretKey = pgp.getBcPgpKeyOrFail(pgpKey).getSecretKey();
		if (secretKey == null)
			throw new IllegalStateException(String.format(
					"The PGP key %s does not have a secret key attached (it is a public key only)!",
					pgpKey.getPgpKeyId()));

		return secretKey;
	}
}
