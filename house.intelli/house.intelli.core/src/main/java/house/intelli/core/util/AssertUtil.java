package house.intelli.core.util;

import static house.intelli.core.util.StringUtil.*;

import java.util.Objects;

import house.intelli.core.event.EventQueue;

public final class AssertUtil {
	private AssertUtil() {
	}

	/**
	 * @deprecated Use {@link Objects#requireNonNull(Object, String)} instead!
	 */
	@Deprecated
	public static <T> T assertNotNull(T object, String name) {
		if (object == null)
			throw new NullPointerException(name);

		if (name == null)
			throw new NullPointerException("name");

		return object;
	}

	public static String assertNotEmpty(String value, String name) {
		if (value == null)
			throw new NullPointerException(name);

		if (name == null)
			throw new NullPointerException("name");

		if (isEmpty(value))
			throw new IllegalArgumentException(name + " is empty!");

		return value;
	}

	public static void assertEventThread() {
		if (! EventQueue.isDispatchThread())
			throw new IllegalStateException("This thread is not the EventQueue.dispatchThread! " + Thread.currentThread().getName());
	}
}
