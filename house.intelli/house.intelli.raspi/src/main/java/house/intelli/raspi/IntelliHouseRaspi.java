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
import house.intelli.core.util.IOUtil;

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
					logger.info("Created Spring ApplicationContext successfully.");
				} catch (Throwable x) {
					logger.error("Creating Spring ApplicationContext failed: " + x, x);
					System.exit(1);
				}
			}
		});

		while (! Thread.currentThread().isInterrupted()) {
			Thread.sleep(500L);
		}
	}

	private static void initLogging() throws IOException, JoranException {
		final File logDir = ConfigDir.getInstance().getLogDir();
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
