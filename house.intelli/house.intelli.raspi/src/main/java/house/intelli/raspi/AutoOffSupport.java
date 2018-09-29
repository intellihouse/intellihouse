package house.intelli.raspi;

import static house.intelli.core.event.EventQueue.*;
import static house.intelli.core.util.AssertUtil.*;
import static java.util.Objects.*;

import java.util.Timer;
import java.util.TimerTask;

public class AutoOffSupport {

	public final AutoOff autoOff;

	private static final Timer autoOffTimer = new Timer("AutoOffSupport.autoOffTimer", true);
	private TimerTask autoOffTimerTask;

	public AutoOffSupport(AutoOff autoOff) {
		this.autoOff = requireNonNull(autoOff, "autoOff");
	}

	public void scheduleDeferredAutoOff() {
		assertEventThread();
		if (autoOffTimerTask != null) {
			autoOffTimerTask.cancel();
			autoOffTimerTask = null;
		}

		final int autoOffPeriod = autoOff.getAutoOffPeriod();
		if (autoOffPeriod <= 0)
			return;

		autoOffTimerTask = new TimerTask() {
			@Override
			public void run() {
				invokeLater(() -> {
					autoOffTimerTask = null;
					AutoOffEvent event = new AutoOffEvent(autoOff);
					autoOff.onAutoOff(event);
				});
			}
		};
		autoOffTimer.schedule(autoOffTimerTask, autoOffPeriod * 1000L);
	}

	public void cancelDeferredAutoOff() {
		assertEventThread();
		if (autoOffTimerTask != null) {
			autoOffTimerTask.cancel();
			autoOffTimerTask = null;
		}
	}
}
