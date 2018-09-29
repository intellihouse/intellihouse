package house.intelli.pgp;

import static java.util.Objects.*;

import java.util.Set;

import house.intelli.core.auth.SignatureException;

public class MissingSigningPgpKeyException extends SignatureException {

	private static final long serialVersionUID = 1L;

	private final Set<PgpKeyId> missingPgpKeyIds;

	public MissingSigningPgpKeyException(Set<PgpKeyId> missingPgpKeyIds, String message) {
		super(message);
		this.missingPgpKeyIds = requireNonNull(missingPgpKeyIds, "missingPgpKeyIds");
	}

	public Set<PgpKeyId> getMissingPgpKeyIds() {
		return missingPgpKeyIds;
	}
}
