package org.openhab.binding.intellihouse.jdo;

import static java.util.Objects.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.jdo.JDOHelper;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;
import javax.jdo.Transaction;

import org.openhab.binding.intellihouse.IntelliHouseActivator;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.wiring.BundleWiring;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import house.intelli.core.util.ReflectionUtil;
import house.intelli.jdo.IntelliHouseTransaction;
import house.intelli.jdo.IntelliHouseTransactionImpl;
import house.intelli.jdo.model.PvStatusEntity;

public class JdoPersistenceServiceImpl implements JdoPersistenceService {

    private final Logger logger = LoggerFactory.getLogger(JdoPersistenceServiceImpl.class);

    private BundleContext bundleContext;

    private final AtomicInteger singleThreadAssertCounter = new AtomicInteger();

    private volatile PersistenceManagerFactory persistenceManagerFactory;

    private volatile Throwable startError;

    private volatile Map<String, Object> configProps = Collections.emptyMap();

    public static final String CONFIG_KEY_ENABLED = "enabled";

    private final List<Bundle> requiredBundles = new LinkedList<>();

    public JdoPersistenceServiceImpl() {
        logger.debug("<init>");
    }

    protected void activate(Map<String, Object> configProps) {
        int singleThreadAssertCounterValue = singleThreadAssertCounter.getAndIncrement();
        try {
            startError = null;
            logger.debug("activate: Starting JdoPersistenceService. configProps={}", configProps);
            if (singleThreadAssertCounterValue != 0) {
                throw new IllegalStateException("singleThreadAssertCounterValue != 0");
            }
            bundleContext = IntelliHouseActivator.getInstance().getBundleContext();
            this.configProps = Collections.unmodifiableMap(new HashMap<>(configProps));

            start();
        } catch (Throwable x) {
            startError = x;
            logger.error("activate: " + x, x);
        } finally {
            singleThreadAssertCounter.decrementAndGet();
        }
    }

    protected void modified(Map<String, Object> configProps) {
        int singleThreadAssertCounterValue = singleThreadAssertCounter.getAndIncrement();
        try {
            startError = null;
            logger.debug("modified: New configuration JdoPersistenceService. configProps={}", configProps);
            if (singleThreadAssertCounterValue != 0) {
                throw new IllegalStateException("singleThreadAssertCounterValue != 0");
            }
            stop();

            this.configProps = Collections.unmodifiableMap(new HashMap<>(configProps));

            start();
        } catch (Throwable x) {
            startError = x;
            logger.error("modified: " + x, x);
        } finally {
            singleThreadAssertCounter.decrementAndGet();
        }
    }

    protected void deactivate() {
        int singleThreadAssertCounterValue = singleThreadAssertCounter.getAndIncrement();
        try {
            startError = null;
            logger.debug("deactivate: Stopping JdoPersistenceService.");
            if (singleThreadAssertCounterValue != 0) {
                throw new IllegalStateException("singleThreadAssertCounterValue != 0");
            }

            stop();
        } catch (Exception e) {
            logger.error("deactivate: " + e, e);
        } finally {
            singleThreadAssertCounter.decrementAndGet();
        }
    }

    protected void start() throws Exception {
        if (! isEnabled()) {
            logger.warn("JdoPersistenceService is not enabled! To enable it, you must set 'org.openhab.binding.intellihouse.jdo.JdoPersistenceService:enabled=true' in the file 'JdoPersistenceService.cfg'!");
            return;
        }
        startRequiredBundlesIfNeeded();

        final ClassLoader cl = createCombiClassLoader();

        initJdbcDriver(cl);

        final Map<String, Object> persistenceProperties = new HashMap<>(configProps);
        persistenceProperties.put("javax.jdo.PersistenceManagerFactoryClass", "org.datanucleus.api.jdo.JDOPersistenceManagerFactory");
        persistenceProperties.put("datanucleus.plugin.pluginRegistryClassName", "org.datanucleus.plugin.OSGiPluginRegistry");
        persistenceManagerFactory = JDOHelper.getPersistenceManagerFactory(
                persistenceProperties, cl);

        final PersistenceManager pm = persistenceManagerFactory.getPersistenceManager();
        try {
            final Transaction tx = pm.currentTransaction();
            tx.begin();
            try {
                initEntityClasses(pm);
                tx.commit();
            } finally {
                if (tx.isActive()) {
                    tx.rollback();
                }
            }
        } finally {
            pm.close();
        }

        logger.info("start: JdoPersistenceService started successfully.");
    }

