package co.codewizards.raspi1.steca.dto;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class InverterMode {

	public static final char MODE_ON = 'P';
	public static final char MODE_STAND_BY = 'S';
	public static final char MODE_AC_BY_PASS = 'L';
	public static final char MODE_BATTERY = 'B';
	public static final char MODE_ERROR = 'F';
	public static final char MODE_POWER_SAVE = 'H';

	private char mode;

	public char getMode() {
		return mode;
	}

	public void setMode(char mode) {
		this.mode = mode;
	}

	@Override
	public String toString() {
		return "InverterMode [mode=" + mode + "]";
	}
}
