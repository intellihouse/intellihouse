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

    private JdoPersistenceService jdoPersistenceService;

    public PvStatusEventRpcService() {
        logger.debug("<init>");
    }

    @Override
    public VoidResponse process(PvStatusEventRequest request) throws Exception {
        logger.debug("process: request={}", request);
        final JdoPersistenceService jps = requireNonNull(jdoPersistenceService, "jdoPersistenceService");
        try (final IntelliHouseTransaction tx = jps.beginTransaction()) {
            final PvStatusDao pvStatusDao = tx.getDao(PvStatusDao.class);
            for (final PvStatus pvStatus : request.getPvStatuses()) {
                PvStatusEntity entity = pvStatusDao.getPvStatusEntity(pvStatus.getDeviceName(), pvStatus.getMeasured());
                if (entity == null) {
                    entity = new PvStatusEntity();
                }
                updatePvStatusEntity(entity, pvStatus);
                pvStatusDao.makePersistent(entity);
            }
            tx.commit();
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
