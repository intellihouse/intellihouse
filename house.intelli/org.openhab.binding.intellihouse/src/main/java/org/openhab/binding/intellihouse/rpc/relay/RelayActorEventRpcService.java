package org.openhab.binding.intellihouse.rpc.relay;

import static org.openhab.binding.intellihouse.IntelliHouseBindingConstants.*;

import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.openhab.binding.intellihouse.rpc.ChannelRpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import house.intelli.core.rpc.VoidResponse;
import house.intelli.core.rpc.relay.RelayActorEventRequest;

public class RelayActorEventRpcService extends ChannelRpcService<RelayActorEventRequest, VoidResponse> {

    private final Logger logger = LoggerFactory.getLogger(RelayActorEventRpcService.class);

    public RelayActorEventRpcService() {
        logger.debug("<init>");
    }

    @Override
    public VoidResponse process(RelayActorEventRequest request) throws Exception {
        logger.debug("process: request={}", request);
        OnOffType onOff = request.isEnergized() ? OnOffType.ON : OnOffType.OFF;
        for (ChannelUID channelUID : getChannelUIDs(THING_TYPE_SWITCH, request)) {
            stateUpdated(channelUID, onOff);
        }
        return null;
    }
}
