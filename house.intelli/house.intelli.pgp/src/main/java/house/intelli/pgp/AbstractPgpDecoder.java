package house.intelli.pgp;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Set;

public abstract class AbstractPgpDecoder implements PgpDecoder {

	private InputStream inputStream;
	private OutputStream outputStream;
	private InputStream signInputStream;
	private PgpKey decryptPgpKey;
	private PgpKey signPgpKey;
	private Set<PgpKeyId> signPgpKeyIds = Collections.emptySet();
	private PgpSignature pgpSignature;
	private boolean failOnMissingSignPgpKey = true;

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
	public InputStream getSignInputStream() {
		return signInputStream;
	}
	@Override
	public void setSignInputStream(InputStream signInputStream) {
		this.signInputStream = signInputStream;
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

	@Override
	public PgpKey getDecryptPgpKey() {
		return decryptPgpKey;
	}
	protected void setDecryptPgpKey(PgpKey decryptPgpKey) {
		this.decryptPgpKey = decryptPgpKey;
	}

	@Override
	public Set<PgpKeyId> getSignPgpKeyIds() {
		return signPgpKeyIds;
	}
	protected void setSignPgpKeyIds(final Set<PgpKeyId> signPgpKeyIds) {
		this.signPgpKeyIds = signPgpKeyIds == null ? Collections.<PgpKeyId>emptySet() : Collections.unmodifiableSet(signPgpKeyIds);
	}

	public PgpKey getSignPgpKey() {
		return signPgpKey;
	}
	protected void setSignPgpKey(final PgpKey signPgpKey) {
		this.signPgpKey = signPgpKey;
	}

	@Override
	public boolean isFailOnMissingSignPgpKey() {
		return failOnMissingSignPgpKey;
	}
	@Override
	public void setFailOnMissingSignPgpKey(boolean failOnMissingSignPgpKey) {
		this.failOnMissingSignPgpKey = failOnMissingSignPgpKey;
	}

	@Override
	public PgpSignature getPgpSignature() {
		return pgpSignature;
	}
	public void setPgpSignature(PgpSignature pgpSignature) {
		this.pgpSignature = pgpSignature;
	}
}
