package house.intelli.jdo;

public interface IntelliHouseTransactionListener {

	int getPriority();

	IntelliHouseTransaction getTransaction();

	void setTransaction(IntelliHouseTransaction transaction);

	/**
	 * Notifies this instance about the {@linkplain #getTransaction() transaction} being begun.
	 * @see #onCommit()
	 * @see #onRollback()
	 */
	void onBegin();

	/**
	 * Notifies this instance about the {@linkplain #getTransaction() transaction} being committed.
	 * @see #onBegin()
	 * @see #onRollback()
	 */
	void onCommit();

	/**
	 * Notifies this instance about the {@linkplain #getTransaction() transaction} being rolled back.
	 * @see #onBegin()
	 * @see #onCommit()
	 */
	void onRollback();

}
