package house.intelli.pvagg;

import static house.intelli.core.util.Util.*;
import static java.util.Objects.*;

import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;

import javax.jdo.PersistenceManagerFactory;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;
import house.intelli.core.config.ConfigDir;
import house.intelli.core.util.IOUtil;
import house.intelli.jdo.pmf.JdoPersistenceServiceCfgReader;

public class IntelliHousePvAggregator {

	public static final String PROPERTY_KEY_OPENHAB_BASE_DIR = "openhab.basedir";

	protected final File baseDir;

	protected final File confDir;

	protected final File servicesConfDir;

	protected final PersistenceManagerFactory pmf;

	public static void main(String[] args) throws Exception {
		initLogging();
		parseArgsIntoSystemProperties(args);
		IntelliHousePvAggregator intelliHousePvAggregator = new IntelliHousePvAggregator();
		intelliHousePvAggregator.run();
	}

	public IntelliHousePvAggregator() throws Exception {
		String baseDirString = System.getProperty(PROPERTY_KEY_OPENHAB_BASE_DIR);
		if (baseDirString == null || baseDirString.isEmpty())
			throw new IllegalStateException(String.format(
					"Property missing: %s", PROPERTY_KEY_OPENHAB_BASE_DIR));

		baseDir = new File(baseDirString);
		if (! baseDir.isDirectory())
			throw new IllegalStateException(String.format(
					"Property points to non-existent directory: %s = %s",
					PROPERTY_KEY_OPENHAB_BASE_DIR, baseDir.getAbsolutePath()));

		confDir = new File(baseDir, "conf");
		if (! confDir.isDirectory())
			throw new IllegalStateException(String.format(
					"Config-directory does not exist (property %s wrong?): %s",
					PROPERTY_KEY_OPENHAB_BASE_DIR, confDir.getAbsolutePath()));

		servicesConfDir = new File(confDir, "services");
		if (! servicesConfDir.isDirectory())
			throw new IllegalStateException(String.format(
					"Config-directory for services does not exist (property %s wrong?): %s",
					PROPERTY_KEY_OPENHAB_BASE_DIR, servicesConfDir.getAbsolutePath()));

		pmf = createPersistenceManagerFactory();
	}

	public void run() throws Exception {
		MainAggregator mainAggregator = new MainAggregator(pmf);
		mainAggregator.run();
	}

	protected PersistenceManagerFactory createPersistenceManagerFactory() throws IOException {
		File jdoPersistenceServiceCfgFile = new File(servicesConfDir, "JdoPersistenceService.cfg");
		if (! jdoPersistenceServiceCfgFile.isFile())
			throw new IllegalStateException(String.format(
					"File does not exist: %s", jdoPersistenceServiceCfgFile.getAbsolutePath()));

		JdoPersistenceServiceCfgReader pmfPropertiesReader = JdoPersistenceServiceCfgReader.fromJdoPersistenceServiceCfgFile(jdoPersistenceServiceCfgFile);
		return pmfPropertiesReader.getPersistenceManagerFactory();
	}

	private static void parseArgsIntoSystemProperties(String[] args) throws Exception {
		requireNonNull(args, "args");
		for (String arg : args) {
			int equalsCharIndex = arg.indexOf('=');
			if (equalsCharIndex < 0)
				throw new IllegalArgumentException("Program-argument is not a key-value-pair separated by an equals-character (=): " + arg);

			String key = arg.substring(0, equalsCharIndex);
			String value = arg.substring(equalsCharIndex + 1);

			key = URLDecoder.decode(key, "UTF-8");
			value = URLDecoder.decode(value, "UTF-8");
			System.setProperty(key, value);
		}
	}

	private static void initLogging() throws IOException, JoranException {
		ConfigDir.getInstance().getLogDir(); // creates the directory, if it does not exist yet. hence NECESSARY!
//		DerbyUtil.setLogFile(createFile(logDir, "derby.log")); // we don't use Derby (yet)

		final String logbackXmlName = "logback.xml";
		final File logbackXmlFile = new File(ConfigDir.getInstance().getFile(), logbackXmlName);
		if (! logbackXmlFile.exists()) {
			IOUtil.copyResource(
					IntelliHousePvAggregator.class, logbackXmlName, logbackXmlFile);
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
}
