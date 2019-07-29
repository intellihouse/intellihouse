package house.intelli.core;

import static java.util.Objects.*;

import java.text.SimpleDateFormat;
import java.util.Date;

public class TimeInterval implements Comparable<TimeInterval> {

	private final Date fromIncl;

	private final Date toExcl;

	private static SimpleDateFormat iso8601DateFormat;

	public TimeInterval(final Date fromIncl, final Date toExcl) {
		this.fromIncl = requireNonNull(fromIncl, "fromIncl");
		this.toExcl = requireNonNull(toExcl, "toExcl");
	}

	public Date getFromIncl() {
		return fromIncl;
	}

	public Date getToExcl() {
		return toExcl;
	}

	public long getLengthMillis() {
		return toExcl.getTime() - fromIncl.getTime();
	}

	@Override
	public String toString() {
		String fromStr;
		String toStr;
		synchronized (TimeInterval.class) {
			if (iso8601DateFormat == null)
				iso8601DateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

			fromStr = iso8601DateFormat.format(fromIncl);
			toStr = iso8601DateFormat.format(toExcl);
		}
		return "TimeInterval[" + fromStr + ',' + toStr + ")";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + fromIncl.hashCode();
		result = prime * result + toExcl.hashCode();
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;

		if (obj == null)
			return false;

		if (getClass() != obj.getClass())
			return false;

		TimeInterval other = (TimeInterval) obj;
		return this.fromIncl.equals(other.fromIncl) && this.toExcl.equals(other.toExcl);
	}

	@Override
	public int compareTo(TimeInterval o) {
		return this.fromIncl.compareTo(o.fromIncl);
	}
}
