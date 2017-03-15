/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.intellihouse.servlet;

import static house.intelli.core.util.AssertUtil.*;
import static org.openhab.binding.intellihouse.IntelliHouseBindingConstants.*;

import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.smarthome.core.events.EventPublisher;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingRegistry;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.ThingStatusInfo;
import org.eclipse.smarthome.core.thing.events.ThingEventFactory;
import org.openhab.binding.intellihouse.IntelliHouseActivator;
import org.openhab.binding.intellihouse.service.OsgiServiceRegistryDelegate;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import house.intelli.core.jaxb.IntelliHouseJaxbContextProvider;
import house.intelli.core.rpc.HostId;
import house.intelli.core.rpc.JaxbRpcServerTransport;
import house.intelli.core.rpc.Request;
import house.intelli.core.rpc.RpcContext;
import house.intelli.core.rpc.RpcContextMode;
import house.intelli.core.rpc.RpcServer;
import house.intelli.core.rpc.RpcService;
import house.intelli.core.service.ServiceRegistry;

public class RpcServlet extends BaseServlet {
    private static final String METHOD_POST = "POST";

    private final Logger logger = LoggerFactory.getLogger(RpcServlet.class);

    public static final String SERVLET_NAME = "RPC";

    private EventPublisher eventPublisher;
    private ThingRegistry thingRegistry;

    private RpcContext rpcContext;

    private BundleContext bundleContext;

    @SuppressWarnings("rawtypes")
    private OsgiServiceRegistryDelegate<RpcService> rpcServiceServiceRegistryDelegate;
    private OsgiServiceRegistryDelegate<IntelliHouseJaxbContextProvider> jaxbContextProviderServiceRegistryDelegate;
    private ServiceRegistration<RpcContext> rpcContextServiceRegistration;

    private Timer thingStatusOfflineTimer;
    private TimerTask thingStatusOfflineTimerTask;

