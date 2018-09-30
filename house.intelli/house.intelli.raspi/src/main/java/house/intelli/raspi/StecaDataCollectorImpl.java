package house.intelli.raspi;

import static house.intelli.core.event.EventQueue.*;
import static house.intelli.core.util.AssertUtil.*;
import static house.intelli.core.util.StringUtil.*;
import static java.util.Objects.*;

import java.io.File;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import house.intelli.core.bean.AbstractBean;
import house.intelli.core.config.ConfigDir;
import house.intelli.raspi.pv.DataCollectorEvent;
import house.intelli.raspi.pv.DataCollectorListener;
import house.intelli.raspi.pv.steca.GetInverterMode;
import house.intelli.raspi.pv.steca.GetInverterStatus;
import house.intelli.raspi.pv.steca.InverterMode;
import house.intelli.raspi.pv.steca.InverterStatus;
import house.intelli.raspi.pv.steca.StecaClient;
import house.intelli.raspi.pv.steca.StecaClientRxTx;

public class StecaDataCollectorImpl extends AbstractBean<PvDataCollector.Property> implements PvDataCollector, AutoCloseable {

	private static final Logger logger = LoggerFactory.getLogger(StecaDataCollectorImpl.class);

	protected static final StecaInterfaceType DEFAULT_INTERFACE_TYPE = StecaInterfaceType.RXTX;

	private String beanName;
	private String deviceName;
	private StecaInterfaceType interfaceType;
	private String interfaceAddress;
	private volatile StecaClient stecaClient;
	private long collectPeriod = 1000L;
	private Timer dataCollectorTimer;
	private TimerTask dataCollectorTimerTask;
	private CopyOnWriteArrayList<DataCollectorListener> dataCollectorListeners = new CopyOnWriteArrayList<>();
	private volatile int consecutiveErrorCount;
	private int resetUsbAfterConsecutiveErrorCount;

	public StecaDataCollectorImpl() {
	}

	public void init() {
		assertEventThread();

		if (isEmpty(getDeviceName()))
			throw new IllegalStateException(String.format("%s: deviceName not specified!", getBeanInstanceName()));

		if (getInterfaceType() == null) {
			logger.info("init: bean={}: interfaceType no specified! Setting default value {}.",
					getBeanInstanceName(), DEFAULT_INTERFACE_TYPE);

			setInterfaceType(DEFAULT_INTERFACE_TYPE);
		}

		if (isEmpty(getInterfaceAddress()))
			throw new IllegalStateException(String.format("%s: interfaceAddress not specified!", getBeanInstanceName()));

		if (getCollectPeriod() < 100)
			setCollectPeriod(100);

		close(); // in case, this method is called multiple times

		stecaClient = createStecaClient();

		dataCollectorTimer = new Timer(String.format("dataCollectorTimer[%s]", getBeanName()));
		dataCollectorTimerTask = new TimerTask() {
			@Override
			public void run() {
				try {
					onDataCollectorTimerTaskRun();

					final int resetUsbAfterConsecutiveErrorCount = getResetUsbAfterConsecutiveErrorCount();
					if (resetUsbAfterConsecutiveErrorCount > 0 && consecutiveErrorCount >= resetUsbAfterConsecutiveErrorCount) {
						consecutiveErrorCount = 0;
						requestResetUsbAndExit();
					}
				} catch (Throwable x) {
					logger.error(getBeanInstanceName() + ".dataCollectorTimerTask.run: " + x, x);
				}
			}
		};
		dataCollectorTimer.scheduleAtFixedRate(dataCollectorTimerTask, 0L, getCollectPeriod());
	}

	private StecaClient createStecaClient() {
		final StecaInterfaceType interfaceType = requireNonNull(getInterfaceType(), "interfaceType");
		final String interfaceAddress = requireNonNull(getInterfaceAddress(), "interfaceAddress");
		switch(interfaceType) {
			case RXTX:
				return new StecaClientRxTx(interfaceAddress);
			default:
				throw new UnsupportedOperationException("InterfaceType not yet supported: " + interfaceType);
		}
	}

