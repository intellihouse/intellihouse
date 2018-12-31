package org.openhab.binding.intellihouse.rpc.pv;

import static java.util.Objects.*;

import java.util.ArrayList;
import java.util.List;

import house.intelli.core.rpc.pv.PvStatus;

// TODO this should be configurable! Currently, it manages just 1 single cluster and assumes that all devices belong to it.
public class PvStatusClusterCalculator {

    // TODO should be a list of clusters with assignments of individual machines -- and their names should be configurable, of course.
    protected static final String clusterDeviceName = "stecaCluster";

    public PvStatusClusterCalculator() {
    }

    public List<PvStatus> calculateClusters(final List<PvStatus> currentPvStatuses) {
        requireNonNull(currentPvStatuses, "currentPvStatuses");
        final List<PvStatus> result = new ArrayList<>(1);
        if (currentPvStatuses.isEmpty()) { // empty input => shortcut = return *empty* result.
            return result;
        }

        final PvStatus clusterPvStatus = new PvStatus();

        // initialize aggregated result
        clusterPvStatus.setDeviceName(clusterDeviceName);
        clusterPvStatus.setAcInVoltage(0); // average
        clusterPvStatus.setAcInFrequency(0); // average
        clusterPvStatus.setAcOutVoltage(0); // average
        clusterPvStatus.setAcOutFrequency(0); // average
        clusterPvStatus.setAcOutApparentPower(0); // sum
        clusterPvStatus.setAcOutActivePower(0); // sum
        clusterPvStatus.setAcOutLoadPercentage(Float.MIN_VALUE); // max
        clusterPvStatus.setInternalBusVoltage(0); // average
        clusterPvStatus.setBatteryChargeCurrent(0); // sum
        clusterPvStatus.setBatteryDischargeCurrent(0); // sum
        clusterPvStatus.setBatteryVoltageAtCharger(Float.MAX_VALUE); // min
        clusterPvStatus.setBatteryVoltageAtInverter(Float.MAX_VALUE); // min
        clusterPvStatus.setBatteryCapacityPercentage(Float.MAX_VALUE); // min
        clusterPvStatus.setHeatSinkTemperature(Float.MIN_VALUE); // max
        clusterPvStatus.setPvToBatteryCurrent(0); // sum
        clusterPvStatus.setPvVoltage(0); // average
        clusterPvStatus.setPvPower(0);

        int count = 0;
        for (PvStatus pvStatus : currentPvStatuses) {
            if (isCluster(pvStatus)) {
                continue;
            }
            ++count;
            clusterPvStatus.setMeasured(pvStatus.getMeasured());
            clusterPvStatus.setDeviceMode(pvStatus.getDeviceMode()); // maybe better strategy? or does not matter, because the cluster is always in the same state, anyway?
            clusterPvStatus.setAcInVoltage(clusterPvStatus.getAcInVoltage() + pvStatus.getAcInVoltage());
            clusterPvStatus.setAcInFrequency(clusterPvStatus.getAcInFrequency() + pvStatus.getAcInFrequency());
            clusterPvStatus.setAcOutVoltage(clusterPvStatus.getAcOutVoltage() + pvStatus.getAcOutVoltage());
            clusterPvStatus.setAcOutFrequency(clusterPvStatus.getAcOutFrequency() + pvStatus.getAcOutFrequency());
            clusterPvStatus.setAcOutApparentPower(clusterPvStatus.getAcOutApparentPower() + pvStatus.getAcOutApparentPower());
            clusterPvStatus.setAcOutActivePower(clusterPvStatus.getAcOutActivePower() + pvStatus.getAcOutActivePower());
            clusterPvStatus.setAcOutLoadPercentage(
                    Math.max(clusterPvStatus.getAcOutLoadPercentage(), pvStatus.getAcOutLoadPercentage()));
            clusterPvStatus.setInternalBusVoltage(clusterPvStatus.getInternalBusVoltage() + pvStatus.getInternalBusVoltage());
            clusterPvStatus.setBatteryVoltageAtCharger(
                    Math.min(clusterPvStatus.getBatteryVoltageAtCharger(), pvStatus.getBatteryVoltageAtCharger()));
            clusterPvStatus.setBatteryVoltageAtInverter(
                    Math.min(clusterPvStatus.getBatteryVoltageAtInverter(), pvStatus.getBatteryVoltageAtInverter()));
            clusterPvStatus.setBatteryChargeCurrent(clusterPvStatus.getBatteryChargeCurrent() + pvStatus.getBatteryChargeCurrent());
            clusterPvStatus.setBatteryCapacityPercentage(
                    Math.min(clusterPvStatus.getBatteryCapacityPercentage(), pvStatus.getBatteryCapacityPercentage()));
            clusterPvStatus.setHeatSinkTemperature(
                    Math.max(clusterPvStatus.getHeatSinkTemperature(), pvStatus.getHeatSinkTemperature()));
            clusterPvStatus.setPvToBatteryCurrent(clusterPvStatus.getPvToBatteryCurrent() +pvStatus.getPvToBatteryCurrent());
            clusterPvStatus.setPvVoltage(clusterPvStatus.getPvVoltage() + pvStatus.getPvVoltage());
            clusterPvStatus.setBatteryDischargeCurrent(clusterPvStatus.getBatteryDischargeCurrent() + pvStatus.getBatteryDischargeCurrent());
            clusterPvStatus.setStatusBitmask(pvStatus.getStatusBitmask());
            clusterPvStatus.setEepromVersion(pvStatus.getEepromVersion());
            clusterPvStatus.setPvPower(clusterPvStatus.getPvPower() + pvStatus.getPvPower());

        }
        if (count <= 0) { // none of the list was usable => return *empty* result.
            return result;
        }

        // average divions:
        clusterPvStatus.setAcInVoltage(clusterPvStatus.getAcInVoltage() / count);
        clusterPvStatus.setAcInFrequency(clusterPvStatus.getAcInFrequency() / count);
        clusterPvStatus.setAcOutVoltage(clusterPvStatus.getAcOutVoltage() / count);
        clusterPvStatus.setAcOutFrequency(clusterPvStatus.getAcOutFrequency() / count);
        clusterPvStatus.setInternalBusVoltage(clusterPvStatus.getInternalBusVoltage() / count);
        clusterPvStatus.setPvVoltage(clusterPvStatus.getPvVoltage() / count);

        result.add(clusterPvStatus);
        return result;
    }

    /**
     * Is the PV-status of a cluster -- not of a member-device.
     * @param pvStatus
     * @return
     */
    protected boolean isCluster(PvStatus pvStatus) {
        return clusterDeviceName.equals(pvStatus.getDeviceName());
    }
}
