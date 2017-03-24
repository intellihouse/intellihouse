package house.intelli.pgp.rpc;

import static house.intelli.core.util.AssertUtil.*;

import java.security.SecureRandom;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import house.intelli.core.Uid;
import house.intelli.core.rpc.HostId;

public class Session {

	protected static final SecureRandom random = new SecureRandom();

	/**
	 * Maximum age of a session. It must be replaced by a new session at latest after this time, but it may
	 * be replaced already earlier.
	 */
	public static final long SESSION_MAX_AGE = 24L * 3600L * 1000L;
	public static final int SESSION_KEY_SIZE = 256; // bits

	private final Uid sessionId;
	private final SessionHostIdPair sessionHostIdPair;
	private final byte[] sessionKey;
	private final Date created;

	private final Set<HostId> confirmedByHostIds = Collections.synchronizedSet(new HashSet<>(2));

	private static byte[] createSessionKey() {
		final byte[] sessionKey = new byte[SESSION_KEY_SIZE / 8];
		random.nextBytes(sessionKey);
		return sessionKey;
	}

	public Session(final SessionHostIdPair sessionHostIdPair) {
		this(new Uid(), sessionHostIdPair, createSessionKey(), new Date());
	}

	public Session(final Uid sessionId, final SessionHostIdPair sessionHostIdPair, final byte[] sessionKey, Date created) {
		this.sessionHostIdPair = assertNotNull(sessionHostIdPair, "sessionHostIdPair");
		this.sessionId = assertNotNull(sessionId, "sessionId");
		this.sessionKey = assertNotNull(sessionKey, "sessionKey");
		if (sessionKey.length < 128 / 8)
			throw new IllegalArgumentException("sessionKey too short!");

		this.created = assertNotNull(created, "created");
//		expired = new Date(created.getTime() + SESSION_MAX_AGE);
	}

	public Set<HostId> getConfirmedByHostIds() {
		return Collections.unmodifiableSet(confirmedByHostIds);
	}

	public void confirmByHostId(final HostId hostId) {
		assertNotNull(hostId, "hostId");
		if (confirmedByHostIds.contains(hostId))
			return;

		if (! sessionHostIdPair.getHostIds().contains(hostId))
			throw new IllegalStateException(String.format("Session %s is owned by %s! Cannot confirm by %s!",
					sessionId, sessionHostIdPair.getHostIds(), hostId));

		confirmedByHostIds.add(hostId);
	}

	public boolean isConfirmedCompletely() {
		return confirmedByHostIds.equals(sessionHostIdPair.getHostIds());
	}

//	public boolean isExpired() {
//		return expired.getTime() <= System.currentTimeMillis();
//	}

	public Uid getSessionId() {
		return sessionId;
	}
	public SessionHostIdPair getSessionHostIdPair() {
		return sessionHostIdPair;
	}
	public byte[] getSessionKey() {
		return sessionKey;
	}
	public Date getCreated() {
		return created;
	}
//	public Date getExpired() {
//		return expired;
//	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + '[' + toString_getProperties() + ']';
	}

	protected String toString_getProperties() {
		return "sessionId=" + sessionId
				+ ", sessionHostIdPair=" + sessionHostIdPair
				+ ", created=" + created
				+ ", confirmedByHostIds=" + confirmedByHostIds;
	}

	public boolean isExpired() {
		return System.currentTimeMillis() - created.getTime() > SESSION_MAX_AGE;
	}
}