	@Override
	public void close() {
		invokeAndWait(() -> {
			final StecaClient stecaClient = this.stecaClient;
			if (stecaClient != null) {
				this.stecaClient = null;
				try {
					stecaClient.close();
				} catch (Exception e) {
					logger.error("Closing StecaClient failed: " + e, e);
				}
			}

			if (dataCollectorTimerTask != null) {
				dataCollectorTimerTask.cancel();
				dataCollectorTimerTask = null;
			}

			if (dataCollectorTimer != null) {
				dataCollectorTimer.cancel();
				dataCollectorTimer = null;
			}
		});
	}

	protected void onDataCollectorTimerTaskRun() {
		final StecaClient stecaClient = requireNonNull(this.stecaClient, "stecaClient");

		DataCollectorEvent event;
		try {
			final InverterMode inverterMode = stecaClient.execute(new GetInverterMode());
			final InverterStatus inverterStatus = stecaClient.execute(new GetInverterStatus());
			event = new DataCollectorEvent(this, inverterMode, inverterStatus);
			consecutiveErrorCount = 0;
		} catch (Exception x) {
			++consecutiveErrorCount;
			logger.error(getBeanInstanceName() + ".onDataCollectorTimerTaskRun: consecutiveErrorCount=" + consecutiveErrorCount + ": " + x, x);
			event = new DataCollectorEvent(this, x, consecutiveErrorCount);
		}
		final DataCollectorEvent _event = event;
		invokeLater(() -> fireDataCollectorEvent(_event));
	}

	protected void requestResetUsbAndExit() {
		logger.warn("{}.requestResetUsbAndExit: Entered.", getBeanInstanceName());
		try {
			File resetusbFile = new File(ConfigDir.getInstance().getFile(), "resetusb");
			resetusbFile.createNewFile();
			if (! resetusbFile.exists())
				throw new IOException("Could not create file: " + resetusbFile.getAbsolutePath());

			System.exit(99);
		} catch (Exception x) {
			logger.error(getBeanInstanceName() + ".requestResetUsbAndExit: " + x, x);
		}
	}

	protected void fireDataCollectorEvent(final DataCollectorEvent event) {
		requireNonNull(event, "event");

		for (final DataCollectorListener listener : dataCollectorListeners) {
			if (event.getError() == null)
				listener.onSuccess(event);
			else
				listener.onError(event);
		}
	}

	@Override
	public void addDataCollectorListener(final DataCollectorListener listener) {
		dataCollectorListeners.add(requireNonNull(listener, "listener"));
	}

	@Override
	public void removeDataCollectorListener(final DataCollectorListener listener) {
		dataCollectorListeners.remove(listener);
	}

	@Override
	public long getCollectPeriod() {
		return collectPeriod;
	}
	@Override
	public void setCollectPeriod(long collectPeriod) {
		setPropertyValue(PvDataCollector.PropertyEnum.collectPeriod, collectPeriod);
	}

	@Override
	public String getBeanName() {
		return beanName;
	}
	@Override
	public void setBeanName(String beanName) {
		this.beanName = beanName;
	}

	@Override
	public String getDeviceName() {
		return deviceName;
	}
	@Override
	public void setDeviceName(String deviceName) {
		setPropertyValue(PvDataCollector.PropertyEnum.deviceName, deviceName);
	}

	@Override
	public StecaInterfaceType getInterfaceType() {
		return interfaceType;
	}

	@Override
	public void setInterfaceType(StecaInterfaceType interfaceType) {
		setPropertyValue(PvDataCollector.PropertyEnum.interfaceType, interfaceType);
	}

	@Override
	public String getInterfaceAddress() {
		return interfaceAddress;
	}

	@Override
	public void setInterfaceAddress(String interfaceAddress) {
		setPropertyValue(PvDataCollector.PropertyEnum.interfaceAddress, interfaceAddress);
	}

	public int getResetUsbAfterConsecutiveErrorCount() {
		return resetUsbAfterConsecutiveErrorCount;
	}
	public void setResetUsbAfterConsecutiveErrorCount(int resetUsbAfterConsecutiveErrorCount) {
		this.resetUsbAfterConsecutiveErrorCount = resetUsbAfterConsecutiveErrorCount;
	}
}
