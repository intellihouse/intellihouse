package org.openhab.binding.intellihouse.handler;

import org.eclipse.smarthome.core.library.types.OnOffType;
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
import house.intelli.core.rpc.relay.RelayActorReadRequest;
import house.intelli.core.rpc.relay.RelayActorReadResponse;
import house.intelli.core.rpc.relay.RelayActorWriteRequest;
import house.intelli.core.rpc.relay.RelayActorWriteResponse;

public class IntelliHouseSwitchHandler extends IntelliHouseHandler {

    private Logger logger = LoggerFactory.getLogger(IntelliHouseSwitchHandler.class);

    public IntelliHouseSwitchHandler(Thing thing) {
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
        } else if (command instanceof OnOffType) {
            OnOffType onOff = (OnOffType) command;
            RelayActorWriteRequest request = new RelayActorWriteRequest();
            request.setServerHostId(getServerHostId());
            request.setChannelId(channelId);
            request.setEnergized(OnOffType.ON.equals(onOff));
            RelayActorWriteResponse response = null;
            try (RpcClient rpcClient = rpcContext.createRpcClient()) {
                response = rpcClient.invoke(request);
            } catch (Exception e) {
                logger.error("RelayActorWriteRequest for channelUID=" + channelUID + " failed: " + e, e);
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.toString());
            }
            if (response != null) {
                OnOffType responseOnOff = response.isEnergized() ? OnOffType.ON : OnOffType.OFF;
                updateStatus(ThingStatus.ONLINE);
                updateState(channelUID, responseOnOff);
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
        RelayActorReadRequest request = new RelayActorReadRequest();
        request.setServerHostId(getServerHostId());
        request.setChannelId(channelId);
        RelayActorReadResponse response = null;
        try (RpcClient rpcClient = rpcContext.createRpcClient()) {
            response = rpcClient.invoke(request);
        }
        if (response != null) {
            OnOffType responseOnOff = response.isEnergized() ? OnOffType.ON : OnOffType.OFF;
            updateStatus(ThingStatus.ONLINE);
            updateState(channelUID, responseOnOff);
        }
    }
}
