package org.openhab.binding.intellihouse.handler;

import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import house.intelli.core.rpc.RpcContext;

public class IntelliHouseSwitchHandler extends IntelliHouseHandler {

    private Logger logger = LoggerFactory.getLogger(IntelliHouseSwitchHandler.class);

    public IntelliHouseSwitchHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        logger.info("handleCommand: channelUID={}, command={}", channelUID, command);
        final RpcContext rpcContext = getRpcContextOrFail();
        String channelId = channelUID.getIdWithoutGroup();
        // if (command instanceof PercentType) {
        // PercentType percent = (PercentType) command;
        // DimmerSetRequest request = new DimmerSetRequest();
        // request.setChannelId(channelId);
        // request.setDimmerValue(percent.intValue());
        // DimmerSetResponse response = null;
        // try (RpcClient rpcClient = rpcContext.createRpcClient()) {
        // response = rpcClient.invoke(request);
        // } catch (Exception e) {
        // logger.error("DimmerSetRequest for channelUID=" + channelUID + " failed: " + e, e);
        // updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.toString());
        // }
        // if (response != null) {
        // int dimmerValue = response.getDimmerValue();
        // percent = new PercentType(dimmerValue);
        // updateStatus(ThingStatus.ONLINE);
        // updateState(channelUID, percent);
        // }
        // }
        throw new UnsupportedOperationException("NYI");
    }
}
