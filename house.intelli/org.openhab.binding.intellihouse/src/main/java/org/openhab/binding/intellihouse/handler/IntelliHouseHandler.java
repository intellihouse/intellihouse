/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.intellihouse.handler;

import static house.intelli.core.util.AssertUtil.*;
import static org.openhab.binding.intellihouse.IntelliHouseBindingConstants.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.smarthome.core.common.registry.RegistryChangeListener;
import org.eclipse.smarthome.core.items.ItemRegistry;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.thing.link.ItemChannelLink;
import org.eclipse.smarthome.model.sitemap.SitemapProvider;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import house.intelli.core.rpc.HostId;
import house.intelli.core.rpc.RpcContext;

/**
 * The {@link IntelliHouseHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Marco Nguitragool - Initial contribution
 */
public abstract class IntelliHouseHandler extends BaseThingHandler {

    private Logger logger = LoggerFactory.getLogger(IntelliHouseHandler.class);

    private HostId serverHostId;
    private final Set<ChannelUID> initializedChannelUIDs = Collections.synchronizedSet(new HashSet<>());

    public IntelliHouseHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void initialize() {
        logger.info("initialize: thingUid={}: Beginning initialization.", getThing().getUID());
        try {
            serverHostId = new HostId((String) getThing().getConfiguration().get(THING_CONFIG_KEY_HOST_ID));
        } catch (Exception x) {
            logger.warn("initialize.run: thingUid=" + getThing().getUID() + ": hostId missing/illegal: " + x, x);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "hostId missing/illegal!");
            return;
        }
        getRpcContextOrFail(); // make sure it's available

        final ThingUID thingUID = getThing().getUID();

        // updateStatus(ThingStatus.UNKNOWN); // Status is "INITIALIZING" and we must *not* set it, now!!!

        // We do *not* attempt to talk with the device here. Whether the device is online or offline is
        // determined by the RpcServlet. Hence, we leave the current state "INITIALIZING" -- the RpcServlet
        // should update it soon to either ONLINE or OFFLINE.

        // Additionally, we try to query a status once the linkRegistry notifies us about a channel.
        linkRegistry.addRegistryChangeListener(new RegistryChangeListener<ItemChannelLink>() {
            @Override
            public void added(final ItemChannelLink link) {
                final ChannelUID channelUID = assertNotNull(link, "link").getUID();
                assertNotNull(channelUID, "link.uid");
                if (thingUID.equals(channelUID.getThingUID())) {
                    if (initializedChannelUIDs.add(channelUID)) {
                        startInitializeChannelThread(channelUID);
                    }
                }
            }

            @Override
            public void updated(ItemChannelLink oldLink, ItemChannelLink link) {
            }

            @Override
            public void removed(ItemChannelLink link) {
            }
        });

        // Maybe some channels are already registered (unlikely, but hey) => initilise them now
        for (ChannelUID channelUID : getChannelUIDs()) {
            if (initializedChannelUIDs.add(channelUID)) {
                startInitializeChannelThread(channelUID);
            }
        }

        // new Thread("InitializeThread[" + getThing().getUID() + ']') {
        // @Override
        // public void run() {
        // try {
        // Thread.sleep(20000L); // wait for the linkRegistry to be populated. unfortunately I have no idea how
        // // to do this in a better way, now :-(
        //
        // initializeAsync();
        // // try (RpcClient rpcClient = rpcContext.createRpcClient()) {
        // // EchoRequest echoRequest = new EchoRequest();
        // // echoRequest.setServerHostId(serverHostId);
        // // echoRequest.setPayload("initialize");
        // // EchoResponse echoResponse = rpcClient.invoke(echoRequest);
        // // assertNotNull(echoResponse, "echoResponse");
        // // logger.info("initialize: thingUid={}: Successfully initialized.", getThing().getUID());
        // // }
        // updateStatus(ThingStatus.ONLINE);
        // } catch (Exception x) {
        // logger.error("initialize.run: " + x, x);
        // updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.HANDLER_INITIALIZING_ERROR,
        // String.format("%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS %1$tZ: %2$s", new Date(), x));
        // }
        // }
        // }.start();

        // // TODO: Initialize the thing. If done set status to ONLINE to indicate proper working.
        // // Long running initialization should be done asynchronously in background.
        // updateStatus(ThingStatus.ONLINE);
        //
        // // Note: When initialization can NOT be done set the status with more details for further
        // // analysis. See also class ThingStatusDetail for all available status details.
        // // Add a description to give user information to understand why thing does not work
        // // as expected. E.g.
        // // updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
        // // "Can not access device as username and/or password are invalid");
    }

    protected void startInitializeChannelThread(final ChannelUID channelUID) {
        assertNotNull(channelUID, "channelUID");

        new Thread("InitializeChannelThread[" + channelUID + ']') {
            @Override
            public void run() {
                try {
                    initializeChannel(channelUID);

                    if (ThingStatus.INITIALIZING.equals(thing.getStatus())) {
                        updateStatus(ThingStatus.ONLINE);
                    }
                } catch (Exception x) {
                    logger.error("initialize.run: " + x, x);
                    updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                            String.format("%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS %1$tZ: %2$s", new Date(), x));
                }
            }
        }.start();
    }

    protected void initializeChannel(ChannelUID channelUID) throws Exception {

    }

    protected List<SitemapProvider> getSitemapProviders() {
        try {
            Collection<ServiceReference<SitemapProvider>> serviceReferences = bundleContext
                    .getServiceReferences(SitemapProvider.class, null);
            List<SitemapProvider> result = new ArrayList<>(serviceReferences.size());
            for (ServiceReference<SitemapProvider> serviceReference : serviceReferences) {
                SitemapProvider service = bundleContext.getService(serviceReference);
                if (service != null) {
                    result.add(service);
                }
            }
            return result;
        } catch (InvalidSyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    protected ItemRegistry getItemRegistryOrFail() {
        ServiceReference<ItemRegistry> serviceReference = bundleContext.getServiceReference(ItemRegistry.class);
        if (serviceReference == null) {
            throw new IllegalStateException("No ServiceReference found for: " + ItemRegistry.class.getName());
        }
        ItemRegistry itemRegistry = bundleContext.getService(serviceReference);
        if (itemRegistry == null) {
            throw new IllegalStateException("ServiceReference did not point to existing service: " + serviceReference);
        }
        return itemRegistry;
    }

    protected Collection<ChannelUID> getChannelUIDs() {
        logger.info("getChannelUids: thing.channels={}", thing.getChannels());
        final ThingUID thingUid = thing.getUID();
        Set<ChannelUID> channelUids = new LinkedHashSet<>();
        for (ItemChannelLink itemChannelLink : linkRegistry.getAll()) {
            ChannelUID channelUid = itemChannelLink.getUID();
            if (thingUid.equals(channelUid.getThingUID())) {
                channelUids.add(channelUid);
            }
        }
        logger.info("getChannelUids: channelUids={}", channelUids);
        return channelUids;
    }

    protected HostId getServerHostId() {
        return serverHostId;
    }

    protected RpcContext getRpcContextOrFail() {
        ServiceReference<RpcContext> serviceReference = bundleContext.getServiceReference(RpcContext.class);
        if (serviceReference == null) {
            throw new IllegalStateException("No ServiceReference found for: " + RpcContext.class.getName());
        }
        RpcContext rpcContext = bundleContext.getService(serviceReference);
        if (rpcContext == null) {
            throw new IllegalStateException("ServiceReference did not point to existing service: " + serviceReference);
        }
        return rpcContext;
    }
}