    public void setEventPublisher(EventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    public void unsetEventPublisher(EventPublisher eventPublisher) {
        this.eventPublisher = null;
    }

    public BundleContext getBundleContext() {
        return bundleContext;
    }

    protected void activate() {
        try {
            logger.debug("Starting up RPC servlet at " + WEBAPP_ALIAS + "/" + SERVLET_NAME);

            bundleContext = IntelliHouseActivator.getInstance().getBundleContext();

            rpcServiceServiceRegistryDelegate = new OsgiServiceRegistryDelegate<>(RpcService.class, bundleContext);
            ServiceRegistry.getInstance(RpcService.class).addDelegate(rpcServiceServiceRegistryDelegate);

            jaxbContextProviderServiceRegistryDelegate = new OsgiServiceRegistryDelegate<>(
                    IntelliHouseJaxbContextProvider.class, bundleContext);
            ServiceRegistry.getInstance(IntelliHouseJaxbContextProvider.class)
                    .addDelegate(jaxbContextProviderServiceRegistryDelegate);

            rpcContext = new RpcContext(RpcContextMode.SERVER);
            rpcContextServiceRegistration = bundleContext.registerService(RpcContext.class, rpcContext, null);

            Hashtable<String, String> props = new Hashtable<String, String>();
            httpService.registerServlet(WEBAPP_ALIAS + "/" + SERVLET_NAME, this, props, createHttpContext());

            thingStatusOfflineTimer = new Timer("thingStatusOfflineTimer", true);
            thingStatusOfflineTimerTask = new TimerTask() {
                @Override
                public void run() {
                    try {
                        updateThingStatusOffline();
                    } catch (Throwable x) {
                        logger.error("thingStatusOfflineTimerTask.run: " + x, x);
                    }
                }
            };
            thingStatusOfflineTimer.schedule(thingStatusOfflineTimerTask, THING_OFFLINE_CHECK_PERIOD,
                    THING_OFFLINE_CHECK_PERIOD);
        } catch (Exception e) {
            logger.error("activate: " + e, e);
        }
    }

    protected void deactivate() {
        try {
            if (thingStatusOfflineTimer != null) {
                thingStatusOfflineTimer.cancel();
            }

            httpService.unregister(WEBAPP_ALIAS + "/" + SERVLET_NAME);

            if (rpcContextServiceRegistration != null) {
                rpcContextServiceRegistration.unregister();
            }
            if (rpcServiceServiceRegistryDelegate != null) {
                rpcServiceServiceRegistryDelegate.close();
            }
            if (jaxbContextProviderServiceRegistryDelegate != null) {
                jaxbContextProviderServiceRegistryDelegate.close();
            }
            if (rpcContext != null) {
                rpcContext.close();
            }
        } catch (Exception e) {
            logger.error("deactivate: " + e, e);
        }
    }

    protected RpcContext getRpcContextOrFail() {
        RpcContext result = rpcContext;
        if (result == null) {
            throw new IllegalStateException("rpcContext == null :: activate() not called?!");
        }
        return result;
    }

    @Override
    public void service(final ServletRequest _req, final ServletResponse _res) throws ServletException, IOException {
        final HttpServletRequest req = (HttpServletRequest) _req;
        final HttpServletResponse res = (HttpServletResponse) _res;

        if (METHOD_POST.equals(req.getMethod())) {
            try (RpcServer rpcServer = getRpcContextOrFail().createRpcServer()) {
                try (MyRpcServerTransport rst = new MyRpcServerTransport(rpcContext, req.getInputStream(),
                        res.getOutputStream())) {
                    rpcServer.receiveAndProcessRequest(rst);
                }
                Request<?> request = rpcServer.getRequest();
                assertNotNull(request, "rpcServer.request");
                Date now = new Date();
                ThingStatusInfo thingStatusInfo = new ThingStatusInfo(ThingStatus.ONLINE, ThingStatusDetail.NONE, null);
                for (Thing thing : getThings(request.getClientHostId())) {
                    thing.getConfiguration().put(THING_CONFIG_KEY_LAST_SEEN_DATE, now);
                    thing.getConfiguration().remove(THING_CONFIG_KEY_MAYBE_OFFLINE_SINCE_DATE);

                    // We prevent a configuration error to be overwritten by ONLINE, even if the outpost says properly
                    // "hello"!
                    if (!isThingStatusConfigurationError(thing)) {
                        setThingStatus(thing, thingStatusInfo);
                    }
                }
            }
        } else {
            res.sendError(405, String.format("Method '%s' not supported! Please use 'POST' instead!", req.getMethod()));
        }

        // for (Object key : req.getParameterMap().keySet()) {
        // String itemName = key.toString();
        //
        // if (!itemName.startsWith("__")) { // all additional webapp params start with "__" and should be ignored
        // String commandName = req.getParameter(itemName);
        // try {
        // Item item = itemRegistry.getItem(itemName);
        //
        // // we need a special treatment for the "TOGGLE" command of switches;
        // // this is no command officially supported and must be translated
        // // into real commands by the webapp.
        // if ((item instanceof SwitchItem || item instanceof GroupItem) && commandName.equals("TOGGLE")) {
        // commandName = OnOffType.ON.equals(item.getStateAs(OnOffType.class)) ? "OFF" : "ON";
        // }
        //
        // Command command = TypeParser.parseCommand(item.getAcceptedCommandTypes(), commandName);
        // if (command != null) {
        // eventPublisher.post(ItemEventFactory.createCommandEvent(itemName, command));
        // } else {
        // logger.warn("Received unknown command '{}' for item '{}'", commandName, itemName);
        // }
        // } catch (ItemNotFoundException e) {
        // logger.warn("Received command '{}' for item '{}', but the item does not exist in the registry",
        // commandName, itemName);
        // }
        // }
        // }
    }

    protected void updateThingStatusOffline() {
        final long now = System.currentTimeMillis();
        for (final Thing thing : thingRegistry.getAll()) {
            final String hostIdStr = (String) thing.getConfiguration().get(THING_CONFIG_KEY_HOST_ID);
            final Date lastSeenDate = (Date) thing.getConfiguration().get(THING_CONFIG_KEY_LAST_SEEN_DATE);

            if (lastSeenDate != null) {
                if (now - lastSeenDate.getTime() > THING_OFFLINE_TIMEOUT && !isThingStatusOffline(thing)) {
                    ThingStatusInfo thingStatusInfo = new ThingStatusInfo(ThingStatus.OFFLINE,
                            ThingStatusDetail.COMMUNICATION_ERROR,
                            String.format("Host '%s' was last seen %2$tY-%2$tm-%2$td %2$tH:%2$tM:%2$tS %2$tZ.",
                                    hostIdStr, lastSeenDate));
                    setThingStatus(thing, thingStatusInfo);
                }
                continue;
            }

            Date maybeOfflineSinceDate = (Date) thing.getConfiguration().get(THING_CONFIG_KEY_MAYBE_OFFLINE_SINCE_DATE);
            if (maybeOfflineSinceDate == null) {
                maybeOfflineSinceDate = new Date();
                thing.getConfiguration().put(THING_CONFIG_KEY_MAYBE_OFFLINE_SINCE_DATE, maybeOfflineSinceDate);
                continue;
            }
            if (now - maybeOfflineSinceDate.getTime() > THING_OFFLINE_TIMEOUT && !isThingStatusOffline(thing)) {
                ThingStatusInfo thingStatusInfo = new ThingStatusInfo(ThingStatus.OFFLINE,
                        ThingStatusDetail.COMMUNICATION_ERROR,
                        String.format(
                                "Host '%s' was never seen. It is offline at least since %2$tY-%2$tm-%2$td %2$tH:%2$tM:%2$tS %2$tZ.",
                                hostIdStr, maybeOfflineSinceDate));
                setThingStatus(thing, thingStatusInfo);
            }
        }
    }

    protected boolean isThingStatusOffline(final Thing thing) {
        assertNotNull(thing, "thing");
        ThingStatusInfo statusInfo = assertNotNull(thing.getStatusInfo(), "thing.statusInfo");
        return ThingStatus.OFFLINE.equals(statusInfo.getStatus());
    }

    protected boolean isThingStatusConfigurationError(final Thing thing) {
        assertNotNull(thing, "thing");
        ThingStatusInfo statusInfo = assertNotNull(thing.getStatusInfo(), "thing.statusInfo");
        return ThingStatusDetail.CONFIGURATION_ERROR.equals(statusInfo.getStatusDetail());
    }

    protected List<Thing> getThings(final HostId hostId) {
        assertNotNull(hostId, "hostId");
        List<Thing> result = new ArrayList<>();
        final String hostIdStr = hostId.toString();
        for (final Thing thing : thingRegistry.getAll()) {
            final String hid = (String) thing.getConfiguration().get(THING_CONFIG_KEY_HOST_ID);
            if (hostIdStr.equals(hid)) {
                result.add(thing);
            }
        }
        return result;
    }

    protected void setThingStatus(Thing thing, ThingStatusInfo thingStatusInfo) {
        assertNotNull(thing, "thing");
        assertNotNull(thingStatusInfo, "thingStatusInfo");
        // if (!isThingStatusWritable(thing)) {
        // logger.warn(
        // "setThingStatus: thingUid={}: NOT setting status, because isThingStatusWritable(...) returned false!",
        // thing.getUID());
        // return;
        // }
        ThingStatusInfo oldStatusInfo = thing.getStatusInfo();
        thing.setStatusInfo(thingStatusInfo);
        try {
            eventPublisher.post(ThingEventFactory.createStatusInfoEvent(thing.getUID(), thingStatusInfo));
            if (!oldStatusInfo.equals(thingStatusInfo)) {
                eventPublisher.post(
                        ThingEventFactory.createStatusInfoChangedEvent(thing.getUID(), thingStatusInfo, oldStatusInfo));
            }
        } catch (Exception ex) {
            logger.error("Could not post 'ThingStatusInfoEvent' event: " + ex.getMessage(), ex);
        }
    }

    public void setThingRegistry(ThingRegistry thingRegistry) {
        this.thingRegistry = thingRegistry;
    }

    public void unsetThingRegistry(ThingRegistry thingRegistry) {
        this.thingRegistry = null;
    }

    private static class MyRpcServerTransport extends JaxbRpcServerTransport {
        private final InputStream in;
        private final OutputStream out;

        public MyRpcServerTransport(RpcContext rpcContext, InputStream in, OutputStream out) {
            setRpcContext(assertNotNull(rpcContext, "rpcContext"));
            this.in = assertNotNull(in, "in");
            this.out = assertNotNull(out, "out");
        }

        @Override
        protected InputStream createRequestInputStream() throws IOException {
            return new FilterInputStream(in) {
                @Override
                public void close() throws IOException {
                    // *not* closing! It was *not* opened by us (but given to us from the outside).
                }
            };
        }

        @Override
        protected OutputStream createResponseOutputStream() throws IOException {
            return new FilterOutputStream(out) {
                @Override
                public void close() throws IOException {
                    // *not* closing! It was *not* opened by us (but given to us from the outside).
                }
            };
        }
    }
}
