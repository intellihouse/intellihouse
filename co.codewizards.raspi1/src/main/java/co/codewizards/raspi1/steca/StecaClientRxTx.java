package co.codewizards.raspi1.steca;

import static java.util.Objects.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gnu.io.CommPort;
import gnu.io.CommPortIdentifier;
import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;

public class StecaClientRxTx extends StecaClient {
	private static final Logger logger = LoggerFactory.getLogger(StecaClientRxTx.class);

	private static final int timeoutMillis = 5000; // TODO configurable?!
	private static final int baudrate = 2400;
	private static final int dataBits = SerialPort.DATABITS_8;
	private static final int stopBits = SerialPort.STOPBITS_1;
	private static final int parity = SerialPort.PARITY_NONE;

	private final String portName;

	private CommPortIdentifier commPortIdentifier;

	private CommPort commPort;

	private SerialPort serialPort;

	private volatile InputStream inputStream;

	private volatile OutputStream outputStream;

	public StecaClientRxTx(final String portName) {
		this.portName = requireNonNull(portName, "portName");
	}

	@Override
	public synchronized void open() throws IOException {
		if (isOpen())
			return;

		boolean failed = true;
		try {
			try {
				commPortIdentifier = CommPortIdentifier.getPortIdentifier(portName);
			} catch (NoSuchPortException e) {
				logger.error("open: Port '{}' not found! Listing existing ports:", portName);

				try {
					@SuppressWarnings("unchecked")
					Enumeration<CommPortIdentifier> commPorts = CommPortIdentifier.getPortIdentifiers();

					if (! commPorts.hasMoreElements()) {
						logger.error("open: THERE ARE NO PORTS!!!");
					}

					while (commPorts.hasMoreElements()) {
						CommPortIdentifier cpi = commPorts.nextElement();
						logger.error("open:   * {}", cpi.getName());
					}
				} catch (Exception x) {
					logger.error("open: Listing ports failed: " + x, x);
				}
				throw new IOException(String.format("Port '%s' could not be opened: %s", portName, e.getMessage()), e);
			}

			try {
				commPort = commPortIdentifier.open(this.getClass().getName(), timeoutMillis);
			} catch (PortInUseException e) {
				throw new IOException(String.format("Port '%s' is in use!", portName), e);
			}

			if (commPort == null)
				throw new IOException(String.format("Port '%s' could not be opened for an unknown reason!", portName));

			if (commPort instanceof SerialPort) {
				serialPort = (SerialPort) commPort;
			}
			else
				throw new IOException(String.format("Port '%s' is not a serial port! Its type is %s instead of %s!",
						portName, commPort.getClass().getName(), SerialPort.class.getName()));

			try {
				serialPort.setSerialPortParams(baudrate, dataBits, stopBits, parity);
			} catch (UnsupportedCommOperationException e) {
				throw new IOException(String.format("Port '%s' could not be configured: %s", portName, e.getMessage()), e);
			}

			inputStream = serialPort.getInputStream();
			outputStream = serialPort.getOutputStream();

			failed = false;
		} finally {
			if (failed)
				close();
		}
	}

	@Override
	public InputStream getInputStream() {
		final InputStream result = inputStream;
		if (result == null)
			throw newPortNotOpenException();

		return result;
	}

	@Override
	public OutputStream getOutputStream() {
		final OutputStream result = outputStream;
		if (result == null)
			throw newPortNotOpenException();

		return result;
	}

	@Override
	protected void preExecute(Request<?> request) throws IOException {
		// Clear all garbage from the input-stream *before* we send our request
		// to make sure that we really read the response afterwards -- and not the garbage.
		final InputStream in = getInputStream();
		int bytesAvailable;
		while ((bytesAvailable = in.available()) > 0) {
			in.skip(bytesAvailable);
		}
	}

	@Override
	public boolean isOpen() {
		return outputStream != null;
	}

	protected RuntimeException newPortNotOpenException() {
		return new IllegalStateException(String.format("Port '%s' is not open!", portName));
	}

	@Override
	public synchronized void close() throws IOException {
		if (outputStream != null) {
			outputStream.close();
			outputStream = null;
		}
		if (inputStream != null) {
			inputStream.close();
			inputStream = null;
		}
		serialPort = null; // same as commPort -- closed below.

		if (commPort != null) {
			logger.info("close: Closing port '{}'...", portName);
			commPort.close();
			commPort = null;
			logger.info("close: Closed port '{}'.", portName);
		}

		if (commPortIdentifier != null) {
			commPortIdentifier = null;
		}
	}

}
