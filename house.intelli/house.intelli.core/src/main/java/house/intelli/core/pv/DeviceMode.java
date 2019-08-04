package house.intelli.core.pv;

import static java.util.Objects.*;

public enum DeviceMode {

	BATTERY,
	LINE
	;

	public static DeviceMode from(final String string) {
		requireNonNull(string, "string");
		if (string.length() < 1)
			throw new IllegalArgumentException("string is empty");

		final char c = string.charAt(0);
		switch (c) {
			case 'B':
				return BATTERY;
			case 'L':
				return LINE;
			default:
				throw new IllegalArgumentException("string represents no known DeviceMode: " + string);
		}
	}

}
