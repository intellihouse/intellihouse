package org.openhab.binding.intellihouse.handler;

import org.eclipse.smarthome.core.library.types.PercentType;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import house.intelli.core.rpc.RpcClient;
import house.intelli.core.rpc.RpcContext;
import house.intelli.core.rpc.dimmer.DimmerActorReadRequest;
import house.intelli.core.rpc.dimmer.DimmerActorReadResponse;
import house.intelli.core.rpc.dimmer.DimmerActorWriteRequest;
import house.intelli.core.rpc.dimmer.DimmerActorWriteResponse;

public class IntelliHouseDimmerHandler extends IntelliHouseHandler {

    private Logger logger = LoggerFactory.getLogger(IntelliHouseDimmerHandler.class);

    public IntelliHouseDimmerHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        logger.info("handleCommand: channelUID={}, command={}", channelUID, command);
        final RpcContext rpcContext = getRpcContextOrFail();
        final String channelId = channelUID.getIdWithoutGroup();
        if (command instanceof RefreshType) {
            try {
                readAndUpdateState(channelUID);
            } catch (Exception e) {
                logger.error("DimmerActorReadRequest for channelUID=" + channelUID + " failed: " + e, e);
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.toString());
            }
        } else if (command instanceof PercentType) {
            PercentType percent = (PercentType) command;
            DimmerActorWriteRequest request = new DimmerActorWriteRequest();
            request.setServerHostId(getServerHostId());
            request.setChannelId(channelId);
            request.setDimmerValue(percent.intValue());
            DimmerActorWriteResponse response = null;
            try (RpcClient rpcClient = rpcContext.createRpcClient()) {
                response = rpcClient.invoke(request);
            } catch (Exception e) {
                logger.error("DimmerActorWriteRequest for channelUID=" + channelUID + " failed: " + e, e);
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.toString());
            }
            if (response != null) {
                PercentType responsePercent = new PercentType(response.getDimmerValue());
                updateStatus(ThingStatus.ONLINE);
                updateState(channelUID, responsePercent);
            }
        }
    }

    @Override
    protected void initializeChannel(ChannelUID channelUID) throws Exception {
        super.initializeChannel(channelUID);
        readAndUpdateState(channelUID);
    }

    private void readAndUpdateState(ChannelUID channelUID) throws Exception {
        final String channelId = channelUID.getIdWithoutGroup();
        final RpcContext rpcContext = getRpcContextOrFail();
        DimmerActorReadRequest request = new DimmerActorReadRequest();
        request.setServerHostId(getServerHostId());
        request.setChannelId(channelId);
        DimmerActorReadResponse response = null;
        try (RpcClient rpcClient = rpcContext.createRpcClient()) {
            response = rpcClient.invoke(request);
        }
        if (response != null) {
            PercentType responsePercent = new PercentType(response.getDimmerValue());
            updateStatus(ThingStatus.ONLINE);
            updateState(channelUID, responsePercent);
        }
    }
}
