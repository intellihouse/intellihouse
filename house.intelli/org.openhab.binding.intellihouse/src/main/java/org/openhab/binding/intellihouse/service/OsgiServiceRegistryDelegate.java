package org.openhab.binding.intellihouse.service;

import static java.util.Objects.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import house.intelli.core.service.AbstractServiceRegistryDelegate;
import house.intelli.core.service.ServiceRegistry;

public class OsgiServiceRegistryDelegate<S> extends AbstractServiceRegistryDelegate<S> {
    private final Logger logger = LoggerFactory.getLogger(OsgiServiceRegistryDelegate.class);

    private final Class<S> serviceClass;
    private final BundleContext bundleContext;
    private ServiceListener serviceListener;

    public OsgiServiceRegistryDelegate(Class<S> serviceClass, BundleContext bundleContext) {
        this.serviceClass = requireNonNull(serviceClass, "serviceClass");
        this.bundleContext = requireNonNull(bundleContext, "bundleContext");
    }

    @Override
    public void setServiceRegistry(ServiceRegistry<S> serviceRegistry) {
        super.setServiceRegistry(serviceRegistry);
        hookListener();
    }

    @Override
    public List<S> getServices() {
        try {
            List<S> services = new ArrayList<>();
            Collection<ServiceReference<S>> serviceReferences = bundleContext.getServiceReferences(serviceClass, null);
            for (ServiceReference<S> serviceReference : serviceReferences) {
                S service = bundleContext.getService(serviceReference);
                if (service != null) {
                    services.add(service);
                }
            }
            logger.info("getServices: serviceClass='{}' services={}", serviceClass.getName(), services);
            return services;
        } catch (InvalidSyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    protected void serviceChanged(ServiceEvent event) {
        logger.info("serviceChanged: serviceClass='{}'", serviceClass.getName());
        // sub-classes may override to get notified about this.
        // but they MUST call the super-method!
        final ServiceRegistry<S> serviceRegistry = getServiceRegistry();
        if (serviceRegistry != null) {
            serviceRegistry.fireServiceRegistryChanged();
        }
    }

    protected void hookListener() {
        synchronized (this) {
            if (serviceListener != null) {
                return; // already hooked! hook only once!
            }
            serviceListener = event -> OsgiServiceRegistryDelegate.this.serviceChanged(event);
        }
        boolean unhook = true;
        try {
            final String filter = String.format("(objectclass=%s)", serviceClass.getName());
            bundleContext.addServiceListener(serviceListener, filter);
            unhook = false;
        } catch (RuntimeException x) {
            throw x;
        } catch (Exception x) {
            throw new RuntimeException(x);
        } finally {
            if (unhook) {
                unhookListener();
            }
        }
    }

    protected void unhookListener() {
        ServiceListener sl;
        synchronized (this) {
            sl = serviceListener;
            serviceListener = null;
        }
        if (sl != null) { // maybe not hooked! unhook only once.
            bundleContext.removeServiceListener(sl);
        }
    }

    @Override
    public void close() {
        unhookListener();
        super.close();
    }
}
