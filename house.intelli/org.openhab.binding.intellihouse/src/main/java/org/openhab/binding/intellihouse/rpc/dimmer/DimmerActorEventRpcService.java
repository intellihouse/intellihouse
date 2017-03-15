package org.openhab.binding.intellihouse.rpc.dimmer;

import static org.openhab.binding.intellihouse.IntelliHouseBindingConstants.THING_TYPE_DIMMER;

import org.eclipse.smarthome.core.events.EventPublisher;
import org.eclipse.smarthome.core.library.types.PercentType;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.link.ItemChannelLinkRegistry;
import org.openhab.binding.intellihouse.rpc.ChannelRpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import house.intelli.core.rpc.VoidResponse;
import house.intelli.core.rpc.dimmer.DimmerActorEventRequest;

public class DimmerActorEventRpcService extends ChannelRpcService<DimmerActorEventRequest, VoidResponse> {

    private final Logger logger = LoggerFactory.getLogger(DimmerActorEventRpcService.class);

    protected EventPublisher eventPublisher;
    protected ItemChannelLinkRegistry itemChannelLinkRegistry;
    // protected ItemRegistry itemRegistry;

    public DimmerActorEventRpcService() {
        logger.debug("<init>");
    }

    public void setEventPublisher(EventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    public void unsetEventPublisher(EventPublisher eventPublisher) {
        this.eventPublisher = null;
    }

    public void setItemChannelLinkRegistry(ItemChannelLinkRegistry itemChannelLinkRegistry) {
        this.itemChannelLinkRegistry = itemChannelLinkRegistry;
    }

    public void unsetItemChannelLinkRegistry(ItemChannelLinkRegistry itemChannelLinkRegistry) {
        this.itemChannelLinkRegistry = null;
    }

    // public void setItemRegistry(ItemRegistry itemRegistry) {
    // this.itemRegistry = itemRegistry;
    // }
    //
    // public void unsetItemRegistry(ItemRegistry itemRegistry) {
    // this.itemRegistry = null;
    // }

    @Override
    public VoidResponse process(DimmerActorEventRequest request) throws Exception {
        logger.debug("process: request={}", request);
        PercentType percent = new PercentType(request.getDimmerValue());
        for (ChannelUID channelUID : getChannelUIDs(THING_TYPE_DIMMER, request)) {
            stateUpdated(channelUID, percent);
        }
        return null;
    }

    // protected void setThingStatus(Thing thing, ThingStatusInfo thingStatusInfo) {
    // assertNotNull(thing, "thing");
    // assertNotNull(thingStatusInfo, "thingStatusInfo");
    // ThingStatusInfo oldStatusInfo = thing.getStatusInfo();
    // thing.setStatusInfo(thingStatusInfo);
    // try {
    // eventPublisher.post(ThingEventFactory.createStatusInfoEvent(thing.getUID(), thingStatusInfo));
    // if (!oldStatusInfo.equals(thingStatusInfo)) {
    // eventPublisher.post(
    // ThingEventFactory.createStatusInfoChangedEvent(thing.getUID(), thingStatusInfo, oldStatusInfo));
    // }
    // } catch (Exception ex) {
    // logger.error("Could not post 'ThingStatusInfoEvent' event: " + ex.getMessage(), ex);
    // }
    // }
}