    protected void initEntityClasses(final PersistenceManager pm) {
        pm.getExtent(PvStatusEntity.class);
    }

    protected void initJdbcDriver(final ClassLoader cl) throws Exception {
        final Object jdbcDriverNameObj = configProps.get("javax.jdo.option.ConnectionDriverName");
        if (jdbcDriverNameObj == null) {
            logger.warn("initJdbcDriver: Property 'javax.jdo.option.ConnectionDriverName' is not set!");
        } else {
            final String jdbcDriverName = jdbcDriverNameObj.toString().trim();
            if (jdbcDriverName.isEmpty()) {
                logger.warn("initJdbcDriver: Property 'javax.jdo.option.ConnectionDriverName' is empty!");
            } else {
                logger.info("initJdbcDriver: Loading JDBC driver {}...", jdbcDriverName);
                Class<?> jdbcDriverClass;
                try {
                    jdbcDriverClass = cl.loadClass(jdbcDriverName);
                } catch (Exception x) {
                    jdbcDriverClass = null;
                    logger.error("start: Loading JDBC driver '" + jdbcDriverName + "' failed: " + x, x);
                }
                logger.info("initJdbcDriver: Loaded JDBC driver {} successfully.", jdbcDriverName);

                if (jdbcDriverClass != null) {
                    logger.info("initJdbcDriver: Trying to (re)register JDBC driver.");
                    try {
                        ReflectionUtil.invokeStatic(jdbcDriverClass, "deregister");
                    } catch (Throwable x) {
                        logger.debug("initJdbcDriver: deregister '" + jdbcDriverName + "' failed: " + x, x);
                    }
                    try {
                        ReflectionUtil.invokeStatic(jdbcDriverClass, "register");
                        logger.info("initJdbcDriver: registered {} successfully.", jdbcDriverName);
                    } catch (Throwable x) {
                        logger.warn("initJdbcDriver: register '" + jdbcDriverName + "' failed: " + x, x);
                    }
                }
            }
        }
    }

    protected void startRequiredBundlesIfNeeded() throws Exception {
//        logger.info(">>>>>>>>>>>>>>>>>>>>");
//        logger.info("Bundles:");
//        for (Bundle bundle : bundleContext.getBundles()) {
//            logger.info("  * {} ({})", bundle.getSymbolicName(), getBundleStateString(bundle.getState()));
//        }
//        logger.info("<<<<<<<<<<<<<<<<<<<<");

        requiredBundles.clear();
        requiredBundles.add(startBundleIfNeeded("org.postgresql.jdbc41"));
        requiredBundles.add(startBundleIfNeeded("javax.jdo"));
        requiredBundles.add(startBundleIfNeeded("org.datanucleus"));
        requiredBundles.add(startBundleIfNeeded("org.datanucleus.api.jdo"));
        requiredBundles.add(startBundleIfNeeded("org.datanucleus.store.rdbms"));
    }

    protected CombiClassLoader createCombiClassLoader() {
        final List<ClassLoader> bundleClassLoaders = new LinkedList<>();
        for (final Bundle bundle : requiredBundles) {
            final BundleWiring bundleWiring = bundle.adapt(BundleWiring.class);
            final ClassLoader bundleClassLoader = bundleWiring.getClassLoader();
            bundleClassLoaders.add(bundleClassLoader);
        }
        bundleClassLoaders.add(JdoPersistenceServiceImpl.class.getClassLoader());
        return new CombiClassLoader(bundleClassLoaders);
    }

