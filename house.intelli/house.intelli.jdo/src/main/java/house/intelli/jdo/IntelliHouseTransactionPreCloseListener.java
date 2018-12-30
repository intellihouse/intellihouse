package house.intelli.jdo;

import java.util.EventListener;

public interface IntelliHouseTransactionPreCloseListener extends EventListener {

	void preCommit(IntelliHouseTransactionPreCloseEvent event);

	void preRollback(IntelliHouseTransactionPreCloseEvent event);

}
