package house.intelli.jdo;

import java.util.EventListener;

public interface IntelliHouseTransactionPostCloseListener extends EventListener {

	void postCommit(IntelliHouseTransactionPostCloseEvent event);

	void postRollback(IntelliHouseTransactionPostCloseEvent event);

}
