package house.intelli.jdo.test;

import static java.util.Objects.*;

import java.io.InputStream;
import java.util.Properties;
import java.util.zip.GZIPInputStream;

import javax.jdo.JDOHelper;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;
import javax.jdo.Transaction;
import javax.xml.bind.Unmarshaller;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import house.intelli.core.jaxb.IntelliHouseJaxbContext;
import house.intelli.core.rpc.pv.PvStatus;
import house.intelli.core.rpc.pv.PvStatusList;
import house.intelli.jdo.IntelliHouseTransaction;
import house.intelli.jdo.IntelliHouseTransactionImpl;
import house.intelli.jdo.model.PvStatusDao;
import house.intelli.jdo.model.PvStatusEntity;

@Ignore("Only for manual tests")
public class ImportPvStatusTest {

	private static final Logger logger = LoggerFactory.getLogger(ImportPvStatusTest.class);

	protected PersistenceManagerFactory pmf;

	@Before
	public void before() throws Exception {
		pmf = initPmf();
	}

	@After
	public void after() throws Exception {
		if (pmf != null)
			pmf.close();

		pmf = null;
	}

	@Test
	public void importPvStatusFiles() throws Exception {
		PvStatusList pvStatusList;
		InputStream fin = ImportPvStatusTest.class.getResourceAsStream("/pvStatusList.2018-12-30_19-50-35.075.xml.gz");
		try (final GZIPInputStream in = new GZIPInputStream(fin)) {
			final Unmarshaller unmarshaller = IntelliHouseJaxbContext.getJaxbContext().createUnmarshaller();
			pvStatusList = (PvStatusList) unmarshaller.unmarshal(in);
		}

		try (IntelliHouseTransaction tx = new IntelliHouseTransactionImpl(pmf)) {
			logger.debug("process: Started transaction.");
      final PvStatusDao pvStatusDao = tx.getDao(PvStatusDao.class);
      int i = -1;
      for (final PvStatus pvStatus : pvStatusList.getPvStatuses()) {
          ++i;
          PvStatusEntity entity = pvStatusDao.getPvStatusEntity(pvStatus.getDeviceName(), pvStatus.getMeasured());
          if (entity == null) {
              entity = new PvStatusEntity();
          }
          updatePvStatusEntity(entity, pvStatus);
          pvStatusDao.makePersistent(entity);
          if (i % 100 == 0) {
              logger.debug("process: Persisted {} PvStatusEntity objects. Flushing...");
              tx.flush();
              logger.debug("process: Persisted {} PvStatusEntity objects. Flushed.");
          }
      }
      logger.debug("process: Committing transaction...");
      tx.commit();
      logger.debug("process: Committed transaction...");
		}
	}

  protected void updatePvStatusEntity(final PvStatusEntity entity, final PvStatus pvStatus) {
      requireNonNull(entity, "entity");
      requireNonNull(pvStatus, "pvStatus");

      entity.setDeviceName(pvStatus.getDeviceName());
      entity.setMeasured(pvStatus.getMeasured());
      entity.setDeviceMode(pvStatus.getDeviceMode());
      entity.setAcInVoltage(pvStatus.getAcInVoltage());
      entity.setAcInFrequency(pvStatus.getAcInFrequency());
      entity.setAcOutVoltage(pvStatus.getAcOutVoltage());
      entity.setAcOutFrequency(pvStatus.getAcOutFrequency());
      entity.setAcOutApparentPower(pvStatus.getAcOutApparentPower());
      entity.setAcOutActivePower(pvStatus.getAcOutActivePower());
      entity.setAcOutLoadPercentage(pvStatus.getAcOutLoadPercentage());
      entity.setInternalBusVoltage(pvStatus.getInternalBusVoltage());
      entity.setBatteryVoltageAtInverter(pvStatus.getBatteryVoltageAtInverter());
      entity.setBatteryChargeCurrent(pvStatus.getBatteryChargeCurrent());
      entity.setBatteryCapacityPercentage(pvStatus.getBatteryCapacityPercentage());
      entity.setHeatSinkTemperature(pvStatus.getHeatSinkTemperature());
      entity.setPvToBatteryCurrent(pvStatus.getPvToBatteryCurrent());
      entity.setPvVoltage(pvStatus.getPvVoltage());
      entity.setBatteryVoltageAtCharger(pvStatus.getBatteryVoltageAtCharger());
      entity.setBatteryDischargeCurrent(pvStatus.getBatteryDischargeCurrent());
      entity.setStatusBitmask(pvStatus.getStatusBitmask());
      entity.setEepromVersion(pvStatus.getEepromVersion());
      entity.setPvPower(pvStatus.getPvPower());
  }

	protected PersistenceManagerFactory initPmf() throws Exception {
		Properties persistenceProperties = new Properties();
		InputStream in = ImportPvStatusTest.class.getResourceAsStream("/test-pg-persistence.properties");
		persistenceProperties.load(in);
		in.close();
		persistenceProperties.setProperty("javax.jdo.PersistenceManagerFactoryClass", org.datanucleus.api.jdo.JDOPersistenceManagerFactory.class.getName());
		PersistenceManagerFactory pmf = JDOHelper.getPersistenceManagerFactory(persistenceProperties);

		final PersistenceManager pm = pmf.getPersistenceManager();
		try {
			final Transaction tx = pm.currentTransaction();
			tx.begin();
			try {
				initEntityClasses(pm);
				tx.commit();
			} finally {
				if (tx.isActive()) {
					tx.rollback();
				}
			}
		} finally {
			pm.close();
		}

		return pmf;
	}

  protected void initEntityClasses(final PersistenceManager pm) {
      pm.getExtent(PvStatusEntity.class);
  }

}
