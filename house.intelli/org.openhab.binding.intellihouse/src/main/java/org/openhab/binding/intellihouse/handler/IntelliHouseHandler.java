/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.intellihouse.handler;

import static java.util.Objects.*;
import static org.openhab.binding.intellihouse.IntelliHouseBindingConstants.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.common.registry.RegistryChangeListener;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.thing.binding.builder.ChannelBuilder;
import org.eclipse.smarthome.core.thing.binding.builder.ThingBuilder;
import org.eclipse.smarthome.core.thing.link.ItemChannelLink;
import org.eclipse.smarthome.core.thing.link.ItemChannelLinkRegistry;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
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

    protected BundleContext bundleContext;
    protected ItemChannelLinkRegistry linkRegistry;
    private HostId serverHostId;
    private final Set<ChannelUID> initializedChannelUIDs = Collections.synchronizedSet(new HashSet<ChannelUID>());

    @SuppressWarnings("rawtypes")
    private ServiceTracker linkRegistryServiceTracker;

    public IntelliHouseHandler(Thing thing) {
        super(thing);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
        super.setBundleContext(bundleContext);
        linkRegistryServiceTracker = new ServiceTracker(this.bundleContext, ItemChannelLinkRegistry.class.getName(),
                null) {
            @Override
            public Object addingService(final @Nullable ServiceReference reference) {
                linkRegistry = (ItemChannelLinkRegistry) bundleContext.getService(reference);
                return linkRegistry;
            }

            @Override
            public void removedService(final @Nullable ServiceReference reference, final @Nullable Object service) {
                synchronized (IntelliHouseHandler.this) {
                    linkRegistry = null;
                }
            }
        };
        linkRegistryServiceTracker.open();
    }

    @Override
    public void unsetBundleContext(@NonNull BundleContext bundleContext) {
        linkRegistryServiceTracker.close();
        super.unsetBundleContext(bundleContext);
        this.bundleContext = null;
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

        updateStatus(ThingStatus.UNKNOWN); // Status is "INITIALIZING" and since version 2.4 we *must* set it to UNKNOWN.

        // We do *not* attempt to talk with the device here. Whether the device is online or offline is
        // determined by the RpcServlet. Hence, we leave the current state "INITIALIZING" -- the RpcServlet
        // should update it soon to either ONLINE or OFFLINE.

        // Additionally, we try to query a status once the linkRegistry notifies us about a channel.
        linkRegistry.addRegistryChangeListener(new RegistryChangeListener<ItemChannelLink>() {
            @Override
            public void added(final ItemChannelLink link) {
                final ChannelUID channelUID = requireNonNull(link, "link").getLinkedUID();
                requireNonNull(channelUID, "link.uid");
                if (thingUID.equals(channelUID.getThingUID())) {
                    if (initializedChannelUIDs.add(channelUID)) {
                        registerChannelIfNeeded(channelUID);
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
                registerChannelIfNeeded(channelUID);
                startInitializeChannelThread(channelUID);
            }
        }
    }

    protected void startInitializeChannelThread(final ChannelUID channelUID) {
        requireNonNull(channelUID, "channelUID");

        new Thread("InitializeChannelThread[" + channelUID + ']') {
            @Override
            public void run() {
                try {
                    initializeChannel(channelUID);

                    if (ThingStatus.UNKNOWN.equals(thing.getStatus())) {
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

    protected void registerChannelIfNeeded(ChannelUID channelUID) {
        List<Channel> channels = new ArrayList<>(getThing().getChannels());
        for (Channel channel : channels) {
            if (channelUID.equals(channel.getUID())) {
                logger.info("registerChannelIfNeeded: thingUid={}: Channel already existing (skip): {}", getThing().getUID(), channelUID);
                return;
            }
        }
        logger.info("registerChannelIfNeeded: thingUid={}: Registering channel: {}", getThing().getUID(), channelUID);
        ThingBuilder thingBuilder = editThing();
        ChannelBuilder channelBuilder = ChannelBuilder.create(channelUID, getAcceptedItemType());
        channels.add(channelBuilder.build());
        thingBuilder.withChannels(channels);
        updateThing(thingBuilder.build());
        logger.info("registerChannelIfNeeded: thingUid={}: Channels: {}", getThing().getUID(), toChannelUidStringList(getThing().getChannels()));
    }

    protected String toChannelUidStringList(List<Channel> channels) {
        StringBuilder sb = new StringBuilder();
        for (Channel channel : channels) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(channel.getUID());
        }
        return sb.toString();
    }

    protected void initializeChannel(ChannelUID channelUID) throws Exception {
    }

    protected abstract String getAcceptedItemType();

//    protected List<SitemapProvider> getSitemapProviders() {
//        try {
//            Collection<ServiceReference<SitemapProvider>> serviceReferences = bundleContext
//                    .getServiceReferences(SitemapProvider.class, null);
//            List<SitemapProvider> result = new ArrayList<>(serviceReferences.size());
//            for (ServiceReference<SitemapProvider> serviceReference : serviceReferences) {
//                SitemapProvider service = bundleContext.getService(serviceReference);
//                if (service != null) {
//                    result.add(service);
//                }
//            }
//            return result;
//        } catch (InvalidSyntaxException e) {
//            throw new RuntimeException(e);
//        }
//    }

    protected <S> S getServiceOrFail(final Class<S> serviceClass) {
        requireNonNull(serviceClass, "serviceClass");
        ServiceReference<S> serviceReference = bundleContext.getServiceReference(serviceClass);
        if (serviceReference == null) {
            throw new IllegalStateException("No ServiceReference found for: " + serviceClass.getName());
        }
        S service = bundleContext.getService(serviceReference);
        if (service == null) {
            throw new IllegalStateException("ServiceReference did not point to existing service: " + serviceReference);
        }
        return service;
    }

    protected Collection<ChannelUID> getChannelUIDs() {
        logger.info("getChannelUids: thing.channels={}", thing.getChannels());
        final ThingUID thingUid = thing.getUID();
        Set<ChannelUID> channelUids = new LinkedHashSet<>();
        for (ItemChannelLink itemChannelLink : linkRegistry.getAll()) {
            ChannelUID channelUid = itemChannelLink.getLinkedUID();
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
