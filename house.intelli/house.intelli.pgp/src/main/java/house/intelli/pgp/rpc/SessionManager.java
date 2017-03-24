package house.intelli.pgp.rpc;

import static house.intelli.core.util.AssertUtil.*;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import house.intelli.core.Uid;

public class SessionManager {

	private static final Logger logger = LoggerFactory.getLogger(SessionManager.class);

	private static final SessionManager instance = new SessionManager();

	private final Map<Uid, Session> sessionId2Session = new HashMap<>();
	private final Map<SessionHostIdPair, Session> sessionHostIdPair2Session = new HashMap<>();
	// TODO evict!

	public static SessionManager getInstance() {
		return instance;
	}

	protected SessionManager() {
	}

	public synchronized Session getSession(final Uid sessionId) {
		assertNotNull(sessionId, "sessionId");
		return sessionId2Session.get(sessionId);
	}

	public synchronized Session getOrCreateSession(final SessionHostIdPair sessionHostIdPair) {
		Session session = getSession(sessionHostIdPair);
		if (session == null || session.isExpired()) {
			session = new Session(sessionHostIdPair);
			logger.debug("getOrCreateSession: created: {}", session);
			putSession(session);
		}
		else
			logger.debug("getOrCreateSession: found: {}", session);

		return session;
	}

	public synchronized Session getSession(final SessionHostIdPair sessionHostIdPair) {
		assertNotNull(sessionHostIdPair, "sessionHostIdPair");
		return sessionHostIdPair2Session.get(sessionHostIdPair);
	}

	public synchronized void putSession(final Session session) {
		assertNotNull(session, "session");
		sessionId2Session.put(session.getSessionId(), session);
		sessionHostIdPair2Session.put(session.getSessionHostIdPair(), session);
		logger.debug("putSession: {}", session);
	}

	public Session getSessionOrFail(final Uid sessionId) {
		assertNotNull(sessionId, "sessionId");
		final Session session = getSession(sessionId);
		if (session == null)
			throw new IllegalArgumentException("There is no session with sessionId=" + sessionId);

		return session;
	}

}
