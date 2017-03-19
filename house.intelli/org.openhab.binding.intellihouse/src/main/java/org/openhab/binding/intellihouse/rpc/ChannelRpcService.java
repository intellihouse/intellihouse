package org.openhab.binding.intellihouse.rpc;

import static house.intelli.core.util.AssertUtil.assertNotNull;

import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.smarthome.core.items.Item;
import org.eclipse.smarthome.core.items.ItemUtil;
import org.eclipse.smarthome.core.items.events.ItemEventFactory;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.link.ItemChannelLinkRegistry;
import org.eclipse.smarthome.core.types.State;

import house.intelli.core.rpc.Response;
import house.intelli.core.rpc.channel.ChannelRequest;

/**
 * Abstract base-class making work with {@link Thing}s and {@link Channel}s easy. Implementors should usually
 * sub-class this, when they want to implement a service receiving notifications from their devices.
 * <p>
 *
 *
 * @author mn
 *
 * @param <REQ>
 * @param <RES>
 */
public abstract class ChannelRpcService<REQ extends ChannelRequest<RES>, RES extends Response>
        extends ThingRpcService<REQ, RES> {

    private ItemChannelLinkRegistry itemChannelLinkRegistry;

    protected ItemChannelLinkRegistry getItemChannelLinkRegistry() {
        if (itemChannelLinkRegistry == null) {
            itemChannelLinkRegistry = getServiceOrFail(ItemChannelLinkRegistry.class);
        }
        return itemChannelLinkRegistry;
    }

    protected Set<ChannelUID> getChannelUIDs(final ThingTypeUID thingTypeUID, final REQ request) {
        assertNotNull(thingTypeUID, "thingTypeUID");
        assertNotNull(request, "request");
        Set<ChannelUID> channelUids = new LinkedHashSet<>();
        Set<Thing> things = getThings(thingTypeUID, request);
        for (Thing thing : things) {
            ChannelUID channelUID = new ChannelUID(thing.getUID(), request.getChannelId());
            channelUids.add(channelUID);
        }
        return channelUids;
    }

    protected void stateUpdated(ChannelUID channelUID, State state) {
        assertNotNull(channelUID, "channelUID");
        assertNotNull(state, "state");
        Set<Item> items = getItemChannelLinkRegistry().getLinkedItems(channelUID);
        for (Item item : items) {
            State acceptedState = ItemUtil.convertToAcceptedState(state, item);
            getEventPublisher()
                    .post(ItemEventFactory.createStateEvent(item.getName(), acceptedState, channelUID.toString()));
        }
    }
}
