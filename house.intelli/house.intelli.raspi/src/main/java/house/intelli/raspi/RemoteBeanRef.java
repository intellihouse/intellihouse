package house.intelli.raspi;

import static house.intelli.core.util.Util.*;

import house.intelli.core.rpc.HostId;

public class RemoteBeanRef {

	private HostId hostId;

	private String beanId;

	public RemoteBeanRef() {
	}

	public HostId getHostId() {
		return hostId;
	}

	public void setHostId(final HostId hostId) {
		if (this.hostId != null)
			throw new IllegalStateException("hostId already assgined!");

		this.hostId = hostId;
	}

	public void setHostId(final String hostId) {
		setHostId(hostId == null ? null : new HostId(hostId));
	}

	public String getBeanId() {
		return beanId;
	}

	public void setBeanId(final String beanId) {
		if (this.beanId != null)
			throw new IllegalStateException("beanId already assgined!");

		this.beanId = beanId;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((hostId == null) ? 0 : hostId.hashCode());
		result = prime * result + ((beanId == null) ? 0 : beanId.hashCode());
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj)
			return true;

		if (obj == null)
			return false;

		if (getClass() != obj.getClass())
			return false;

		final RemoteBeanRef other = (RemoteBeanRef) obj;
		return equal(this.beanId, other.beanId) && equal(this.hostId, other.hostId);
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + '[' + toString_getProperties() + ']';
	}

	protected String toString_getProperties() {
		return "hostId=" + hostId + ", beanId=" + beanId;
	}
}
