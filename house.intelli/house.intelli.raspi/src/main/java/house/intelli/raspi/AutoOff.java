package house.intelli.raspi;

import house.intelli.core.event.EventQueue;

public interface AutoOff {
	/**
	 * Gets the period in seconds, after which to automatically switch off the device.
	 * <p>
	 * A value &lt;= 0 means to never switch off (i.e. to disable the auto-off-feature).
	 * @return the period in seconds, after which to automatically switch off the device.
	 */
	int getAutoOffPeriod();

	void setAutoOffPeriod(int autoOffPeriod);

	/**
	 * Call-back method invoked to switch off the device. This method is invoked
	 * on the {@linkplain EventQueue#isDispatchThread() event-thread}, after the auto-off-period elapsed.
	 * @param event the auto-off-event. Never <code>null</code>.
	 */
	void onAutoOff(AutoOffEvent event);
}
