package house.intelli.jdo;

public abstract class IntelliHouseTransactionPostCloseAdapter implements IntelliHouseTransactionPostCloseListener {

	@Override
	public void postCommit(IntelliHouseTransactionPostCloseEvent event) {
	}

	@Override
	public void postRollback(IntelliHouseTransactionPostCloseEvent event) {
	}
}
