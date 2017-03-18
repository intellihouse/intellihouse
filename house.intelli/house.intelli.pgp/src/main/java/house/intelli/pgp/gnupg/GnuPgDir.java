package house.intelli.pgp.gnupg;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import house.intelli.core.util.IOUtil;

public class GnuPgDir {

	private static final Logger logger = LoggerFactory.getLogger(GnuPgDir.class);

	public static final String CONFIG_KEY_GNU_PG_DIR = "gnupg.dir";
	public static final String DEFAULT_GNU_PG_DIR = "${user.home}/.gnupg";

	private static final class Holder {
		public static final GnuPgDir instance = new GnuPgDir();
	}

	public static GnuPgDir getInstance() {
		return Holder.instance;
	}

	protected GnuPgDir() { }

	public File getFile() {
//		final String dirString = ConfigImpl.getInstance().getPropertyAsNonEmptyTrimmedString(CONFIG_KEY_GNU_PG_DIR, DEFAULT_GNU_PG_DIR);
		// TODO maybe implement a Config class later. right now, we don't need it.
		final String dirString = DEFAULT_GNU_PG_DIR;

		logger.debug("getFile: dirString={}", dirString);
		final String resolvedDir = IOUtil.replaceTemplateVariables(dirString, System.getProperties());
		final File result = new File(resolvedDir).getAbsoluteFile();
		logger.debug("getFile: result={}", result);
		return result;
	}
}
