package co.codewizards.raspi1.steca;

import static java.util.Objects.*;

/**
 * <a href="https://www.lammertbies.nl/forum/viewtopic.php?t=1773">Found here.</a>
 * @author mn
 */
public class Crc {

	private final static int polynomial = 0x1021;   // Represents x^16+x^12+x^5+1

	private int crc;

	public Crc() {
		reset();
	}

	/**
	 * Updates the CRC with the given byte-array.
	 * @param val the value. Must not be <code>null</code>.
	 */
	public void update(final byte[] val) {
		requireNonNull(val, "val");
		update(val, 0, val.length);
	}

	/**
	 * Updates the CRC with the given byte-array starting at the index {@code pos}
	 * and taking {@code length} bytes into account.
	 * @param val the value. Must not be <code>null</code>.
	 * @param pos the start-position-index.
	 * @param length the number of bytes to use from {@code val}.
	 */
	public void update(final byte[] val, final int pos, final int length) {
		requireNonNull(val, "val");
		for (int i = pos; i < pos + length; i++) {
			update(val[i]);
		}
	}

	/**
	 * Updates the CRC with the given byte {@code val}.
	 * @param val the (next) byte to update the CRC value.
	 */
	public void update(final byte val) {
		for (int i = 0; i < 8; i++) {
			boolean bit = ((val >> (7-i) & 1) == 1);
			boolean c15 = ((crc >> 15    & 1) == 1);
			crc <<= 1;
			// If coefficient of bit and remainder polynomial = 1 xor crc with polynomial
			if (c15 ^ bit) crc ^= polynomial;
		}
	}

	/**
	 * Resets the CRC crc to 0.
	 */
	public void reset() {
		crc = 0;
	}

	/**
	 * Gets the CRC16 crc.
	 * @return the CRC16 crc.
	 */
	public int getCrc() {
		return crc;
	}

	public byte[] getCrcBytes() {
		final int value = getCrc();
		return new byte[] {
				(byte) ((value >>> 8) & 0xff),
				(byte) (value & 0xff)
		};
	}

//	public static void main(String[] args) {
////		byte[] val = new byte[] { 'Q', 'P', 'I', 'G', 'S' };
//		byte[] val = new byte[] { 'Q', 'M', 'O', 'D' };
//		Crc crc = new Crc();
//		crc.update(val, 0, val.length);
//		byte[] crcBytes = crc.getCrcBytes();
//		System.out.println(Integer.toHexString(crcBytes[0] & 0xff) + " " + Integer.toHexString(crcBytes[1] & 0xff));
//	}
}