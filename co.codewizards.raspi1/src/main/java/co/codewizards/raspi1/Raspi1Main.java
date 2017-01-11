package co.codewizards.raspi1;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Raspi1Main implements Runnable {
	private final Logger logger = LoggerFactory.getLogger(Raspi1Main.class);
	
	private String[] args;
	
	public static void main(String[] args) {
		new Raspi1Main(args).run();
	}
	
	public Raspi1Main(String[] args) {
		this.args = args;
	}

	@Override
	public void run() {
//		Runnable demo = new LedDimmerDemo(args);
		Runnable demo = new SwitchInDemo(args);
		demo.run();
	}
}
