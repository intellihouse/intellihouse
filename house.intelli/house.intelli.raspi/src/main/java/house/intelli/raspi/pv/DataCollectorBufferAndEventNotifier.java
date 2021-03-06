package house.intelli.raspi.pv;

import static house.intelli.core.event.EventQueue.*;
import static house.intelli.core.util.AssertUtil.*;
import static java.util.Objects.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import house.intelli.core.config.ConfigDir;
import house.intelli.core.jaxb.IntelliHouseJaxbContext;
import house.intelli.core.rpc.HostId;
import house.intelli.core.rpc.RpcClient;
import house.intelli.core.rpc.RpcContext;
import house.intelli.core.rpc.pv.PvStatus;
import house.intelli.core.rpc.pv.PvStatusEventRequest;
import house.intelli.core.rpc.pv.PvStatusList;
import house.intelli.raspi.PvDataCollector;
import house.intelli.raspi.pv.steca.InverterMode;
import house.intelli.raspi.pv.steca.InverterStatus;

@Component
public class DataCollectorBufferAndEventNotifier {

	private static final Logger logger = LoggerFactory.getLogger(DataCollectorBufferAndEventNotifier.class);

	private ApplicationContext applicationContext;

	private RpcContext rpcContext;

	private List<PvDataCollector> pvDataCollectors = Collections.emptyList();

	private PvStatusList pvStatusList = new PvStatusList();

	private Timer notifyTimer;

	private TimerTask notifyTimerTask;

	private long notifyPeriod = 30000L;

	private File bufferDir;

//	private static final String DIR_DATE_FORMAT = "yyyy/MM/dd/HH/mm";
	private static final String DIR_DATE_FORMAT = "yyyy/MM/dd/HH";
	private static final String FILE_NAME_DATE_FORMAT = "yyyy-MM-dd_HH-mm-ss.SSS";
	private static final String FILE_NAME_PREFIX = "pvStatusList.";
	private static final String FILE_NAME_SUFFIX = ".xml.gz";
	private static final String TMP_SUFFIX = ".tmp";
	private Thread shutdownHook;

	private final DataCollectorListener dataCollectorListener = new DataCollectorListener() {

		@Override
		public void onSuccess(final DataCollectorEvent event) {
			assertEventThread();
			requireNonNull(event, "event");
			final PvStatus pvStatus = createPvStatus(event);
			pvStatusList.getPvStatuses().add(pvStatus);
		}

		@Override
		public void onError(final DataCollectorEvent event) {
			assertEventThread();
			requireNonNull(event, "event");
			// nothing to do
		}
	};

	public DataCollectorBufferAndEventNotifier() {
	}

	public void init() {
		assertEventThread();

		if (notifyTimerTask != null)
			notifyTimerTask.cancel();

		if (notifyTimer != null)
			notifyTimer.cancel();

		notifyTimer = new Timer(getClass().getSimpleName() + ".notifyTimer");

		notifyTimerTask = new TimerTask() {
			@Override
			public void run() {
				try {
					notifyPvStatusList(true);
				} catch (Throwable x) {
					logger.error("notifyTimerTask.run: " + x, x);
				}
			}
		};

		notifyTimer.schedule(notifyTimerTask, notifyPeriod, notifyPeriod);

		if (shutdownHook == null) {
			shutdownHook = new Thread() {
				@Override
				public void run() {
					try {
						onShutdown();
					} catch (Throwable x) {
						logger.error("shutdownHook.run: " + x, x);
					}
				}
			};
			Runtime.getRuntime().addShutdownHook(shutdownHook);
		}
	}

	protected void onShutdown() throws Exception {
		logger.info("onShutdown: Invoking notifyPvStatusList...");
		notifyPvStatusList(false);
		logger.info("onShutdown: Invoked notifyPvStatusList.");
	}

	protected PvStatusList rollPvStatusList() {
		final PvStatusList[] result = new PvStatusList[1];

		invokeAndWait(new Runnable() {
			@Override
			public void run() {
				result[0] = pvStatusList;
				pvStatusList = new PvStatusList();
			}
		});

		return requireNonNull(result[0], "result[0]");
	}

