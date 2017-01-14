package house.intelli.core.util;

import java.awt.EventQueue;

public final class AssertUtil {
	private AssertUtil() {
	}

	public static <T> T assertNotNull(T object, String name) {
		if (object == null)
			throw new NullPointerException(name);

		if (name == null)
			throw new NullPointerException("name");

		return object;
	}

	public static void assertEventThread() {
		if (! EventQueue.isDispatchThread())
			throw new IllegalStateException("This thread is not the EventQueue.dispatchThread! " + Thread.currentThread().getName());
	}
}
