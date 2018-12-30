package org.openhab.binding.intellihouse.rpc.pv;

import static java.util.Objects.*;

import org.openhab.binding.intellihouse.jdo.JdoPersistenceService;
import org.openhab.binding.intellihouse.rpc.ThingRpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import house.intelli.core.rpc.VoidResponse;
import house.intelli.core.rpc.pv.PvStatus;
import house.intelli.core.rpc.pv.PvStatusEventRequest;
import house.intelli.jdo.IntelliHouseTransaction;
import house.intelli.jdo.model.PvStatusDao;
import house.intelli.jdo.model.PvStatusEntity;

public class PvStatusEventRpcService extends ThingRpcService<PvStatusEventRequest, VoidResponse> {

    private final Logger logger = LoggerFactory.getLogger(PvStatusEventRpcService.class);

//    private BundleContext bundleContext;
    private JdoPersistenceService jdoPersistenceService;

//    @SuppressWarnings("rawtypes")
//    private ServiceTracker jdoPersistenceServiceTracker;

    public PvStatusEventRpcService() {
        logger.info("<init>");
    }

//    @SuppressWarnings({ "unchecked", "rawtypes" })
//    protected void activate(Map<String, Object> configProps) {
//        bundleContext = IntelliHouseActivator.getInstance().getBundleContext();
//
//        jdoPersistenceServiceTracker = new ServiceTracker(bundleContext, JdoPersistenceService.class.getName(), null) {
//            @Override
//            public Object addingService(final @Nullable ServiceReference reference) {
//                jdoPersistenceService = (JdoPersistenceService) bundleContext.getService(reference);
//                return jdoPersistenceService;
//            }
//
//            @Override
//            public void removedService(final @Nullable ServiceReference reference, final @Nullable Object service) {
//                jdoPersistenceService = null;
//            }
//        };
//        jdoPersistenceServiceTracker.open();
//    }
//
//    protected void deactivate() {
//        @SuppressWarnings("rawtypes")
//        ServiceTracker st = jdoPersistenceServiceTracker;
//        if (st != null) {
//            st.close();
//        }
//        jdoPersistenceServiceTracker = null;
//    }

    @Override
    public VoidResponse process(PvStatusEventRequest request) throws Exception {
        logger.debug("process: request={}", request);
        final JdoPersistenceService jps = requireNonNull(jdoPersistenceService, "jdoPersistenceService");
        try (final IntelliHouseTransaction tx = jps.beginTransaction()) {
            logger.debug("process: Started transaction.");
            final PvStatusDao pvStatusDao = tx.getDao(PvStatusDao.class);
            int count = 0;
            for (final PvStatus pvStatus : request.getPvStatuses()) {
                ++count;
                PvStatusEntity entity = pvStatusDao.getPvStatusEntity(pvStatus.getDeviceName(), pvStatus.getMeasured());
                logger.debug("process: Read entity: {}", entity);
                if (entity == null) {
                    entity = new PvStatusEntity();
                }
                updatePvStatusEntity(entity, pvStatus);
                pvStatusDao.makePersistent(entity);
                if (count % 100 == 0) {
                    logger.debug("process: Persisted {} PvStatusEntity objects. Flushing...", count);
                    tx.flush();
                    logger.debug("process: Persisted {} PvStatusEntity objects. Flushed.", count);
                }
            }
            logger.debug("process: Committing transaction...");
            tx.commit();
            logger.debug("process: Committed transaction.");
        }
        return null;
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

    public JdoPersistenceService getJdoPersistenceService() {
        return jdoPersistenceService;
    }
    public void setJdoPersistenceService(JdoPersistenceService jdoPersistenceService) {
        this.jdoPersistenceService = jdoPersistenceService;
    }
    public void unsetJdoPersistenceService(JdoPersistenceService jdoPersistenceService) {
        this.jdoPersistenceService = null;
    }
}
