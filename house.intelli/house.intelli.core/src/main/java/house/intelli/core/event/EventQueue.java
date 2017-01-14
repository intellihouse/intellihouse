package house.intelli.core.event;

import java.lang.reflect.InvocationTargetException;

public final class EventQueue {

	private EventQueue() {
	}

	/**
	 * Causes <i>doRun.run()</i> to be executed asynchronously on the
	 * event dispatching thread.  This will happen after all
	 * pending events have been processed.
	 *
	 * @see #invokeAndWait(Runnable)
	 */
	public static void invokeLater(Runnable doRun) {
		java.awt.EventQueue.invokeLater(doRun);
	}

	/**
	 * Causes <code>doRun.run()</code> to be executed synchronously on the
	 * event dispatching thread.  This call blocks until
	 * all pending events have been processed and (then)
	 * <code>doRun.run()</code> returns.
	 *
	 * @throws RuntimeException if <code>doRun.run(...)</code> threw an exception.
	 * @see #invokeLater(Runnable)
	 */
	public static void invokeAndWait(final Runnable doRun) throws RuntimeException {
		try {
			java.awt.EventQueue.invokeAndWait(doRun);
		} catch (InvocationTargetException e) {
			Throwable targetException = e.getTargetException();
			if (targetException instanceof RuntimeException)
				throw (RuntimeException) targetException;

			if (targetException instanceof Error)
				throw (Error) targetException;

			throw new RuntimeException(targetException);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}
}
