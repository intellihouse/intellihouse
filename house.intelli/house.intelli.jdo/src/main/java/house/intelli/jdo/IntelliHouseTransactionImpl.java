package house.intelli.jdo;

import static java.util.Objects.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;
import javax.jdo.Transaction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import house.intelli.core.context.ExtensibleContextSupport;

public class IntelliHouseTransactionImpl implements IntelliHouseTransaction, ContextWithPersistenceManager {
	private static final Logger logger = LoggerFactory.getLogger(IntelliHouseTransactionImpl.class);

	private final PersistenceManagerFactory persistenceManagerFactory;
	private PersistenceManager persistenceManager;
	private Transaction jdoTransaction;
	private final Map<Class<?>, Object> daoClass2Dao = new HashMap<>();
	private final ExtensibleContextSupport extensibleContextSupport = new ExtensibleContextSupport();

	private final IntelliHouseTransactionListenerRegistry listenerRegistry = new IntelliHouseTransactionListenerRegistry(this);

	private final CopyOnWriteArrayList<IntelliHouseTransactionPreCloseListener> preCloseListeners = new CopyOnWriteArrayList<>();
	private final CopyOnWriteArrayList<IntelliHouseTransactionPostCloseListener> postCloseListeners = new CopyOnWriteArrayList<>();

	public IntelliHouseTransactionImpl(final PersistenceManagerFactory persistenceManagerFactory) {
		this.persistenceManagerFactory = requireNonNull(persistenceManagerFactory, "persistenceManagerFactory");
		begin();
	}

	private void begin() {
		if (isActive())
			throw new IllegalStateException("Transaction is already active!");

		persistenceManager = persistenceManagerFactory.getPersistenceManager();
		jdoTransaction = persistenceManager.currentTransaction();
		jdoTransaction.begin();
		listenerRegistry.onBegin();
	}

	@Override
	public void commit() {
		if (!isActive())
			throw new IllegalStateException("Transaction is not active!");

		listenerRegistry.onCommit();
		firePreCloseListeners(true);
		daoClass2Dao.clear();
		jdoTransaction.commit();
		persistenceManager.close();
		jdoTransaction = null;
		persistenceManager = null;

		firePostCloseListeners(true);
	}

	@Override
	public boolean isActive() {
		final Transaction tx = jdoTransaction;
		return tx != null && tx.isActive();
	}

	@Override
	public void rollback() {
		_rollback();
		firePostCloseListeners(false);
	}

	@Override
	public void rollbackIfActive() {
		final boolean active = isActive();

		if (active) {
			_rollback();
		}

		if (active) {
			firePostCloseListeners(false);
		}
	}

	protected void _rollback() {
		if (!isActive())
			throw new IllegalStateException("Transaction is not active!");

		listenerRegistry.onRollback();
		firePreCloseListeners(false);
		daoClass2Dao.clear();
		jdoTransaction.rollback();
		persistenceManager.close();
		jdoTransaction = null;
		persistenceManager = null;
	}

	@Override
	public void close() {
		rollbackIfActive();
	}

	@Override
	public PersistenceManager getPersistenceManager() {
		if (!isActive()) {
			throw new IllegalStateException("Transaction is not active!");
		}
		return persistenceManager;
	}

	@Override
	public <D> D getDao(final Class<D> daoClass) {
		requireNonNull(daoClass, "daoClass");

		@SuppressWarnings("unchecked")
		D dao = (D) daoClass2Dao.get(daoClass);

		if (dao == null) {
			final PersistenceManager pm = getPersistenceManager();
			try {
				dao = daoClass.newInstance();
			} catch (final InstantiationException e) {
				throw new RuntimeException(e);
			} catch (final IllegalAccessException e) {
				throw new RuntimeException(e);
			}

			if (!(dao instanceof Dao))
				throw new IllegalStateException(String.format("dao class %s does not extend Dao!", daoClass.getName()));

			((Dao<?, ?>)dao).setPersistenceManager(pm);
			((Dao<?, ?>)dao).setDaoProvider(this);

			daoClass2Dao.put(daoClass, dao);
		}
		return dao;
	}

	@Override
	public void flush() {
		final PersistenceManager pm = getPersistenceManager();
		pm.flush();
	}

	@Override
	public void setContextObject(final Object object) {
		extensibleContextSupport.setContextObject(object);
	}

	@Override
	public <T> T getContextObject(final Class<T> clazz) {
		return extensibleContextSupport.getContextObject(clazz);
	}

	@Override
	public void removeContextObject(Object object) {
		extensibleContextSupport.removeContextObject(object);
	}

	@Override
	public void removeContextObject(Class<?> clazz) {
		extensibleContextSupport.removeContextObject(clazz);
	}

	@Override
	public void addPreCloseListener(IntelliHouseTransactionPreCloseListener listener) {
		preCloseListeners.add(requireNonNull(listener, "listener"));
	}
	@Override
	public void addPostCloseListener(IntelliHouseTransactionPostCloseListener listener) {
		postCloseListeners.add(requireNonNull(listener, "listener"));
	}

	protected void firePreCloseListeners(final boolean commit) {
		IntelliHouseTransactionPreCloseEvent event = null;
		for (final IntelliHouseTransactionPreCloseListener listener : preCloseListeners) {
			try {
				if (event == null)
					event = new IntelliHouseTransactionPreCloseEvent(this);

				if (commit)
					listener.preCommit(event);
				else
					listener.preRollback(event);
			} catch (Exception x) {
				logger.error("firePreCloseListeners: " + x, x);
			}
		}
	}
	protected void firePostCloseListeners(final boolean commit) {
		IntelliHouseTransactionPostCloseEvent event = null;
		for (final IntelliHouseTransactionPostCloseListener listener : postCloseListeners) {
			try {
				if (event == null)
					event = new IntelliHouseTransactionPostCloseEvent(this);

				if (commit)
					listener.postCommit(event);
				else
					listener.postRollback(event);
			} catch (Exception x) {
				logger.error("firePostCloseListeners: " + x, x);
			}
		}
	}
}