	protected void notifyPvStatusList(boolean sendOld) throws Exception {
		final PvStatusList pvStatusList = rollPvStatusList();

		if (! pvStatusList.getPvStatuses().isEmpty()) {
			try {
				sendPvStatusListToServer(pvStatusList);
			} catch (Throwable x) {
				logger.error("notifyPvStatusList: " + x, x);
				storePvStatusListLocally(pvStatusList);
				return;
			}
		}
		if (sendOld)
			sendOldPvStatusListsToServer();
	}

	protected void sendPvStatusListToServer(final PvStatusList pvStatusList) throws Exception {
		requireNonNull(pvStatusList, "pvStatusList");

		if (pvStatusList.getPvStatuses().isEmpty()) {
			logger.warn("sendPvStatusListToServer: pvStatusList.pvStatuses is empty! Skipping.");
			return;
		}

		final PvStatusEventRequest request = new PvStatusEventRequest();
		request.setServerHostId(HostId.SERVER);
		request.setPvStatuses(pvStatusList.getPvStatuses());
		try (RpcClient rpcClient = rpcContext.createRpcClient()) {
			rpcClient.invoke(request);
		}
	}

	protected void storePvStatusListLocally(final PvStatusList pvStatusList) throws Exception {
		requireNonNull(pvStatusList, "pvStatusList");
		final Date now = new Date();

		final String dirStr = new SimpleDateFormat(DIR_DATE_FORMAT).format(now);
		final File dir = new File(getBufferDir(), dirStr);
		if (! dir.isDirectory())
			dir.mkdirs();

		if (! dir.isDirectory())
			throw new IOException("Could not create directory: " + dir.getAbsolutePath());

		final String fileName = FILE_NAME_PREFIX
				+ new SimpleDateFormat(FILE_NAME_DATE_FORMAT).format(now)
				+ FILE_NAME_SUFFIX;

		final File file = new File(dir, fileName);
		final File tmpFile = new File(dir, fileName + TMP_SUFFIX);

		try (final GZIPOutputStream out = new GZIPOutputStream(new FileOutputStream(tmpFile))) {
			final Marshaller marshaller = IntelliHouseJaxbContext.getJaxbContext().createMarshaller();
			marshaller.marshal(pvStatusList, out);
		}

		tmpFile.renameTo(file);
	}

	public File getBufferDir() {
		if (bufferDir == null) {
			bufferDir = new File(ConfigDir.getInstance().getFile(), "pvStatus");
			if (! bufferDir.isDirectory())
				bufferDir.mkdir();

			if (! bufferDir.isDirectory())
				throw new RuntimeException(new IOException("Could not create directory: " + bufferDir.getAbsolutePath()));
		}
		return bufferDir;
	}

	protected void sendOldPvStatusListsToServer() throws Exception {
		sendOldPvStatusListsToServer(System.currentTimeMillis(), getBufferDir());
	}

	protected boolean sendOldPvStatusListsToServer(long sendOldPvStatusListsToServerStartTimestamp, final File dirOrFile) throws Exception {
		requireNonNull(dirOrFile);
		final long duration = System.currentTimeMillis() - sendOldPvStatusListsToServerStartTimestamp;
		if (duration > notifyPeriod) {
			logger.info("sendOldPvStatusListsToServer: duration={} exceeds max={} => Aborting and postponing.", duration, notifyPeriod);
			return false;
		}
		if (dirOrFile.isDirectory()) {
			final File[] children = dirOrFile.listFiles();
			if (children != null) {
				Arrays.sort(children, fileNameComparator);
				for (File child : children) {
					if (! sendOldPvStatusListsToServer(sendOldPvStatusListsToServerStartTimestamp, child))
						return false;

					File[] children2 = child.listFiles();
					if (children2 == null || children2.length == 0)
						child.delete(); // delete empty directories
				}
			}
		}
		else if (dirOrFile.isFile() && dirOrFile.getName().endsWith(FILE_NAME_SUFFIX)) {
			if (dirOrFile.length() == 0) {
				logger.warn("sendOldPvStatusListsToServer: Skipping and deleting empty file: {}", dirOrFile.getAbsolutePath());
			}
			else {
				final PvStatusList pvStatusList;
				try (final GZIPInputStream in = new GZIPInputStream(new FileInputStream(dirOrFile))) {
					final Unmarshaller unmarshaller = IntelliHouseJaxbContext.getJaxbContext().createUnmarshaller();
					pvStatusList = (PvStatusList) unmarshaller.unmarshal(in);
				}
				sendPvStatusListToServer(pvStatusList);
			}
			// no exception => successfully stored => delete file.
			dirOrFile.delete();
		}
		return true;
	}

