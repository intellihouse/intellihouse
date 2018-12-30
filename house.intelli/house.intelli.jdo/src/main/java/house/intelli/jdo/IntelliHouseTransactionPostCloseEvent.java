package house.intelli.jdo;

import static java.util.Objects.*;

import java.util.EventObject;

import javax.jdo.PersistenceManager;

import house.intelli.core.context.ExtensibleContext;

public class IntelliHouseTransactionPostCloseEvent extends EventObject {
	private static final long serialVersionUID = 1L;

	private final PersistenceManager persistenceManager;

	public IntelliHouseTransactionPostCloseEvent(IntelliHouseTransaction source) {
		super(source);
		persistenceManager = requireNonNull(source, "source").getPersistenceManager();
	}

	@Override
	public IntelliHouseTransaction getSource() {
		return (IntelliHouseTransaction) super.getSource();
	}

	/**
	 * Gets the <b>closed</b> {@link IntelliHouseTransaction}.
	 * <p>
	 * Please note that this event is fired after the transaction was already closed. This object thus
	 * cannot be used for anything else than accessing its
	 * {@linkplain ExtensibleContext#getContextObject(Class) context-objects}.
	 * <p>
	 * Alternatively, you might want to access the {@link #getPersistenceManager() persistenceManager}
	 * and create a new transaction.
	 * @return the <b>closed</b> {@link IntelliHouseTransaction}. Never <code>null</code>.
	 */
	public IntelliHouseTransaction getTransaction() {
		return getSource();
	}

	/**
	 * Gets the {@code LocalRepoManager}.
	 * @return the {@code LocalRepoManager}. Never <code>null</code>.
	 */
	public PersistenceManager getPersistenceManager() {
		return persistenceManager;
	}
}
