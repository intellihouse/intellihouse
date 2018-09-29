package house.intelli.pgp.gnupg;

import static java.util.Objects.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.bouncycastle.bcpg.SymmetricKeyAlgorithmTags;
import org.bouncycastle.openpgp.PGPCompressedData;
import org.bouncycastle.openpgp.PGPEncryptedDataList;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPLiteralData;
import org.bouncycastle.openpgp.PGPObjectFactory;
import org.bouncycastle.openpgp.PGPOnePassSignature;
import org.bouncycastle.openpgp.PGPOnePassSignatureList;
import org.bouncycastle.openpgp.PGPPrivateKey;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyEncryptedData;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.bouncycastle.openpgp.PGPSignature;
import org.bouncycastle.openpgp.PGPSignatureList;
import org.bouncycastle.openpgp.PGPUtil;
import org.bouncycastle.openpgp.operator.PublicKeyDataDecryptorFactory;
import org.bouncycastle.openpgp.operator.bc.BcKeyFingerprintCalculator;
import org.bouncycastle.openpgp.operator.bc.BcPBESecretKeyDecryptorBuilder;
import org.bouncycastle.openpgp.operator.bc.BcPGPContentVerifierBuilderProvider;
import org.bouncycastle.openpgp.operator.bc.BcPGPDigestCalculatorProvider;
import org.bouncycastle.openpgp.operator.bc.BcPublicKeyDataDecryptorFactory;
import org.bouncycastle.util.io.Streams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import house.intelli.core.auth.SignatureException;
import house.intelli.pgp.AbstractPgpDecoder;
import house.intelli.pgp.MissingSigningPgpKeyException;
import house.intelli.pgp.PgpKey;
import house.intelli.pgp.PgpKeyId;

public class BcPgpDecoder extends AbstractPgpDecoder {
	private static final Logger logger = LoggerFactory.getLogger(BcPgpDecoder.class);

	private final BcWithLocalGnuPgPgp pgp;

	public BcPgpDecoder(final BcWithLocalGnuPgPgp pgp) {
		this.pgp = requireNonNull(pgp, "pgp");
	}

	@Override
	public void decode() throws SignatureException, IOException {
		setDecryptPgpKey(null);
		setSignPgpKey(null);
		setPgpSignature(null);
		setSignPgpKeyIds(null);

		InputStream in = getInputStreamOrFail();
		InputStream signIn = getSignInputStream();

		if (signIn == null)
			in = PGPUtil.getDecoderStream(in);

		if (signIn != null) {
			signIn = PGPUtil.getDecoderStream(signIn);
			decodePlainWithDetachedSignature(in, signIn);
		}
		else {
			final PGPObjectFactory pgpF = new PGPObjectFactory(in, new BcKeyFingerprintCalculator());
			PGPEncryptedDataList enc = null;
			PGPCompressedData comp = null;

			Object o = pgpF.nextObject();

			// the first object might be a PGP marker packet
			if (o instanceof PGPEncryptedDataList) // encrypted (+ maybe signed)
				enc = (PGPEncryptedDataList) o;
			else if (o instanceof PGPCompressedData) { // *not* encrypted, only signed
				enc = null;
				comp = (PGPCompressedData) o;
			}
			else {
				// in case the first object was some marker
				o = pgpF.nextObject();

				if (o instanceof PGPEncryptedDataList) // encrypted (+ maybe signed)
					enc = (PGPEncryptedDataList) o;
				else if (o instanceof PGPCompressedData) { // *not* encrypted, only signed
					enc = null;
					comp = (PGPCompressedData) o;
				}
			}

			if (enc != null)
				decodeEncrypted(enc);
			else if (comp != null)
				decodeCompressed(comp);
			else
				throw new IllegalStateException("WTF?!");
		}
	}

	private void decodePlainWithDetachedSignature(final InputStream in, final InputStream signIn) throws SignatureException, IOException {
		requireNonNull(in, "in");
		requireNonNull(signIn, "signIn");

		final PGPObjectFactory pgpF = new PGPObjectFactory(signIn, new BcKeyFingerprintCalculator());

		PGPOnePassSignatureList onePassSignatureList = null;
		PGPSignatureList signatureList = null;

		try {
			Object message = pgpF.nextObject();
			while (message != null) {
				if (message instanceof PGPOnePassSignatureList) {
					onePassSignatureList = (PGPOnePassSignatureList) message;
				} else if (message instanceof PGPSignatureList) {
					signatureList = (PGPSignatureList) message;
				} else
					throw new PGPException("message unknown message type.");

				message = pgpF.nextObject();
			}

			if (onePassSignatureList == null || signatureList == null)
				throw new PGPException("Poor PGP. Signatures not found.");

			verifySignature(onePassSignatureList, signatureList, in, getOutputStreamOrFail());
		} catch (final PGPException x) {
			throw new IOException(x);
		}
	}

