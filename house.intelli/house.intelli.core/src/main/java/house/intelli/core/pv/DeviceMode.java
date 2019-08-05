package house.intelli.core.pv;

import static java.util.Objects.*;

public enum DeviceMode {

	BATTERY,
	LINE,
	FAULT,
	POWERED,
	STAND_BY,
	ENERGY_SAVING
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
			case 'F':
				return FAULT;
			case 'P':
				return POWERED;
			case 'S':
				return STAND_BY;
			case 'H':
			case 'E': // *not* coming from the inverter, but for compatibility with the valueOf(...) method.
				return ENERGY_SAVING;
			default:
				throw new IllegalArgumentException("string represents no known DeviceMode: " + string);
		}
	}

}
