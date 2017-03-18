package house.intelli.pgp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.StringTokenizer;

public class PgpKeyIdList extends ArrayList<PgpKeyId> {
	private static final long serialVersionUID = 1L;

	public PgpKeyIdList() {
	}

	public PgpKeyIdList(int initialCapacity) {
		super(initialCapacity);
	}

	public PgpKeyIdList(Collection<? extends PgpKeyId> c) {
		super(c);
	}

	public PgpKeyIdList(final String pgpKeyIdsString) {
		if (pgpKeyIdsString == null)
			return;

		final StringTokenizer st = new StringTokenizer(pgpKeyIdsString, ", \t", false);
		while (st.hasMoreTokens()) {
			final String token = st.nextToken();
			if (!token.isEmpty())
				this.add(new PgpKeyId(token));
		}
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		for (final PgpKeyId pgpKeyId : this) {
			if (sb.length() > 0)
				sb.append(',');

			sb.append(pgpKeyId);
		}
		return sb.toString();
	}
}
