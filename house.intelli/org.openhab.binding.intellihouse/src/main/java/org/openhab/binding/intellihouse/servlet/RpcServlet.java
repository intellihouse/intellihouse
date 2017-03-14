/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.intellihouse.servlet;

import static house.intelli.core.util.AssertUtil.assertNotNull;

import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.smarthome.core.events.EventPublisher;
import org.openhab.binding.intellihouse.IntelliHouseActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.http.NamespaceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import house.intelli.core.rpc.JaxbRpcServerTransport;
import house.intelli.core.rpc.RpcContext;
import house.intelli.core.rpc.RpcContextMode;
import house.intelli.core.rpc.RpcServer;
import house.intelli.core.rpc.RpcService;
import house.intelli.core.service.AbstractServiceRegistryDelegate;
import house.intelli.core.service.ServiceRegistry;
import house.intelli.core.service.ServiceRegistryDelegate;

public class RpcServlet extends BaseServlet {
    private static final String METHOD_POST = "POST";

    private final Logger logger = LoggerFactory.getLogger(RpcServlet.class);

    public static final String SERVLET_NAME = "RPC";

    private EventPublisher eventPublisher;

    private RpcContext rpcContext;

    private BundleContext bundleContext;

    private ServiceRegistry<RpcService> rcpServiceServiceRegistry;
    private ServiceListener rpcServiceServiceListener;
    private ServiceRegistryDelegate<RpcService> rpcServiceServiceRegistryDelegate;
    private ServiceRegistration<RpcContext> rpcContextServiceRegistration;

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

            rcpServiceServiceRegistry = ServiceRegistry.getInstance(RpcService.class);
            rpcServiceServiceRegistryDelegate = new AbstractServiceRegistryDelegate<RpcService>() {
                @Override
                public List<RpcService> getServices() {
                    try {
                        List<RpcService> rpcServices = new ArrayList<>();
                        Collection<ServiceReference<RpcService>> serviceReferences = bundleContext
                                .getServiceReferences(RpcService.class, null);
                        for (ServiceReference<RpcService> serviceReference : serviceReferences) {
                            RpcService service = bundleContext.getService(serviceReference);
                            if (service != null) {
                                rpcServices.add(service);
                            }
                        }
                        return rpcServices;
                    } catch (InvalidSyntaxException e) {
                        throw new RuntimeException(e);
                    }
                }
            };
            rcpServiceServiceRegistry.addDelegate(rpcServiceServiceRegistryDelegate);
            rpcServiceServiceListener = event -> rcpServiceServiceRegistry.fireServiceRegistryChanged();
            bundleContext.addServiceListener(rpcServiceServiceListener);

            if (rpcContext != null) {
                rpcContext.close();
            }
            rpcContext = new RpcContext(RpcContextMode.SERVER);
            rpcContextServiceRegistration = bundleContext.registerService(RpcContext.class, rpcContext, null);

            Hashtable<String, String> props = new Hashtable<String, String>();
            httpService.registerServlet(WEBAPP_ALIAS + "/" + SERVLET_NAME, this, props, createHttpContext());

        } catch (NamespaceException e) {
            logger.error("Error during servlet startup", e);
        } catch (ServletException e) {
            logger.error("Error during servlet startup", e);
        }
    }

    protected void deactivate() {
        if (rpcContextServiceRegistration != null) {
            rpcContextServiceRegistration.unregister();
            rpcContextServiceRegistration = null;
        }
        if (rcpServiceServiceRegistry != null && rpcServiceServiceRegistryDelegate != null) {
            rcpServiceServiceRegistry.removeDelegate(rpcServiceServiceRegistryDelegate);
        }
        if (rpcServiceServiceListener != null) {
            bundleContext.removeServiceListener(rpcServiceServiceListener);
        }
        httpService.unregister(WEBAPP_ALIAS + "/" + SERVLET_NAME);
        if (rpcContext != null) {
            rpcContext.close();
            rpcContext = null;
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
    public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        if (METHOD_POST.equals(request.getMethod())) {
            try (RpcServer rpcServer = getRpcContextOrFail().createRpcServer()) {
                try (MyRpcServerTransport rpcServerTransport = new MyRpcServerTransport(rpcContext,
                        request.getInputStream(), res.getOutputStream())) {
                    rpcServer.receiveAndProcessRequest(rpcServerTransport);
                }
            }
        } else {
            for (Map.Entry<String, String[]> me : req.getParameterMap().entrySet()) {
                logger.info("{} = {}", me.getKey(), Arrays.toString(me.getValue()));
            }
            response.sendError(405, "Method not supported! Please use POST!");
            // ServletOutputStream out = res.getOutputStream();
            // OutputStreamWriter w = new OutputStreamWriter(out, StandardCharsets.UTF_8);
            // w.write("<html><head><title>Method not supported!</title></head><body><b>Method not supported! Please use
            // POST!</b></body></html>");
            // w.flush();
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
                    // *not* closing!
                }
            };
        }

        @Override
        protected OutputStream createResponseOutputStream() throws IOException {
            return new FilterOutputStream(out) {
                @Override
                public void close() throws IOException {
                    // *not* closing!
                }
            };
        }

    }

}
