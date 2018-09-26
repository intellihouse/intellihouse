package co.codewizards.raspi1.steca;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import co.codewizards.raspi1.steca.dto.InverterMode;

public class GetInverterMode extends StecaRequest<InverterMode> {

	private static final byte[] COMMAND = new byte[] {
			'Q', 'M', 'O', 'D',
			(byte) 0x49, (byte) 0xC1, // CRC
			'\r'
	};

	public GetInverterMode() {
	}

	@Override
	public InverterMode execute() throws IOException {
		final OutputStream out = getStecaClientOrFail().getOutputStream();
		out.write(COMMAND);

		final byte[] response = readResponse();

		String s = new String(response, StandardCharsets.US_ASCII);
		if (s.length() != 1)
			throw new IOException("Response has unexpected length (!= 1): " + s);

		InverterMode result = new InverterMode();

		result.setMode(s.charAt(0));

		return result;
	}

}
