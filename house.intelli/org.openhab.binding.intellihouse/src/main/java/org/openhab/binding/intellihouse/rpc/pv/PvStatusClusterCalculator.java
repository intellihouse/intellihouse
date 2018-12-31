package org.openhab.binding.intellihouse.rpc.pv;

import static java.util.Objects.*;

import java.util.ArrayList;
import java.util.List;

import house.intelli.core.rpc.pv.PvStatus;

// TODO this should be configurable! Currently, it manages just 1 single cluster and assumes that all devices belong to it.
public class PvStatusClusterCalculator {

    // TODO should be a list of clusters with assignments of individual machines.
    protected static final String clusterDeviceName = "stecaCluster";

    public PvStatusClusterCalculator() {
    }

    public List<PvStatus> calculateClusters(final List<PvStatus> currentPvStatuses) {
        requireNonNull(currentPvStatuses, "currentPvStatuses");
        final List<PvStatus> result = new ArrayList<>(1);

        final PvStatus clusterPvStatus = new PvStatus();
        if (! currentPvStatuses.isEmpty()) {
            result.add(clusterPvStatus);
            clusterPvStatus.setDeviceName(clusterDeviceName);
            clusterPvStatus.setPvPower(0);
            clusterPvStatus.setAcOutApparentPower(0);
            clusterPvStatus.setAcOutActivePower(0);
            clusterPvStatus.setBatteryChargeCurrent(0);
            clusterPvStatus.setBatteryDischargeCurrent(0);
        }
        // TODO check what other values have to be summed up.
        // TODO instead of the last-data-wins-strategy, maybe calculate an average or take the minimum/maximum of the values?!
        for (PvStatus pvStatus : currentPvStatuses) {
            if (isCluster(pvStatus)) {
                continue;
            }
            clusterPvStatus.setMeasured(pvStatus.getMeasured());
            clusterPvStatus.setDeviceMode(pvStatus.getDeviceMode()); // maybe better strategy? or does not matter, because the cluster is always in the same state, anyway?
            clusterPvStatus.setAcInVoltage(pvStatus.getAcInVoltage());
            clusterPvStatus.setAcInFrequency(pvStatus.getAcInFrequency());
            clusterPvStatus.setAcOutVoltage(pvStatus.getAcOutVoltage());
            clusterPvStatus.setAcOutFrequency(pvStatus.getAcOutFrequency());
            clusterPvStatus.setAcOutApparentPower(clusterPvStatus.getAcOutApparentPower() + pvStatus.getAcOutApparentPower());
            clusterPvStatus.setAcOutActivePower(clusterPvStatus.getAcOutActivePower() + pvStatus.getAcOutActivePower());
            clusterPvStatus.setAcOutLoadPercentage(pvStatus.getAcOutLoadPercentage());
            clusterPvStatus.setInternalBusVoltage(pvStatus.getInternalBusVoltage());
            clusterPvStatus.setBatteryVoltageAtInverter(pvStatus.getBatteryVoltageAtInverter());
            clusterPvStatus.setBatteryChargeCurrent(clusterPvStatus.getBatteryChargeCurrent() + pvStatus.getBatteryChargeCurrent());
            clusterPvStatus.setBatteryCapacityPercentage(pvStatus.getBatteryCapacityPercentage());
            clusterPvStatus.setHeatSinkTemperature(pvStatus.getHeatSinkTemperature());
            clusterPvStatus.setPvToBatteryCurrent(pvStatus.getPvToBatteryCurrent());
            clusterPvStatus.setPvVoltage(pvStatus.getPvVoltage()); // this should likely be an average.
            clusterPvStatus.setBatteryVoltageAtCharger(pvStatus.getBatteryVoltageAtCharger()); // minimum? maximum? average?
            clusterPvStatus.setBatteryDischargeCurrent(clusterPvStatus.getBatteryDischargeCurrent() + pvStatus.getBatteryDischargeCurrent());
            clusterPvStatus.setStatusBitmask(pvStatus.getStatusBitmask());
            clusterPvStatus.setEepromVersion(pvStatus.getEepromVersion());
            clusterPvStatus.setPvPower(clusterPvStatus.getPvPower() + pvStatus.getPvPower());
        }
        return result;
    }

    /**
     * Is the PV-status of a cluster -- not a member-device.
     * @param pvStatus
     * @return
     */
    protected boolean isCluster(PvStatus pvStatus) {
        return clusterDeviceName.equals(pvStatus.getDeviceName());
    }
}
