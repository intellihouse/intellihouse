package house.intelli.pgp;

public enum PgpOwnerTrust {

	UNSPECIFIED,
	UNKNOWN,
	NEVER,
	MARGINAL,
	FULL,
	ULTIMATE;

	@Override
	public String toString() {
		return Messages.getString(String.format("PgpOwnerTrust[%s].string", name())); //$NON-NLS-1$
	}

	public String toShortString() {
		return Messages.getString(String.format("PgpOwnerTrust[%s].shortString", name())); //$NON-NLS-1$
	}

	public String getAnswer() {
		return Messages.getString(String.format("PgpOwnerTrust[%s].answer", name())); //$NON-NLS-1$
	}

	public String getDescription() {
		return Messages.getString(String.format("PgpOwnerTrust[%s].description", name())); //$NON-NLS-1$
	}
}
