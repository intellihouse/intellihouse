package house.intelli.pgp;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public enum PgpSignatureType {
	BINARY_DOCUMENT(-1),
    CANONICAL_TEXT_DOCUMENT(-1),
    STAND_ALONE(-1),

    DEFAULT_CERTIFICATION(10),
    NO_CERTIFICATION(20),
    CASUAL_CERTIFICATION(30),
    POSITIVE_CERTIFICATION(40),

    SUBKEY_BINDING(-1),
    PRIMARYKEY_BINDING(-1),
    DIRECT_KEY(-1),
    KEY_REVOCATION(-1),
    SUBKEY_REVOCATION(-1),
    CERTIFICATION_REVOCATION(-1),
    TIMESTAMP(-1);

	private final int trustLevel;

	public static final Set<PgpSignatureType> CERTIFICATIONS = Collections.unmodifiableSet(EnumSet.of(
			DEFAULT_CERTIFICATION,
			NO_CERTIFICATION,
			CASUAL_CERTIFICATION,
			POSITIVE_CERTIFICATION));

	private PgpSignatureType(final int trustLevel) {
		this.trustLevel = trustLevel;
	}

	public int getTrustLevel() {
		return trustLevel;
	}

	public boolean isCertification() {
		return CERTIFICATIONS.contains(this);
	}

	@Override
	public String toString() {
		return Messages.getString(String.format("PgpSignatureType[%s].string", name())); //$NON-NLS-1$
	}

	public String toShortString() {
		return Messages.getString(String.format("PgpSignatureType[%s].shortString", name())); //$NON-NLS-1$
	}

	public String getAnswer() {
		return Messages.getString(String.format("PgpSignatureType[%s].answer", name())); //$NON-NLS-1$
	}

	public String getDescription() {
		return Messages.getString(String.format("PgpSignatureType[%s].description", name())); //$NON-NLS-1$
	}
}
