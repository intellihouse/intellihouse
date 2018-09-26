package co.codewizards.raspi1;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;

import gnu.io.CommPort;
import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;

public class RxTxDemo implements Runnable {

	public RxTxDemo(String[] args) {
	}

	@Override
	public void run() {
		try {
			System.out.println("Listing available serial ports...");

			@SuppressWarnings("unchecked")
			Enumeration<CommPortIdentifier> commPorts = CommPortIdentifier.getPortIdentifiers();

			if (! commPorts.hasMoreElements()) {
				System.out.println("*** THERE ARE NO SERIAL PORTS!!! ***");
			}

			while (commPorts.hasMoreElements()) {
				CommPortIdentifier portIdentifier = commPorts.nextElement();
				System.out.println();
				System.out.println(portIdentifier.getName());
			}

			CommPortIdentifier portIdentifier = CommPortIdentifier.getPortIdentifier("/dev/ttyUSB1");

			if( portIdentifier.isCurrentlyOwned() ) {
				System.out.println( "Error: Port is currently in use" );
			} else {
				System.out.println(String.format("Opening port %s...", portIdentifier.getName()));
				int timeout = 2000;
				CommPort commPort = portIdentifier.open( this.getClass().getName(), timeout );
				System.out.println(String.format("Opened port %s!", portIdentifier.getName()));

				if( commPort instanceof SerialPort ) {
					SerialPort serialPort = ( SerialPort )commPort;
					serialPort.setSerialPortParams( 2400,
							SerialPort.DATABITS_8,
							SerialPort.STOPBITS_1,
							SerialPort.PARITY_NONE );

					InputStream in = serialPort.getInputStream();
					OutputStream out = serialPort.getOutputStream();

					SerialReader serialReader = new SerialReader(in);
					serialReader.start();

					byte[] command = new byte[] {
							'Q', 'P', 'I', 'G', 'S',
							(byte) 0xB7, (byte) 0xA9, // CRC
							'\r'
					};

					for (int i = 0; i < 5 ; ++i) {
						System.out.println("sending...");
						out.write(command);
						System.out.println("sent!");

						Thread.sleep(5000L);
					}

					serialReader.interrupt();
					in.close();
					out.close();

				} else {
					System.out.println( "Error: Not a serial port!" );
				}

				System.out.println(String.format("Closing port %s...", portIdentifier.getName()));
				commPort.close();
				System.out.println(String.format("Closed port %s!", portIdentifier.getName()));
			}
		} catch (Exception x) {
			x.printStackTrace();
		}
	}

	public static class SerialReader extends Thread {

		InputStream in;

		public SerialReader( InputStream in ) {
			this.in = in;
		}

		@Override
		public void run() {
			byte[] buffer = new byte[ 1024 ];
			int len = -1;
			try {
				while( ( len = this.in.read( buffer ) ) > -1 ) {
					System.out.print( new String( buffer, 0, len ) );
				}
			} catch( IOException e ) {
				e.printStackTrace();
			}
		}
	}

}