	private void decodeCompressed(final PGPCompressedData comp) throws SignatureException, IOException {
		// TODO extract duplicate code and re-use in both, decodeEncrypted(...) and this method!
		try {
			PGPObjectFactory plainFact = new PGPObjectFactory(comp.getDataStream(), new BcKeyFingerprintCalculator());

			Object message = null;

			PGPOnePassSignatureList onePassSignatureList = null;
			PGPSignatureList signatureList = null;
			PGPCompressedData compressedData = null;

			message = plainFact.nextObject();
			final ByteArrayOutputStream actualOutput = new ByteArrayOutputStream();

			while (message != null) {
				if (message instanceof PGPCompressedData) {
					compressedData = (PGPCompressedData) message;
					plainFact = new PGPObjectFactory(compressedData.getDataStream(), new BcKeyFingerprintCalculator());
					message = plainFact.nextObject();
				}

				if (message instanceof PGPLiteralData) {
					// have to read it and keep it somewhere.
					Streams.pipeAll(((PGPLiteralData) message).getInputStream(), actualOutput);
				} else if (message instanceof PGPOnePassSignatureList) {
					onePassSignatureList = (PGPOnePassSignatureList) message;
				} else if (message instanceof PGPSignatureList) {
					signatureList = (PGPSignatureList) message;
				} else {
					throw new PGPException("message unknown message type.");
				}
				message = plainFact.nextObject();
			}

			actualOutput.close();
			final byte[] output = actualOutput.toByteArray();

			if (onePassSignatureList == null || signatureList == null) // if it's not encrypted, it really should be signed!
				throw new PGPException("Poor PGP. Signatures not found.");

			verifySignature(onePassSignatureList, signatureList, new ByteArrayInputStream(output), getOutputStreamOrFail());
		} catch (final PGPException x) {
			throw new IOException(x);
		}
	}

	private void decodeEncrypted(final PGPEncryptedDataList enc) throws SignatureException, IOException {
		try {
			//
			// find the secret key
			//
			final Iterator<?> it = enc.getEncryptedDataObjects();
			PgpKey decryptPgpKey = null;
			PGPPrivateKey sKey = null;
			PGPPublicKeyEncryptedData pbe = null;

			final List<PgpKeyId> encryptPgpKeyIds = new ArrayList<PgpKeyId>();
			char[] passphrase;
			while (sKey == null && it.hasNext()) {
				pbe = (PGPPublicKeyEncryptedData) it.next();
				final PgpKeyId pgpKeyId = new PgpKeyId(pbe.getKeyID());
				encryptPgpKeyIds.add(pgpKeyId);
				final BcPgpKey bcPgpKey = pgp.getBcPgpKey(pgpKeyId);
				if (bcPgpKey != null) {
					PGPSecretKey secretKey = bcPgpKey.getSecretKey();
					if (secretKey != null && ! secretKey.isPrivateKeyEmpty()) {
						if (secretKey.getKeyEncryptionAlgorithm() != SymmetricKeyAlgorithmTags.NULL) {
							passphrase = getPgpAuthenticationCallbackOrFail().getPassphrase(bcPgpKey.getPgpKey());
							if (passphrase == null) // maybe user cancelled - try next
								secretKey = null;
						}
						else
							passphrase = null;

						if (secretKey != null) {
							sKey = secretKey.extractPrivateKey(
									new BcPBESecretKeyDecryptorBuilder(new BcPGPDigestCalculatorProvider()).build(passphrase));

							decryptPgpKey = requireNonNull(bcPgpKey.getPgpKey(), "bcPgpKey.pgpKey");
						}
					}
				}
			}

			logger.debug("decodeEncrypted: encryptingPgpKeyIds={}", encryptPgpKeyIds);

			if (sKey == null) {
				if (! encryptPgpKeyIds.isEmpty())
					throw new IllegalArgumentException(String.format("Data is encrypted for the PGP keys %s, which we do not have a private key for!", encryptPgpKeyIds));

				throw new IllegalArgumentException("message not found.");
			}

			setDecryptPgpKey(decryptPgpKey);

			final PublicKeyDataDecryptorFactory dataDecryptorFactory = new BcPublicKeyDataDecryptorFactory(sKey);

			final InputStream clear = pbe.getDataStream(dataDecryptorFactory);

			PGPObjectFactory plainFact = new PGPObjectFactory(clear, new BcKeyFingerprintCalculator());

			Object message = null;

			PGPOnePassSignatureList onePassSignatureList = null;
			PGPSignatureList signatureList = null;
			PGPCompressedData compressedData = null;

			message = plainFact.nextObject();
			final ByteArrayOutputStream actualOutput = new ByteArrayOutputStream();

			while (message != null) {
				if (message instanceof PGPCompressedData) {
					compressedData = (PGPCompressedData) message;
					plainFact = new PGPObjectFactory(compressedData.getDataStream(), new BcKeyFingerprintCalculator());
					message = plainFact.nextObject();
				}

				if (message instanceof PGPLiteralData) {
					// have to read it and keep it somewhere.
					Streams.pipeAll(((PGPLiteralData) message).getInputStream(), actualOutput);
				} else if (message instanceof PGPOnePassSignatureList) {
					onePassSignatureList = (PGPOnePassSignatureList) message;
				} else if (message instanceof PGPSignatureList) {
					signatureList = (PGPSignatureList) message;
				} else {
					throw new PGPException("message unknown message type.");
				}
				message = plainFact.nextObject();
			}

			actualOutput.close();
			final byte[] output = actualOutput.toByteArray();

			if (onePassSignatureList != null && signatureList != null) // It might be encrypted, but not signed!
				verifySignature(onePassSignatureList, signatureList, new ByteArrayInputStream(output), getOutputStreamOrFail());

			if (getPgpSignature() == null) // either it had no signature, or we don't have the signing key (and failOnMissingSignPgpKey is false).
				getOutputStreamOrFail().write(output);

			if (pbe.isIntegrityProtected() && !pbe.verify())
				throw new PGPException("Data is integrity protected but integrity is lost.");
		} catch (final PGPException x) {
			throw new IOException(x);
		}
	}

