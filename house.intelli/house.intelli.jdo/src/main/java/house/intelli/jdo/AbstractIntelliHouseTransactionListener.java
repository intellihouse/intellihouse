package house.intelli.jdo;

import static java.util.Objects.*;

public abstract class AbstractIntelliHouseTransactionListener implements IntelliHouseTransactionListener {

	private IntelliHouseTransaction transaction;

	@Override
	public int getPriority() {
		return 0;
	}

	@Override
	public IntelliHouseTransaction getTransaction() {
		return transaction;
	}

	protected IntelliHouseTransaction getTransactionOrFail() {
		final IntelliHouseTransaction transaction = getTransaction();
		requireNonNull(transaction, "transaction");
		return transaction;
	}

	@Override
	public void setTransaction(final IntelliHouseTransaction transaction) {
		this.transaction = transaction;
	}

	@Override
	public void onBegin() {
		// override to react on this!
	}

	@Override
	public void onCommit() {
		// override to react on this!
	}

	@Override
	public void onRollback() {
		// override to react on this!
	}

}