	private final Comparator<File> fileNameComparator = new Comparator<File>() {
		@Override
		public int compare(File f1, File f2) {
			final String name1 = f1.getName();
			final String name2 = f2.getName();
			return name1.compareTo(name2);
		}
	};

	protected PvStatus createPvStatus(final DataCollectorEvent event) {
		requireNonNull(event, "event");
		final InverterMode inverterMode = requireNonNull(event.getInverterMode(), "event.inverterMode");
		final InverterStatus inverterStatus = requireNonNull(event.getInverterStatus(), "event.inverterStatus");

		final PvStatus pvStatus = new PvStatus();
		pvStatus.setDeviceName(event.getSource().getDeviceName());
		pvStatus.setMeasured(inverterStatus.getMeasured());
		pvStatus.setDeviceMode(new String(new char[] { inverterMode.getMode() }));
		pvStatus.setAcInVoltage(inverterStatus.getAcInVoltage());
		pvStatus.setAcInFrequency(inverterStatus.getAcInFrequency());
		pvStatus.setAcOutVoltage(inverterStatus.getAcOutVoltage());
		pvStatus.setAcOutFrequency(inverterStatus.getAcOutFrequency());
		pvStatus.setAcOutApparentPower(inverterStatus.getAcOutApparentPower());
		pvStatus.setAcOutActivePower(inverterStatus.getAcOutActivePower());
		pvStatus.setAcOutLoadPercentage(inverterStatus.getAcOutLoadPercentage());
		pvStatus.setInternalBusVoltage(inverterStatus.getInternalBusVoltage());
		pvStatus.setBatteryVoltageAtInverter(inverterStatus.getBatteryVoltageAtInverter());
		pvStatus.setBatteryChargeCurrent(inverterStatus.getBatteryChargeCurrent());
		pvStatus.setBatteryCapacityPercentage(inverterStatus.getBatteryCapacityPercentage());
		pvStatus.setHeatSinkTemperature(inverterStatus.getHeatSinkTemperature());
		pvStatus.setPvToBatteryCurrent(inverterStatus.getPvToBatteryCurrent());
		pvStatus.setPvVoltage(inverterStatus.getPvVoltage());
		pvStatus.setBatteryVoltageAtCharger(inverterStatus.getBatteryVoltageAtCharger());
		pvStatus.setBatteryDischargeCurrent(inverterStatus.getBatteryDischargeCurrent());
		pvStatus.setStatusBitmask(inverterStatus.getStatusBitmask());
		pvStatus.setEepromVersion(inverterStatus.getEepromVersion());
		pvStatus.setPvPower(inverterStatus.getPvPower());
		return pvStatus;
	}

	public long getNotifyPeriod() {
		return notifyPeriod;
	}
	public void setNotifyPeriod(long notifyPeriod) {
		this.notifyPeriod = notifyPeriod;
	}

	public ApplicationContext getApplicationContext() {
		return applicationContext;
	}

	@Autowired
	public void setApplicationContext(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	public RpcContext getRpcContext() {
		return rpcContext;
	}

	@Autowired
	public void setRpcContext(RpcContext rpcContext) {
		this.rpcContext = rpcContext;
	}

	public List<PvDataCollector> getPvDataCollectors() {
		return pvDataCollectors;
	}

	@Autowired(required = false)
	public void setPvDataCollectors(final List<PvDataCollector> pvDataCollectors) {
		assertEventThread();
		logger.debug("setKeyButtonSensors: pvDataCollectors={}", pvDataCollectors);

		final List<PvDataCollector> oldStecaDataCollectors = this.pvDataCollectors;
		for (final PvDataCollector pvDataCollector : oldStecaDataCollectors)
			pvDataCollector.removeDataCollectorListener(dataCollectorListener);

		if (pvDataCollectors == null)
			this.pvDataCollectors = Collections.emptyList();
		else {
			for (final PvDataCollector pvDataCollector : pvDataCollectors)
				pvDataCollector.addDataCollectorListener(dataCollectorListener);

			this.pvDataCollectors = Collections.unmodifiableList(new ArrayList<>(pvDataCollectors));
		}
	}

}
