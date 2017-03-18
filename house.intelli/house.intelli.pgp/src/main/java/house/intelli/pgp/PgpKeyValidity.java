package house.intelli.pgp;

import static house.intelli.core.util.AssertUtil.*;

public enum PgpKeyValidity {

	EXPIRED,
	REVOKED,
	NOT_TRUSTED,
	DISABLED,

	MARGINAL,
	FULL,
	ULTIMATE;

	@Override
	public String toString() {
		return Messages.getString(String.format("PgpKeyValidity[%s].string", name())); //$NON-NLS-1$
	}

	public String toShortString() {
		return Messages.getString(String.format("PgpKeyValidity[%s].shortString", name())); //$NON-NLS-1$
	}

	public String getDescription(final PgpKeyId pgpKeyId) {
		assertNotNull(pgpKeyId, "pgpKeyId");
		final String s = Messages.getString(String.format("PgpKeyValidity[%s].description", name())); //$NON-NLS-1$
		return String.format(s, pgpKeyId.toHumanString());
	}
}
