package house.intelli.jdo;

public abstract class IntelliHouseTransactionPreCloseAdapter implements IntelliHouseTransactionPreCloseListener {

	@Override
	public void preCommit(IntelliHouseTransactionPreCloseEvent event) {
	}

	@Override
	public void preRollback(IntelliHouseTransactionPreCloseEvent event) {
	}
}
