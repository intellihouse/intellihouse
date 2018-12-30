package house.intelli.jdo;

import javax.jdo.PersistenceManager;

import house.intelli.core.context.ExtensibleContext;

public interface IntelliHouseTransaction extends AutoCloseable, DaoProvider, ExtensibleContext {

	void commit();

	boolean isActive();

	void rollback();

	void rollbackIfActive();

	/**
	 * Equivalent to {@link #rollbackIfActive()}.
	 * <p>
	 * Implementations must make sure that invoking {@code close()} means exactly the same as invoking
	 * {@code rollbackIfActive()}. This method was added to make the usage of {@code IntelliHouseTransaction}
	 * possible in a try-with-resources-clause. See {@link AutoCloseable} for more details. Here's a code
	 * example:
	 * <pre>  try ( IntelliHouseTransaction transaction = localRepoManager.beginWriteTransaction(); ) {
	 *    // Write sth. into the database...
	 *
	 *    // And don't forget to commit!
	 *    transaction.commit();
	 *  }</pre>
	 * <p>
	 * @see #rollbackIfActive()
	 */
	@Override
	public void close();

	PersistenceManager getPersistenceManager();

	void flush();

	void addPreCloseListener(IntelliHouseTransactionPreCloseListener listener);

	void addPostCloseListener(IntelliHouseTransactionPostCloseListener listener);
}
