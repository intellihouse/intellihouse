package org.openhab.binding.intellihouse.rpc.pv;

import static java.util.Objects.*;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TimeZone;
import java.util.TreeMap;

import org.eclipse.smarthome.core.library.types.DateTimeType;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.types.State;
import org.eclipse.smarthome.core.types.UnDefType;
import org.openhab.binding.intellihouse.IntelliHouseBindingConstants;
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
    private Map<String, PvStatus> deviceName2CurrentPvStatus = Collections.synchronizedMap(new HashMap<>());
    private PvStatusClusterCalculator clusterCalculator = new PvStatusClusterCalculator(); // TODO make configurable...

    public PvStatusEventRpcService() {
        logger.info("<init>");
    }

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
                enlistCurrentPvStatus(pvStatus);
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
            final List<PvStatus> clusterPvStatuses = clusterCalculator.calculateClusters(new ArrayList<>(deviceName2CurrentPvStatus.values()));
            for (PvStatus pvStatus : clusterPvStatuses) {
                enlistCurrentPvStatus(pvStatus);
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

    protected void enlistCurrentPvStatus(final PvStatus pvStatus) throws Exception {
        requireNonNull(pvStatus, "pvStatus");
        synchronized (deviceName2CurrentPvStatus) {
            final String deviceName = requireNonNull(pvStatus.getDeviceName(), "pvStatus.deviceName");
            requireNonNull(pvStatus.getMeasured(), "pvStatus.measured");
            final PvStatus currentPvStatus = deviceName2CurrentPvStatus.get(deviceName);
            if (currentPvStatus != null && pvStatus.getMeasured().before(currentPvStatus.getMeasured())) {
                return; // nothing changed => return!
            }
            deviceName2CurrentPvStatus.put(deviceName, pvStatus);
        }

        final BeanInfo pvStatusBeanInfo = Introspector.getBeanInfo(PvStatus.class);
        for (final PropertyDescriptor propertyDescriptor : pvStatusBeanInfo.getPropertyDescriptors()) {
            if (propertyDescriptor.getPropertyType() == null) {
                continue;
            }
            final Method readMethod = propertyDescriptor.getReadMethod();
            if (readMethod == null) {
                continue;
            }
            if (handleStateUpdatedForPvDateTime(pvStatus, propertyDescriptor)) {
                continue;
            }
            if (handleStateUpdatedForPvNumber(pvStatus, propertyDescriptor)) {
                continue;
            }
            if (handleStateUpdatedForPvString(pvStatus, propertyDescriptor)) {
                continue;
            }
        }
    }

    protected boolean handleStateUpdatedForPvDateTime(final PvStatus pvStatus, final PropertyDescriptor propertyDescriptor) throws Exception {
        requireNonNull(pvStatus, "pvStatus");
        requireNonNull(propertyDescriptor, "propertyDescriptor");
        final Class<?> propertyType = requireNonNull(propertyDescriptor.getPropertyType(), "propertyDescriptor.propertyType");
        final Method readMethod = requireNonNull(propertyDescriptor.getReadMethod(), "propertyDescriptor.readMethod");
        if (Date.class.isAssignableFrom(propertyType)) {
            final Date value = (Date) readMethod.invoke(pvStatus);
            final State state;
            if (value == null) {
                state = UnDefType.NULL;
            } else {
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(value);
                ZonedDateTime zonedDateTime = ZonedDateTime
                        .ofInstant(calendar.toInstant(), TimeZone.getDefault().toZoneId())
                        .withFixedOffsetZone();
                state = new DateTimeType(zonedDateTime);
            }
            stateUpdated(getPvDateTimeChannelUid(pvStatus, propertyDescriptor), state);
            return true;
        }
        if (Calendar.class.isAssignableFrom(propertyType)) {
            final Calendar value = (Calendar) readMethod.invoke(pvStatus);
            final State state;
            if (value == null) {
                state = UnDefType.NULL;
            } else {
                ZonedDateTime zonedDateTime = ZonedDateTime
                        .ofInstant(value.toInstant(), TimeZone.getDefault().toZoneId())
                        .withFixedOffsetZone();
                state = new DateTimeType(zonedDateTime);
            }
            stateUpdated(getPvDateTimeChannelUid(pvStatus, propertyDescriptor), state);
            return true;
        }
        return false;
    }

    protected boolean handleStateUpdatedForPvNumber(final PvStatus pvStatus, final PropertyDescriptor propertyDescriptor) throws Exception {
        requireNonNull(pvStatus, "pvStatus");
        requireNonNull(propertyDescriptor, "propertyDescriptor");
        final Class<?> propertyType = requireNonNull(propertyDescriptor.getPropertyType(), "propertyDescriptor.propertyType");
        final Method readMethod = requireNonNull(propertyDescriptor.getReadMethod(), "propertyDescriptor.readMethod");
        if (propertyType == byte.class || propertyType == Byte.class) {
            final Byte value = (Byte) readMethod.invoke(pvStatus);
            stateUpdated(getPvNumberChannelUid(pvStatus, propertyDescriptor),
                    value == null ? UnDefType.NULL : new DecimalType(value));
            return true;
        }
        if (propertyType == short.class || propertyType == Short.class) {
            final Short value = (Short) readMethod.invoke(pvStatus);
            stateUpdated(getPvNumberChannelUid(pvStatus, propertyDescriptor),
                    value == null ? UnDefType.NULL : new DecimalType(value));
            return true;
        }
        if (propertyType == int.class || propertyType == Integer.class) {
            final Integer value = (Integer) readMethod.invoke(pvStatus);
            stateUpdated(getPvNumberChannelUid(pvStatus, propertyDescriptor),
                    value == null ? UnDefType.NULL : new DecimalType(value));
            return true;
        }
        if (propertyType == long.class || propertyType == Long.class) {
            final Long value = (Long) readMethod.invoke(pvStatus);
            stateUpdated(getPvNumberChannelUid(pvStatus, propertyDescriptor),
                    value == null ? UnDefType.NULL : new DecimalType(value));
            return true;
        }
        if (propertyType == float.class || propertyType == Float.class) {
            final Float value = (Float) readMethod.invoke(pvStatus);
            stateUpdated(getPvNumberChannelUid(pvStatus, propertyDescriptor),
                    value == null ? UnDefType.NULL : new DecimalType(value));
            return true;
        }
        if (propertyType == double.class || propertyType == Double.class) {
            final Double value = (Double) readMethod.invoke(pvStatus);
            stateUpdated(getPvNumberChannelUid(pvStatus, propertyDescriptor),
                    value == null ? UnDefType.NULL : new DecimalType(value));
            return true;
        }
        return false;
    }

    protected boolean handleStateUpdatedForPvString(final PvStatus pvStatus, final PropertyDescriptor propertyDescriptor) throws Exception {
        requireNonNull(pvStatus, "pvStatus");
        requireNonNull(propertyDescriptor, "propertyDescriptor");
        final Class<?> propertyType = requireNonNull(propertyDescriptor.getPropertyType(), "propertyDescriptor.propertyType");
        final Method readMethod = requireNonNull(propertyDescriptor.getReadMethod(), "propertyDescriptor.readMethod");
        if (propertyType == String.class) {
            final String value = (String) readMethod.invoke(pvStatus);
            stateUpdated(getPvStringChannelUid(pvStatus, propertyDescriptor),
                    value == null ? UnDefType.NULL : new StringType(value));
            return true;
        }
        return false;
    }

    protected ChannelUID getPvDateTimeChannelUid(final PvStatus pvStatus, final PropertyDescriptor propertyDescriptor) {
        requireNonNull(pvStatus, "pvStatus");
        requireNonNull(propertyDescriptor, "propertyDescriptor");
        final ThingUID thingUid = new ThingUID(IntelliHouseBindingConstants.THING_TYPE_PV_DATE_TIME, pvStatus.getDeviceName());
        return new ChannelUID(thingUid, propertyDescriptor.getName());
    }

    protected ChannelUID getPvNumberChannelUid(final PvStatus pvStatus, final PropertyDescriptor propertyDescriptor) {
        requireNonNull(pvStatus, "pvStatus");
        requireNonNull(propertyDescriptor, "propertyDescriptor");
        final ThingUID thingUid = new ThingUID(IntelliHouseBindingConstants.THING_TYPE_PV_NUMBER, pvStatus.getDeviceName());
        return new ChannelUID(thingUid, propertyDescriptor.getName());
    }

    protected ChannelUID getPvStringChannelUid(final PvStatus pvStatus, final PropertyDescriptor propertyDescriptor) {
        requireNonNull(pvStatus, "pvStatus");
        requireNonNull(propertyDescriptor, "propertyDescriptor");
        final ThingUID thingUid = new ThingUID(IntelliHouseBindingConstants.THING_TYPE_PV_STRING, pvStatus.getDeviceName());
        return new ChannelUID(thingUid, propertyDescriptor.getName());
    }

    protected SortedMap<String, Method> getPropertyName2Getter(final Class<?> clazz) throws Exception {
        requireNonNull(clazz, "clazz");

        final SortedMap<String, Method> propertyName2Getter = new TreeMap<>();
        for (Method method : clazz.getMethods()) {
            if (method.getParameterTypes().length == 0
                    && (method.getName().startsWith("get") || method.getName().startsWith("is"))) {

            }
        }
        return propertyName2Getter;
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
