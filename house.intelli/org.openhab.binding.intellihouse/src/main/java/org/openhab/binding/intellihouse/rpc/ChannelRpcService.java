package org.openhab.binding.intellihouse.rpc;

import static java.util.Objects.*;

import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingTypeUID;

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

    protected Set<ChannelUID> getChannelUIDs(final ThingTypeUID thingTypeUID, final REQ request) {
        requireNonNull(thingTypeUID, "thingTypeUID");
        requireNonNull(request, "request");
        Set<ChannelUID> channelUids = new LinkedHashSet<>();
        Set<Thing> things = getThings(thingTypeUID, request);
        for (Thing thing : things) {
            ChannelUID channelUID = new ChannelUID(thing.getUID(), request.getChannelId());
            channelUids.add(channelUID);
        }
        return channelUids;
    }
}