    protected Bundle startBundleIfNeeded(final String symbolicName) throws Exception {
        requireNonNull(symbolicName, "symbolicName");
        final Bundle bundle = getBundleBySymbolicName(symbolicName);
        if (bundle == null) {
            throw new IllegalArgumentException("Bundle not found: " + symbolicName);
        }
        final int bundleState = bundle.getState();
        if (Bundle.ACTIVE == bundleState) {
            logger.info("Bundle '{}' is already active.", symbolicName);
            return bundle;
        }
        logger.info("Bundle '{}' is not active, but state='{}'. Starting it now...", symbolicName, getBundleStateString(bundleState));
        try {
            bundle.start();
        } catch (Exception e) {
            logger.info("Bundle '" + symbolicName + "' could not be started: " + e, e);
            throw e;
        }
        logger.info("Bundle '{}' was started. Current state='{}'.", symbolicName, getBundleStateString(bundle.getState()));
        return bundle;
    }

    protected Bundle getBundleBySymbolicName(final String symbolicName) {
        requireNonNull(symbolicName, "symbolicName");
        for (Bundle bundle : bundleContext.getBundles()) {
            if (symbolicName.equals(bundle.getSymbolicName())) {
                return bundle;
            }
        }
        return null;
    }

    protected String getBundleStateString(final int bundleState) {
        switch (bundleState) {
            case Bundle.ACTIVE:
                return "ACTIVE";
            case Bundle.INSTALLED:
                return "INSTALLED";
            case Bundle.RESOLVED:
                return "RESOLVED";
            case Bundle.STARTING:
                return "STARTING";
            case Bundle.STOPPING:
                return "STOPPING";
            case Bundle.UNINSTALLED:
                return "UNINSTALLED";
            default:
                return Integer.toString(bundleState);
        }
    }

    protected void stop() throws Exception {
        final PersistenceManagerFactory pmf = persistenceManagerFactory;
        if (pmf != null) {
            pmf.close();
        }
        persistenceManagerFactory = null;
    }

    @Override
    public boolean isEnabled() {
        final Object configValue = configProps.get(CONFIG_KEY_ENABLED);
        if (configValue == null) {
            logger.warn("Configuration does not contain key '{}'! JdoPersistenceService is disabled!", CONFIG_KEY_ENABLED);
            return false;
        }
        final String configStr = String.valueOf(configValue).trim();
        if (Boolean.TRUE.toString().equals(configStr)) {
            return true;
        }
        if (Boolean.FALSE.toString().equals(configStr)) {
            return false;
        }
        logger.warn("Configuration key '{}' is assigned illegal value '{}'! JdoPersistenceService is disabled!", CONFIG_KEY_ENABLED);
        return false;
    }

    @Override
    public PersistenceManagerFactory getPersistenceManagerFactory() {
        if (startError != null) {
            throw new IllegalStateException("Starting up PersistenceManagerFactory failed (before): " + startError);
        }
        if (bundleContext == null) {
            throw new IllegalStateException("activate was not yet called!");
        }
        final PersistenceManagerFactory pmf = persistenceManagerFactory;
        if (pmf == null) {
            if (isEnabled()) {
                int singleThreadAssertCounterValue = singleThreadAssertCounter.get();
                if (singleThreadAssertCounterValue != 0) {
                    throw new IllegalStateException("JdoPersistenceService is not active (currently starting or stopping)!");
                } else {
                    throw new IllegalStateException("JdoPersistenceService is not active (but why?!??)!");
                }
            } else {
                throw new IllegalStateException("JdoPersistenceService is not enabled! You must set 'org.openhab.binding.intellihouse.jdo.JdoPersistenceService:enabled=true' in the file 'JdoPersistenceService.cfg'!");
            }
        }
        return pmf;
    }

    @Override
    public IntelliHouseTransaction beginTransaction() {
        return new IntelliHouseTransactionImpl(getPersistenceManagerFactory());
    }
}
