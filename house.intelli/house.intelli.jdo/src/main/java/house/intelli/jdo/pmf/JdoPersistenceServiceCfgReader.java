package house.intelli.jdo.pmf;

import static java.util.Objects.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import javax.jdo.JDOHelper;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;
import javax.jdo.Transaction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import house.intelli.jdo.model.PvStatusEntity;
import house.intelli.jdo.model.PvStatusHourEntity;
import house.intelli.jdo.model.PvStatusMinuteEntity;
import house.intelli.jdo.model.PvStatusQuarterHourEntity;

/**
 * Reader for an OpenHAB {@code JdoPersistenceService.cfg} file. This file is usually located in
 * {@code openhab/conf/services/}.
 * <p>
 * This file is usually read by OpenHAB. Thus, normally this reader class is not used -- JDO
 * runs inside OpenHAB. But since we also want to use it outside OpenHAB, we provide this reader.
 *
 * @author mn
 */
public class JdoPersistenceServiceCfgReader {

	private static final Logger logger = LoggerFactory.getLogger(JdoPersistenceServiceCfgReader.class);

	private static final String PREFIX = "org.openhab.binding.intellihouse.jdo.JdoPersistenceService:";

	private Map<String, String> jdoPersistenceServiceCfgProperties;
	private Properties pmfProperties;
	private PersistenceManagerFactory pmf;

	protected JdoPersistenceServiceCfgReader() {
	}

	public static JdoPersistenceServiceCfgReader fromJdoPersistenceServiceCfgFile(File jdoPersistenceServiceCfgFile) throws IOException {
		JdoPersistenceServiceCfgReader reader = new JdoPersistenceServiceCfgReader();
		reader.readJdoPersistenceServiceCfgFile(jdoPersistenceServiceCfgFile);
		return reader;
	}

	protected void readJdoPersistenceServiceCfgFile(File jdoPersistenceServiceCfgFile) throws IOException {
		Map<String, String> properties = new LinkedHashMap<>();
		try (InputStream in = new FileInputStream(jdoPersistenceServiceCfgFile)) {
			BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
			String line;
			while (null != (line = r.readLine())) {
				String trimmed = line.trim();
				if (trimmed.isEmpty() || trimmed.startsWith("#"))
					continue;

				int equalsCharIndex = trimmed.indexOf('=');
				if (equalsCharIndex < 0)
					continue;

				String key = trimmed.substring(0, equalsCharIndex).trim();
				String value = trimmed.substring(equalsCharIndex + 1).trim();
				properties.put(key, value);
			}
		}
		jdoPersistenceServiceCfgProperties = properties;
		pmfProperties = null;
	}

	public Properties getPmfProperties() {
		if (pmfProperties == null) {
			Map<String, String> srcProperties = requireNonNull(jdoPersistenceServiceCfgProperties, "jdoPersistenceServiceCfgProperties");
			Properties destProperties = new Properties();
			for (Map.Entry<String, String> me : srcProperties.entrySet()) {
				String key = me.getKey();
				String value = me.getValue();
				if (! key.startsWith(PREFIX)) {
					logger.warn("'JdoPersistenceService.cfg' contains key without / with wrong prefix! Skipping: {} = {}", key, value);
					continue;
				}
				String keyWithoutPrefix = key.substring(PREFIX.length());
				destProperties.put(keyWithoutPrefix, value);
			}
			destProperties.setProperty("javax.jdo.PersistenceManagerFactoryClass", org.datanucleus.api.jdo.JDOPersistenceManagerFactory.class.getName());
			pmfProperties = destProperties;
		}
		return pmfProperties;
	}

	public PersistenceManagerFactory getPersistenceManagerFactory() throws IOException {
		if (pmf == null) {
			Properties pmfProperties = getPmfProperties();

			pmf = JDOHelper.getPersistenceManagerFactory(pmfProperties);

			final PersistenceManager pm = pmf.getPersistenceManager();
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
		}
		return pmf;
	}

	protected void initEntityClasses(final PersistenceManager pm) {
    pm.getExtent(PvStatusEntity.class);
    pm.getExtent(PvStatusMinuteEntity.class);
    pm.getExtent(PvStatusQuarterHourEntity.class);
    pm.getExtent(PvStatusHourEntity.class);
	}

}
