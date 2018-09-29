package co.codewizards.raspi1;

import java.util.Date;

import co.codewizards.raspi1.steca.GetInverterMode;
import co.codewizards.raspi1.steca.GetInverterStatus;
import co.codewizards.raspi1.steca.StecaClientRxTx;
import co.codewizards.raspi1.steca.dto.InverterMode;
import co.codewizards.raspi1.steca.dto.InverterStatus;

public class StecaDemo implements Runnable {

	public StecaDemo(String[] args) {
	}

	@Override
	public void run() {
		try {
			StecaClientRxTx stecaClient0 = new StecaClientRxTx("/dev/ttyUSB0");
			StecaClientRxTx stecaClient1 = new StecaClientRxTx("/dev/ttyUSB1");

			while (! Thread.currentThread().isInterrupted()) {
				try {
					System.out.println();
					System.out.println(new Date());

					System.out.println();
					InverterMode inverterMode0 = stecaClient0.execute(new GetInverterMode());
					System.out.println("inverterMode0: " + inverterMode0);

					InverterStatus inverterStatus0 = stecaClient0.execute(new GetInverterStatus());
					System.out.println("inverterStatus0: " + inverterStatus0);

					System.out.println();
					InverterMode inverterMode1 = stecaClient1.execute(new GetInverterMode());
					System.out.println("inverterMode1: " + inverterMode1);

					InverterStatus inverterStatus1 = stecaClient1.execute(new GetInverterStatus());
					System.out.println("inverterStatus1: " + inverterStatus1);

					System.out.println();
					Thread.sleep(1000);
				} catch (Exception x) {
					x.printStackTrace();

					System.out.println("Closing stecaClient0...");
					stecaClient0.close();
					System.out.println("Closed stecaClient0.");

					System.out.println("Closing stecaClient1...");
					stecaClient1.close();
					System.out.println("Closed stecaClient1.");

					Thread.sleep(5000);
				}
			}

		} catch (Exception x) {
			x.printStackTrace();
		}
	}

}
