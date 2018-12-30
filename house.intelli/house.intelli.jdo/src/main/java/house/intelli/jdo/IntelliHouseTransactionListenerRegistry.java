package house.intelli.jdo;

import static java.util.Objects.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ServiceLoader;

public class IntelliHouseTransactionListenerRegistry {

	private final IntelliHouseTransaction transaction;

	private final List<IntelliHouseTransactionListener> listeners;
	private static List<Class<? extends IntelliHouseTransactionListener>> listenerClasses;

	public IntelliHouseTransactionListenerRegistry(final IntelliHouseTransaction transaction) {
		this.transaction = requireNonNull(transaction, "transaction");
		this.listeners = createListeners();

		for (final IntelliHouseTransactionListener listener : listeners)
			transaction.setContextObject(listener);
	}

	public IntelliHouseTransaction getTransaction() {
		return transaction;
	}

	/**
	 * Notifies this instance about the {@linkplain #getTransaction() transaction} being begun.
	 * @see #onCommit()
	 * @see #onRollback()
	 */
	public void onBegin() {
		for (final IntelliHouseTransactionListener listener : listeners)
			listener.onBegin();
	}

	/**
	 * Notifies this instance about the {@linkplain #getTransaction() transaction} being committed.
	 * @see #onBegin()
	 * @see #onRollback()
	 */
	public void onCommit() {
		// We flush *before* triggering each listener! It's likely that flushing causes a few JDO-lifecycle-listeners to be
		// triggered and thus the IntelliHouseTransactionListeners might work on an incomplete state, if we flushed later.
		// Additionally, every listener might change some data and we thus need to flush again between the listeners.
		transaction.flush();
		for (final IntelliHouseTransactionListener listener : listeners) {
			listener.onCommit();
			transaction.flush();
		}
	}

	/**
	 * Notifies this instance about the {@linkplain #getTransaction() transaction} being rolled back.
	 * @see #onBegin()
	 * @see #onCommit()
	 */
	public void onRollback() {
		for (final IntelliHouseTransactionListener listener : listeners)
			listener.onRollback();
	}

	private List<IntelliHouseTransactionListener> createListeners() {
		if (listenerClasses == null) {
			final List<IntelliHouseTransactionListener> listeners = new LinkedList<>();
			final Iterator<IntelliHouseTransactionListener> iterator = ServiceLoader.load(IntelliHouseTransactionListener.class).iterator();
			while (iterator.hasNext()) {
				final IntelliHouseTransactionListener listener = iterator.next();
				listener.setTransaction(transaction);
				listeners.add(listener);
			}

			sortListeners(listeners);

			final List<Class<? extends IntelliHouseTransactionListener>> lcl = new ArrayList<>(listeners.size());
			for (final IntelliHouseTransactionListener listener : listeners)
				lcl.add(listener.getClass());

			listenerClasses = lcl;
			return listeners;
		}
		else {
			final List<IntelliHouseTransactionListener> listeners = new ArrayList<>(listenerClasses.size());
			for (final Class<? extends IntelliHouseTransactionListener> lc : listenerClasses) {
				final IntelliHouseTransactionListener listener = createInstance(lc);
				listener.setTransaction(transaction);
				listeners.add(listener);
			}

			return listeners;
		}
	}

	private void sortListeners(final List<IntelliHouseTransactionListener> listeners) {
		Collections.sort(listeners, new Comparator<IntelliHouseTransactionListener>() {
			@Override
			public int compare(final IntelliHouseTransactionListener o1, final IntelliHouseTransactionListener o2) {
				final int result = -1 * Integer.compare(o1.getPriority(), o2.getPriority());
				if (result != 0)
					return result;

				return o1.getClass().getName().compareTo(o2.getClass().getName());
			}
		});
	}

	private static <T> T createInstance(final Class<T> clazz) {
		try {
			return clazz.newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}
}
