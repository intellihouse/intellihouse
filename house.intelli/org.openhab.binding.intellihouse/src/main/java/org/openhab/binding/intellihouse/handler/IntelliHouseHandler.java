/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.intellihouse.handler;

import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import house.intelli.core.rpc.HostId;
import house.intelli.core.rpc.RpcClient;
import house.intelli.core.rpc.RpcContext;
import house.intelli.core.rpc.echo.EchoRequest;
import house.intelli.core.rpc.echo.EchoResponse;

/**
 * The {@link IntelliHouseHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Marco Nguitragool - Initial contribution
 */
public abstract class IntelliHouseHandler extends BaseThingHandler {

    private Logger logger = LoggerFactory.getLogger(IntelliHouseHandler.class);

    private HostId serverHostId;

    public IntelliHouseHandler(Thing thing) {
        super(thing);
    }

    // @Override
    // public void handleCommand(ChannelUID channelUID, Command command) {
    // logger.info("handleCommand: channelUID={}, command={}", channelUID, command);
    // final RpcContext rpcContext = getRpcContextOrFail();
    //// if (channelUID.getId().equals(CHANNEL_1)) {
    //// // TODO: handle command
    ////
    //// // Note: if communication with thing fails for some reason,
    //// // indicate that by setting the status with detail information
    //// // updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
    //// // "Could not control device at IP address x.x.x.x");
    ////
    //// }
    // }

    @Override
    public void initialize() {
        logger.info("initialize: thingUid={}", getThing().getUID());
        final RpcContext rpcContext = getRpcContextOrFail();
        // updateStatus(ThingStatus.UNKNOWN); // Status is "INITIALIZING" and we must not set it, now!
        new Thread("InitializeThread:" + getThing().getUID()) {
            @Override
            public void run() {
                try {
                    try {
                        serverHostId = new HostId((String) getThing().getConfiguration().get("hostId"));
                    } catch (Exception x) {
                        logger.warn(
                                "initialize.run: thingUid=" + getThing().getUID() + ": hostId missing/illegal: " + x,
                                x);
                        updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                                "hostId missing/illegal!");
                        return;
                    }

                    if (true) { // TODO later!
                        try (RpcClient rpcClient = rpcContext.createRpcClient()) {
                            EchoRequest echoRequest = new EchoRequest();
                            echoRequest.setServerHostId(serverHostId);
                            echoRequest.setPayload("initialize");
                            EchoResponse echoResponse = rpcClient.invoke(echoRequest);
                            logger.info("");
                        }
                    }
                    updateStatus(ThingStatus.ONLINE);
                } catch (Exception x) {
                    logger.error("initialize.run: " + x, x);
                    updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.HANDLER_INITIALIZING_ERROR,
                            "Devices offline: " + x);
                }
            }
        }.start();

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
