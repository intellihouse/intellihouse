package org.openhab.binding.intellihouse;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class IntelliHouseActivator implements BundleActivator {

    private BundleContext bundleContext;

    private static IntelliHouseActivator instance;

    public static IntelliHouseActivator getInstance() {
        return instance;
    }

    public BundleContext getBundleContext() {
        return bundleContext;
    }

    @Override
    public void start(BundleContext context) throws Exception {
        this.bundleContext = context;
        instance = this;
    }

    @Override
    public void stop(BundleContext context) throws Exception {
    }

}
