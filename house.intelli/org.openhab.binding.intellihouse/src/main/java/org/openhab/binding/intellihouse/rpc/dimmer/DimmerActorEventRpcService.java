package org.openhab.binding.intellihouse.rpc.dimmer;

import static org.openhab.binding.intellihouse.IntelliHouseBindingConstants.THING_TYPE_DIMMER;

import org.eclipse.smarthome.core.library.types.PercentType;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.openhab.binding.intellihouse.rpc.ChannelRpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import house.intelli.core.rpc.VoidResponse;
import house.intelli.core.rpc.dimmer.DimmerActorEventRequest;

public class DimmerActorEventRpcService extends ChannelRpcService<DimmerActorEventRequest, VoidResponse> {

    private final Logger logger = LoggerFactory.getLogger(DimmerActorEventRpcService.class);

    public DimmerActorEventRpcService() {
        logger.debug("<init>");
    }

    @Override
    public VoidResponse process(DimmerActorEventRequest request) throws Exception {
        logger.debug("process: request={}", request);
        PercentType percent = new PercentType(request.getDimmerValue());
        for (ChannelUID channelUID : getChannelUIDs(THING_TYPE_DIMMER, request)) {
            stateUpdated(channelUID, percent);
        }
        return null;
    }
}
