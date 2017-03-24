package house.intelli.pgp.rpc;

import java.util.Date;
import java.util.Iterator;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import house.intelli.core.Uid;
import house.intelli.core.rpc.HostId;

@XmlRootElement
public class SessionRequest {

	private Uid sessionId;
	private HostId hostId0;
	private HostId hostId1;
	private byte[] sessionKey;
	private Date created;

	public SessionRequest() {
	}

	public SessionRequest(Session session) {
		this.sessionId = session.getSessionId();
		Iterator<HostId> hostIdsIterator = session.getSessionHostIdPair().getHostIds().iterator();
		this.hostId0 = hostIdsIterator.next();
		this.hostId1 = hostIdsIterator.hasNext() ? hostIdsIterator.next() : this.hostId0;
		if (hostIdsIterator.hasNext())
			throw new IllegalStateException("sessionHostIdPair contains more than 2 hostIds: " + session.getSessionHostIdPair().getHostIds());

		this.sessionKey = session.getSessionKey();
		this.created = session.getCreated();
	}

	public Session createSession() {
		Session session = new Session(sessionId, getSessionHostIdPair(), sessionKey, created);
		return session;
	}

	public Uid getSessionId() {
		return sessionId;
	}
	public void setSessionId(Uid sessionId) {
		this.sessionId = sessionId;
	}

	public HostId getHostId0() {
		return hostId0;
	}
	public void setHostId0(HostId hostId0) {
		this.hostId0 = hostId0;
	}

	@XmlTransient
	public SessionHostIdPair getSessionHostIdPair() {
		return new SessionHostIdPair(hostId0, hostId1);
	}

	public HostId getHostId1() {
		return hostId1;
	}
	public void setHostId1(HostId hostId1) {
		this.hostId1 = hostId1;
	}

	public byte[] getSessionKey() {
		return sessionKey;
	}
	public void setSessionKey(byte[] sessionKey) {
		this.sessionKey = sessionKey;
	}

	public Date getCreated() {
		return created;
	}
	public void setCreated(Date sessionCreateDate) {
		this.created = sessionCreateDate;
	}

//	public Date getExpired() {
//		return expired;
//	}
//	public void setExpired(Date sessionExpiryDate) {
//		this.expired = sessionExpiryDate;
//	}
}
