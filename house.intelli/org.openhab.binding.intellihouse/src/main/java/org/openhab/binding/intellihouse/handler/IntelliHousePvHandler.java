package org.openhab.binding.intellihouse.handler;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class IntelliHousePvHandler extends IntelliHouseHandler {

    private Logger logger = LoggerFactory.getLogger(IntelliHousePvHandler.class);

    public IntelliHousePvHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void handleCommand(@NonNull ChannelUID channelUID, @NonNull Command command) {
        logger.info("handleCommand: channelUID={}, command={}", channelUID, command);
//        final RpcContext rpcContext = getRpcContextOrFail();
//        final String channelId = channelUID.getIdWithoutGroup();
//        if (command instanceof RefreshType) {
//            try {
//                readAndUpdateState(channelUID);
//            } catch (Exception e) {
//                logger.error("DimmerActorReadRequest for channelUID=" + channelUID + " failed: " + e, e);
//                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.toString());
//            }
//        }
//        else
//        if (command instanceof DecimalType) {
//            updateStatus(ThingStatus.ONLINE);
//            updateState(channelUID, (DecimalType) command);
//        }
    }

    @Override
    protected void initializeChannel(ChannelUID channelUID) throws Exception {
        super.initializeChannel(channelUID);
//        readAndUpdateState(channelUID);
    }

//    private void readAndUpdateState(ChannelUID channelUID) throws Exception {
//        final String channelId = channelUID.getIdWithoutGroup();
//        final RpcContext rpcContext = getRpcContextOrFail();
//
//        DecimalType responsePercent = new DecimalType(response.getDimmerValue());
//        updateStatus(ThingStatus.ONLINE);
//        updateState(channelUID, responsePercent);
//    }
}
