package co.codewizards.raspi1.steca;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import co.codewizards.raspi1.steca.dto.InverterStatus;

public class GetInverterStatus extends AbstractRequest<InverterStatus> {

	private static final long readTimeoutMillis = 3000;

	private static final byte[] COMMAND = new byte[] {
			'Q', 'P', 'I', 'G', 'S',
			(byte) 0xB7, (byte) 0xA9, // CRC
			'\r'
	};

	public GetInverterStatus() {
	}

	@Override
	public InverterStatus execute() throws IOException {
		final OutputStream out = getStecaClientOrFail().getOutputStream();


		out.write(COMMAND);

		final byte[] response = readResponse();
		// TODO CRC check!

		if (response[0] != '(')
			throw new IOException("Response did not start with '('!");

		final byte[] payload = new byte[response.length - 1 - 2 - 1]; // - starter '(' - CRC - \r
		System.arraycopy(response, 1, payload, 0, payload.length);

//		Reader r = new InputStreamReader(new ByteArrayInputStream(payload), StandardCharsets.US_ASCII);

		String s = new String(payload, StandardCharsets.US_ASCII);
		String[] fields = s.split(" ");
		if (fields.length != 21)
			throw new IOException("Malformed response: " + s);

		InverterStatus result = new InverterStatus();

		int idx = -1;
		result.setAcInVoltage(parseFloat(fields[++idx]));
		result.setAcInFrequency(parseFloat(fields[++idx]));
		result.setAcOutVoltage(parseFloat(fields[++idx]));
		result.setAcOutFrequency(parseFloat(fields[++idx]));
		result.setAcOutApparentPower(parseFloat(fields[++idx]));
		result.setAcOutActivePower(parseFloat(fields[++idx]));
		result.setAcOutLoadPercentage(parseFloat(fields[++idx]));
		result.setInternalBusVoltage(parseFloat(fields[++idx]));
		result.setBatteryVoltageAtInverter(parseFloat(fields[++idx]));
		result.setBatteryChargeCurrent(parseFloat(fields[++idx]));
		result.setBatteryCapacityPercentage(parseFloat(fields[++idx]));
		result.setHeatSinkTemperature(parseFloat(fields[++idx]));
		result.setPvToBatteryCurrent(parseInt(fields[++idx]));
		result.setPvVoltage(parseFloat(fields[++idx]));
		result.setBatteryVoltageAtCharger(parseFloat(fields[++idx]));
		result.setBatteryDischargeCurrent(parseFloat(fields[++idx]));
		result.setStatusBitmask(parseInt(fields[++idx]));
		result.setEepromVersion(parseInt(fields[++idx]));
		result.setPvChargePower(parseFloat(fields[++idx]));

		return result;
	}

	private int parseInt(String string) {
		return Integer.parseInt(string);
	}

	private float parseFloat(String string) {
		return Float.parseFloat(string);
	}

	/**
	 * Reads a response (until CR) in a non-blocking way and taking the timeout {@link #readTimeoutMillis} into account.
	 * @return
	 * @throws IOException
	 */
	protected byte[] readResponse() throws IOException {
		final InputStream in = getStecaClientOrFail().getInputStream();

		final byte[] buf = new byte[32];
		final ByteArrayOutputStream responseBuffer = new ByteArrayOutputStream();

		final long startTimeStamp = System.currentTimeMillis();
		while (true) {
			if (System.currentTimeMillis() - startTimeStamp > readTimeoutMillis)
				throw new IOException("Timeout! Read so far: " + Arrays.toString(responseBuffer.toByteArray()));

			int available = in.available();
			if (available > 0) {
				int length = in.read(buf);
				responseBuffer.write(buf, 0, length);
				if (containsCr(buf, length))
					return responseBuffer.toByteArray();
			}
			else {
				try {
					Thread.sleep(300);
				} catch (InterruptedException e) {
					// ignore
				}
			}
		}
	}

	@Override
	public boolean isResultNullable() {
		return false;
	}

	protected boolean containsCr(byte[] buf, int length) {
		for (int i = 0; i < buf.length; i++) {
			if (i >= length)
				return false;

			if (buf[i] == '\r')
				return true;
		}
		return false;
	}
}
