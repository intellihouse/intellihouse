package house.intelli.pgp.rpc;

import static java.util.Objects.*;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import house.intelli.core.rpc.HostId;

public class SessionHostIdPair {

	private final HostId hostId0;
	private final HostId hostId1;

	private transient Set<HostId> hostIds;

	public final Set<HostId> getHostIds() {
		if (hostIds == null)
			hostIds = new LinkedHashSet<>(Arrays.asList(hostId0, hostId1));

		return hostIds;
	}

	public SessionHostIdPair(final HostId ... hostIds) {
		if (requireNonNull(hostIds, "hostIds").length != 2)
			throw new IllegalArgumentException("hostIdslength != 2");

		requireNonNull(hostIds[0], "hostIds[0]");
		requireNonNull(hostIds[1], "hostIds[1]");

		if (hostIds[0].compareTo(hostIds[1]) < 0) {
			this.hostId0 = hostIds[0];
			this.hostId1 = hostIds[1];
		}
		else {
			this.hostId0 = hostIds[1];
			this.hostId1 = hostIds[0];
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + hostId0.hashCode();
		result = prime * result + hostId1.hashCode();
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

		SessionHostIdPair other = (SessionHostIdPair) obj;
		return this.hostId0.equals(other.hostId0)
				&& this.hostId1.equals(other.hostId1);
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + '[' + toString_getProperties() + ']';
	}

	protected String toString_getProperties() {
		return "hostId0=" + hostId0 + ", hostId1=" + hostId1;
	}
}