package house.intelli.jdo;

import java.util.EventObject;

import javax.jdo.PersistenceManager;

public class IntelliHouseTransactionPreCloseEvent extends EventObject {
	private static final long serialVersionUID = 1L;

	public IntelliHouseTransactionPreCloseEvent(IntelliHouseTransaction source) {
		super(source);
	}

	@Override
	public IntelliHouseTransaction getSource() {
		return (IntelliHouseTransaction) super.getSource();
	}

	/**
	 * Gets the <b>active</b> {@link IntelliHouseTransaction}.
	 * @return the <b>active</b> {@link IntelliHouseTransaction}. Never <code>null</code>.
	 */
	public IntelliHouseTransaction getTransaction() {
		return getSource();
	}

	/**
	 * Gets the {@code PersistenceManager}.
	 * @return the {@code PersistenceManager}. Never <code>null</code>.
	 */
	public PersistenceManager getPersistenceManager() {
		return getTransaction().getPersistenceManager();
	}
}
