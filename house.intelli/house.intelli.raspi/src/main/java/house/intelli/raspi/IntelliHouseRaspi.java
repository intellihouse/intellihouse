package house.intelli.raspi;

import static house.intelli.core.util.Util.*;

import java.io.File;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;
import house.intelli.core.config.ConfigDir;
import house.intelli.core.event.EventQueue;
import house.intelli.core.jaxb.IntelliHouseJaxbContextProvider;
import house.intelli.core.rpc.RpcService;
import house.intelli.core.service.ServiceRegistry;
import house.intelli.core.util.IOUtil;
import house.intelli.pgp.Pgp;
import house.intelli.pgp.PgpKey;
import house.intelli.pgp.PgpOwnerTrust;
import house.intelli.pgp.PgpRegistry;
import house.intelli.raspi.service.SpringServiceRegistryDelegate;

public class IntelliHouseRaspi {

	public static void main(String[] args) throws Exception {
		initLogging();
		initUserHomeSpringContextFile();

		final Logger logger = LoggerFactory.getLogger(IntelliHouseRaspi.class);
		logger.info("Starting up...");

		EventQueue.invokeLater(new Runnable() {
			@Override
			public void run() {
				try {
					logger.info("Creating Spring ApplicationContext...");
					ApplicationContext applicationContext = new ClassPathXmlApplicationContext("META-INF/spring/spring-context.xml");

					ServiceRegistry.getInstance(RpcService.class).addDelegate(
							new SpringServiceRegistryDelegate<>(RpcService.class, applicationContext));

					ServiceRegistry.getInstance(IntelliHouseJaxbContextProvider.class).addDelegate(
							new SpringServiceRegistryDelegate<>(IntelliHouseJaxbContextProvider.class, applicationContext));

					setupPgp();

					logger.info("Created Spring ApplicationContext successfully.");
				} catch (Throwable x) {
					logger.error("Creating Spring ApplicationContext failed: " + x + ' ', x);
					System.exit(1);
				}
			}
		});

		while (! Thread.currentThread().isInterrupted()) {
			Thread.sleep(500L);
		}
	}

	private static void setupPgp() {
		try {
			Pgp pgp = PgpRegistry.getInstance().getPgpOrFail();

			// Automatically trust all our own keys ULTIMATELY (i.e. all those we have a secret key for).
			for (PgpKey pgpKey : pgp.getMasterKeysWithSecretKey())
				pgp.setOwnerTrust(pgpKey, PgpOwnerTrust.ULTIMATE);

			// And update the trust database to make sure the transitive trust is fine.
			pgp.updateTrustDb();
		} catch (Throwable x) {
			LoggerFactory.getLogger(IntelliHouseRaspi.class).warn("setupPgp: " + x + ' ', x);
		}
	}

	private static void initLogging() throws IOException, JoranException {
		ConfigDir.getInstance().getLogDir(); // creates the directory, if it does not exist yet. hence NECESSARY!
//		DerbyUtil.setLogFile(createFile(logDir, "derby.log")); // we don't use Derby (yet)

		final String logbackXmlName = "logback.xml";
		final File logbackXmlFile = new File(ConfigDir.getInstance().getFile(), logbackXmlName);
		if (! logbackXmlFile.exists()) {
			IOUtil.copyResource(
					IntelliHouseRaspi.class, logbackXmlName, logbackXmlFile);
		}

		final LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
		try {
			final JoranConfigurator configurator = new JoranConfigurator();
			configurator.setContext(context);
			// Call context.reset() to clear any previous configuration, e.g. default
			// configuration. For multi-step configuration, omit calling context.reset().
			context.reset();
			configurator.doConfigure(logbackXmlFile);
		} catch (final JoranException je) {
			// StatusPrinter will handle this
			doNothing();
		}
		StatusPrinter.printInCaseOfErrorsOrWarnings(context);
	}

	private static void initUserHomeSpringContextFile() throws IOException {
		final String springContextLocalName = "spring-context-local.xml";
		final File springContextLocalFile = new File(ConfigDir.getInstance().getFile(), springContextLocalName);
		if (! springContextLocalFile.exists()) {
			IOUtil.copyResource(
					IntelliHouseRaspi.class, springContextLocalName, springContextLocalFile);
		}
	}
}
