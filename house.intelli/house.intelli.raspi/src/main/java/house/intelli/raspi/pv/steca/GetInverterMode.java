package house.intelli.raspi.pv.steca;

import java.io.IOException;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GetInverterMode extends StecaRequest<InverterMode> {

	private static final Logger logger = LoggerFactory.getLogger(GetInverterMode.class);

//	private static final byte[] COMMAND = new byte[] {
//			'Q', 'M', 'O', 'D',
//			(byte) 0x49, (byte) 0xC1, // CRC
//			'\r'
//	};

	public GetInverterMode() {
	}

	@Override
	public InverterMode execute() throws IOException {
		try {
			return _execute();
		} catch (MalformedResponseException | CrcException x) {
			logger.warn("execute: Caught '{}' => Retrying.", x.toString());
			return _execute(); // retry
		} catch (IOException x) {
			throw x;
		}
	}

	protected InverterMode _execute() throws IOException {
//		final OutputStream out = getStecaClientOrFail().getOutputStream();
//		out.write(COMMAND);
		writeRequest("QMOD");

		final String response = readResponseAsString();
		if (response.length() != 1)
			throw new IOException("Response has unexpected length (!= 1): " + response);

		InverterMode result = new InverterMode();
		result.setMeasured(new Date());

		result.setMode(response.charAt(0));

		return result;
	}

}
