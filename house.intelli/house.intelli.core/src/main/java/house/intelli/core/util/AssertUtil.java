package house.intelli.core.util;

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
}
