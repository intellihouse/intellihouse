package co.codewizards.raspi1;

import co.codewizards.raspi1.steca.GetInverterStatus;
import co.codewizards.raspi1.steca.StecaClientRxTx;
import co.codewizards.raspi1.steca.dto.InverterStatus;

public class StecaDemo implements Runnable {

	public StecaDemo(String[] args) {
	}

	@Override
	public void run() {
		try {
			StecaClientRxTx stecaClient0 = new StecaClientRxTx("/dev/ttyUSB0");
			StecaClientRxTx stecaClient1 = new StecaClientRxTx("/dev/ttyUSB1");

			for (int i = 0; i < 100 ; ++i) {
				System.out.println();
				InverterStatus inverterStatus0 = stecaClient0.execute(new GetInverterStatus());
				System.out.println("inverterStatus0: " + inverterStatus0);

				System.out.println();
				InverterStatus inverterStatus1 = stecaClient1.execute(new GetInverterStatus());
				System.out.println("inverterStatus1: " + inverterStatus1);

				System.out.println();
				Thread.sleep(1000);
			}

		} catch (Exception x) {
			x.printStackTrace();
		}
	}

}
