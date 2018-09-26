package co.codewizards.raspi1.steca;

import java.io.IOException;
import java.io.OutputStream;

import co.codewizards.raspi1.steca.dto.InverterMode;

public class GetInverterMode extends AbstractRequest<InverterMode> {

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


		return null;
	}

	@Override
	public boolean isResultNullable() {
		return false;
	}
}