	private void verifySignature(final PGPOnePassSignatureList onePassSignatureList, final PGPSignatureList signatureList, final InputStream signedDataIn, final OutputStream signedDataOut) throws SignatureException, IOException {
		requireNonNull(onePassSignatureList, "onePassSignatureList");
		requireNonNull(signatureList, "signatureList");
		requireNonNull(signedDataIn, "signedDataIn");

		setSignPgpKey(null);
		setPgpSignature(null);
		setSignPgpKeyIds(null);

		if (onePassSignatureList.size() == 0)
			return; // there is no signature

		final Set<PgpKeyId> pgpKeyIds = new HashSet<>();
		try {
			PGPPublicKey publicKey = null;
			for (int i = 0; i < onePassSignatureList.size(); i++) {
				final PGPOnePassSignature ops = onePassSignatureList.get(i);
				pgpKeyIds.add(new PgpKeyId(ops.getKeyID()));
				if (getPgpSignature() != null)
					continue;

				final BcPgpKey bcPgpKey = pgp.getBcPgpKey(new PgpKeyId(ops.getKeyID()));
				if (bcPgpKey != null) {
					publicKey = bcPgpKey.getPublicKey();
					ops.init(new BcPGPContentVerifierBuilderProvider(), publicKey);

					final byte[] buf = new byte[64 * 1024];
					int bytesRead;
					while ((bytesRead = signedDataIn.read(buf)) > 0) {
						ops.update(buf, 0, bytesRead);

						if (signedDataOut != null)
							signedDataOut.write(buf, 0, bytesRead);
					}
					final PGPSignature signature = signatureList.get(i);
					if (ops.verify(signature)) {
						setSignPgpKey(bcPgpKey.getPgpKey());
						setPgpSignature(pgp.createPgpSignature(signature));
					} else
						throw new SignatureException("Signature verification failed!");
				}
			}
		} catch (final PGPException x) {
			throw new IOException(x);
		}
		setSignPgpKeyIds(pgpKeyIds);

		logger.debug("verifySignature: signingPgpKeyIds={}", pgpKeyIds);

		if (getPgpSignature() == null && isFailOnMissingSignPgpKey())
			throw new MissingSigningPgpKeyException(pgpKeyIds,
					"The data was signed using the following PGP-keys, of which none could be found in the local key-ring: " + pgpKeyIds);
	}
}
