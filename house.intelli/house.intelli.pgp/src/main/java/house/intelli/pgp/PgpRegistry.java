package house.intelli.pgp;

import java.util.ServiceLoader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PgpRegistry {
	private static final Logger logger = LoggerFactory.getLogger(PgpRegistry.class);

	private Pgp pgp;
	private PgpAuthenticationCallback pgpAuthenticationCallback;

	protected PgpRegistry() { }

	private static final class Holder {
		public static final PgpRegistry instance = new PgpRegistry();
	}

	public static PgpRegistry getInstance() {
		return Holder.instance;
	}

	/**
	 * @deprecated Should normally only be used by tests.
	 */
	@Deprecated
	public synchronized void clearCache() {
		logger.info("clearCache: entered.");
		if (pgp instanceof AutoCloseable) { // AFAIK not used anymore.
			try {
				((AutoCloseable) pgp).close();
			} catch (Exception e) {
				logger.error("clearCache: " + e, e);
			}
		}
		pgp = null;
	}

	public synchronized Pgp getPgpOrFail() {
		Pgp pgp = this.pgp;
		if (pgp == null) {
			for (final Pgp p : ServiceLoader.load(Pgp.class)) {
				if (! p.isSupported())
					continue;

				if (pgp == null || pgp.getPriority() < p.getPriority())
					pgp = p;
			}

			if (pgp == null)
				throw new IllegalStateException("No supported Pgp implementation found!");

			this.pgp = pgp;
		}
		else
			logger.debug("getPgpOrFail: returning existing Pgp instance.");

		return pgp;
	}

	public PgpAuthenticationCallback getPgpAuthenticationCallback() {
		return pgpAuthenticationCallback;
	}

	public void setPgpAuthenticationCallback(final PgpAuthenticationCallback pgpAuthenticationCallback) {
		this.pgpAuthenticationCallback = pgpAuthenticationCallback;
	}
}
