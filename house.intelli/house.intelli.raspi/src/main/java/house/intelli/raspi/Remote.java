package house.intelli.raspi;

import house.intelli.core.rpc.RemoteBeanRef;

public interface Remote {

	RemoteBeanRef getRemoteBeanRef();
	void setRemoteBeanRef(RemoteBeanRef remoteBeanRef);

}
