package co.codewizards.raspi1.steca;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.codewizards.raspi1.steca.dto.InverterStatus;

public class GetInverterStatus extends StecaRequest<InverterStatus> {

	private static final Logger logger = LoggerFactory.getLogger(GetInverterStatus.class);

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

		String s = new String(response, StandardCharsets.US_ASCII);
		String[] fields = s.split(" ");
		if (fields.length != 21)
			throw new IOException("Malformed response: " + s);

		System.out.println(s);

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
		result.setStatusBitmask(parseIntBinary(fields[++idx]));
		result.setEepromVersion(parseInt(fields[++idx]));
		result.setPvChargePower(parseFloat(fields[++idx]));

		int statusBits2 = parseIntBinary(fields[++idx]);
		statusBits2 = statusBits2 << 8;
		result.setStatusBitmask(result.getStatusBitmask() | statusBits2);

		return result;
	}
}
