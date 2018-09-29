package house.intelli.pgp;

import static java.util.Objects.*;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;


public abstract class AbstractPgpEncoder implements PgpEncoder {

	private InputStream inputStream;
	private OutputStream outputStream;
	private OutputStream signOutputStream;

	private final Set<PgpKey> encryptPgpKeys = new HashSet<PgpKey>(0);
	private PgpKey signPgpKey;
	private String fileName = "";

	private boolean withIntegrityCheck;
	private SymmetricEncryptionAlgorithm symmetricEncryptionAlgorithm = SymmetricEncryptionAlgorithm.TWOFISH;
	private CompressionAlgorithm compressionAlgorithm = CompressionAlgorithm.ZIP;
	private HashAlgorithm hashAlgorithm = HashAlgorithm.SHA256;

	@Override
	public InputStream getInputStream() {
		return inputStream;
	}
	@Override
	public void setInputStream(final InputStream inputStream) {
		this.inputStream = inputStream;
	}
	protected InputStream getInputStreamOrFail() {
		final InputStream inputStream = getInputStream();
		if (inputStream == null)
			throw new IllegalStateException("inputStream == null");

		return inputStream;
	}

	@Override
	public OutputStream getOutputStream() {
		return outputStream;
	}
	@Override
	public void setOutputStream(final OutputStream outputStream) {
		this.outputStream = outputStream;
	}
	protected OutputStream getOutputStreamOrFail() {
		final OutputStream outputStream = getOutputStream();
		if (outputStream == null)
			throw new IllegalStateException("outputStream == null");

		return outputStream;
	}

	@Override
	public OutputStream getSignOutputStream() {
		return signOutputStream;
	}
	@Override
	public void setSignOutputStream(OutputStream signOutputStream) {
		this.signOutputStream = signOutputStream;
	}

	@Override
	public boolean isWithIntegrityCheck() {
		return withIntegrityCheck;
	}
	@Override
	public void setWithIntegrityCheck(final boolean withIntegrityCheck) {
		this.withIntegrityCheck = withIntegrityCheck;
	}

	@Override
	public SymmetricEncryptionAlgorithm getSymmetricEncryptionAlgorithm() {
		return symmetricEncryptionAlgorithm;
	}
	@Override
	public void setSymmetricEncryptionAlgorithm(final SymmetricEncryptionAlgorithm symmetricEncryptionAlgorithm) {
		this.symmetricEncryptionAlgorithm = requireNonNull(symmetricEncryptionAlgorithm, "symmetricEncryptionAlgorithm");
	}

	@Override
	public CompressionAlgorithm getCompressionAlgorithm() {
		return compressionAlgorithm;
	}
	@Override
	public void setCompressionAlgorithm(final CompressionAlgorithm compressionAlgorithm) {
		this.compressionAlgorithm = requireNonNull(compressionAlgorithm, "compressionAlgorithm");
	}

	@Override
	public HashAlgorithm getHashAlgorithm() {
		return hashAlgorithm;
	}
	@Override
	public void setHashAlgorithm(final HashAlgorithm hashAlgorithm) {
		this.hashAlgorithm = requireNonNull(hashAlgorithm, "hashAlgorithm");
	}

	@Override
	public Set<PgpKey> getEncryptPgpKeys() {
		return encryptPgpKeys;
	}

	@Override
	public PgpKey getSignPgpKey() {
		return signPgpKey;
	}
	@Override
	public void setSignPgpKey(final PgpKey signPgpKey) {
		if (signPgpKey != null && !signPgpKey.isSecretKeyAvailable())
			throw new IllegalArgumentException("signPgpKey.privateKeyAvailable == false :: A private key is required for signing! signPgpKey.pgpKeyId=" + signPgpKey.getPgpKeyId() + ", signPgpKey.userIds=" + signPgpKey.getUserIds());

		this.signPgpKey = signPgpKey;
	}

	public String getFileName() {
		return fileName;
	}
	public void setFileName(final String fileName) {
		this.fileName = requireNonNull(fileName, "fileName");
	}

	protected PgpAuthenticationCallback getPgpAuthenticationCallback() {
		final PgpAuthenticationCallback pgpAuthenticationCallback = PgpRegistry.getInstance().getPgpAuthenticationCallback();
		return pgpAuthenticationCallback;
	}

	protected PgpAuthenticationCallback getPgpAuthenticationCallbackOrFail() {
		final PgpAuthenticationCallback pgpAuthenticationCallback = getPgpAuthenticationCallback();
		if (pgpAuthenticationCallback == null)
			throw new IllegalStateException("There is no PgpAuthenticationCallback assigned!");

		return pgpAuthenticationCallback;
	}
}
