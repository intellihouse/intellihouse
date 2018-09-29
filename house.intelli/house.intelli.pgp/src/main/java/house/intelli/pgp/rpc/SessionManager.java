package house.intelli.pgp.rpc;

import static java.util.Objects.*;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import house.intelli.core.Uid;

public class SessionManager {

	private static final Logger logger = LoggerFactory.getLogger(SessionManager.class);

	private static final long EVICT_PERIOD = 30L * 60L * 1000L;
	private static final long EVICT_ADDITIONAL_AGE = 15L * 60L * 1000L;

	private static final SessionManager instance = new SessionManager();

	private final Map<Uid, Session> sessionId2Session = new HashMap<>();
	private final Map<SessionHostIdPair, Session> sessionHostIdPair2Session = new HashMap<>();

	private final Timer evictTimer = new Timer("SessionManager.evictTimer", true);
	private final TimerTask evictTimerTask = new TimerTask() {
		@Override
		public void run() {
			try {
				evict();
			} catch (Throwable x) {
				logger.error("evictTimerTask.run: " + x + ' ', x);
			}
		}
	};

	public static SessionManager getInstance() {
		return instance;
	}

	protected SessionManager() {
		evictTimer.schedule(evictTimerTask, EVICT_PERIOD, EVICT_PERIOD);
	}

	public synchronized Session getSession(final Uid sessionId) {
		requireNonNull(sessionId, "sessionId");
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
		requireNonNull(sessionHostIdPair, "sessionHostIdPair");
		return sessionHostIdPair2Session.get(sessionHostIdPair);
	}

	public synchronized void putSession(final Session session) {
		requireNonNull(session, "session");
		sessionId2Session.put(session.getSessionId(), session);
		sessionHostIdPair2Session.put(session.getSessionHostIdPair(), session);
		logger.debug("putSession: {}", session);
	}

	public synchronized boolean removeSession(final Uid sessionId) {
		requireNonNull(sessionId, "sessionId");
		final Session session = sessionId2Session.remove(sessionId);
		if (session != null) {
			sessionHostIdPair2Session.remove(session.getSessionHostIdPair());
			return true;
		}
		return false;
	}

	private static final String SESSION_NOT_FOUND_EXCEPTION_MESSAGE_PREFIX = "There is no session with sessionId=";

	public Session getSessionOrFail(final Uid sessionId) throws SessionNotFoundException {
		requireNonNull(sessionId, "sessionId");
		final Session session = getSession(sessionId);
		if (session == null)
			throw new SessionNotFoundException(SESSION_NOT_FOUND_EXCEPTION_MESSAGE_PREFIX + sessionId);

		return session;
	}

	public static Uid getSessionIdFromSessionNotFoundExceptionMessage(final String message) {
		requireNonNull(message, "message");
		if (! message.startsWith(SESSION_NOT_FOUND_EXCEPTION_MESSAGE_PREFIX))
			throw new IllegalArgumentException(String.format("message does not start with prefix '%s'!", SESSION_NOT_FOUND_EXCEPTION_MESSAGE_PREFIX));

		String sessionIdString = message.substring(SESSION_NOT_FOUND_EXCEPTION_MESSAGE_PREFIX.length());
		return new Uid(sessionIdString);
	}

	protected synchronized void evict() {
		final Date oldestSessionCreatedNotYetEvicted = new Date(System.currentTimeMillis() - Session.SESSION_MAX_AGE - EVICT_ADDITIONAL_AGE);
		final Set<Uid> evictedSessionIds = new HashSet<>();
		for (final Iterator<Map.Entry<Uid, Session>> it = sessionId2Session.entrySet().iterator(); it.hasNext(); ) {
			final Map.Entry<Uid, Session> me = it.next();
			final Session session = me.getValue();
			if (session.getCreated().before(oldestSessionCreatedNotYetEvicted)) {
				evictedSessionIds.add(session.getSessionId());
				it.remove();
				sessionHostIdPair2Session.remove(session.getSessionHostIdPair());
			}
		}
		logger.info("evict: evictedSessionIds={}", evictedSessionIds);
	}
}
