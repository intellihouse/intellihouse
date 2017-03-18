package house.intelli.pgp;

import static house.intelli.core.util.AssertUtil.*;

import java.util.Set;

import house.intelli.core.auth.SignatureException;

public class MissingSigningPgpKeyException extends SignatureException {

	private static final long serialVersionUID = 1L;

	private final Set<PgpKeyId> missingPgpKeyIds;

	public MissingSigningPgpKeyException(Set<PgpKeyId> missingPgpKeyIds, String message) {
		super(message);
		this.missingPgpKeyIds = assertNotNull(missingPgpKeyIds, "missingPgpKeyIds");
	}

	public Set<PgpKeyId> getMissingPgpKeyIds() {
		return missingPgpKeyIds;
	}
}
