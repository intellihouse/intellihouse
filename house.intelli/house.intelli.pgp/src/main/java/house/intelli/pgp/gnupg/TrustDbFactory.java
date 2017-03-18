package house.intelli.pgp.gnupg;

import static house.intelli.core.util.AssertUtil.*;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.IdentityHashMap;
import java.util.Timer;
import java.util.TimerTask;

import org.bouncycastle.openpgp.wot.IoFile;
import org.bouncycastle.openpgp.wot.TrustDb;
import org.bouncycastle.openpgp.wot.key.PgpKeyRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TrustDbFactory {
	private static final Logger logger = LoggerFactory.getLogger(TrustDbFactory.class);

	private final File trustDbFile;
	private final PgpKeyRegistry pgpKeyRegistry;

	private TrustDb trustDb;
	private IdentityHashMap<TrustDb, TrustDb> proxies = new IdentityHashMap<>();
	private final Timer deferredCloseTimer = new Timer("TrustDbFactory.deferredCloseTimer");
	private DeferredCloseTimerTask deferredCloseTimerTask;

	public TrustDbFactory(final File trustDbFile, final PgpKeyRegistry pgpKeyRegistry) {
		this.trustDbFile = assertNotNull(trustDbFile, "trustDbFile");
		this.pgpKeyRegistry = assertNotNull(pgpKeyRegistry, "pgpKeyRegistry");
	}

	public synchronized TrustDb createTrustDb() {
		if (trustDb == null) {
			logger.debug("createTrustDb: Creating *real* TrustDb instance.");
			try {
				trustDb = TrustDb.Helper.createInstance(
						new IoFile(trustDbFile), pgpKeyRegistry);
			} catch (IOException x) {
				throw new RuntimeException(x);
			}
		}
		else
			logger.trace("createTrustDb: Using existing *real* TrustDb instance.");

		final Object proxy = Proxy.newProxyInstance(this.getClass().getClassLoader(), new Class<?>[] { TrustDb.class }, new InvocationHandler() {
			@Override
			public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
				final TrustDb trustDbProxy = (TrustDb) proxy;

				synchronized (TrustDbFactory.this) {
					if ("close".equals(method.getName())) {
						_close(trustDbProxy);
						return null;
					}

					_assertIsOpen(trustDbProxy);
					final Object result = method.invoke(trustDb, args);
					return result;
				}
			}
		});

		final TrustDb trustDbProxy = (TrustDb) proxy;
		proxies.put(trustDbProxy, trustDbProxy);
		logger.trace("createTrustDb: Created and enlisted new proxy. openProxyCount={}", proxies.size());
		return trustDbProxy;
	}

	protected void _close(final TrustDb trustDbProxy) {
		assertNotNull(trustDbProxy, "trustDbProxy");
		if (_isOpen(trustDbProxy)) {
			proxies.remove(trustDbProxy);
			logger.trace("_close: Delisted proxy. openProxyCount={}", proxies.size());

			if (proxies.isEmpty()) {
				if (deferredCloseTimerTask != null)
					deferredCloseTimerTask.cancel();

				deferredCloseTimerTask = new DeferredCloseTimerTask();
				deferredCloseTimer.schedule(deferredCloseTimerTask, 10000); // defer closing by 10 seconds to avoid quick open-close-reopen-reclose-cycles
			}
		}
	}

	protected void _assertIsOpen(final TrustDb trustDbProxy) {
		assertNotNull(trustDbProxy, "trustDbProxy");
		if (! _isOpen(trustDbProxy))
			throw new IllegalStateException("trustDbProxy is already closed!");
	}

	protected boolean _isOpen(final TrustDb trustDbProxy) {
		assertNotNull(trustDbProxy, "trustDbProxy");
		return proxies.containsKey(trustDbProxy);
	}

	private class DeferredCloseTimerTask extends TimerTask {
		private final Logger logger = LoggerFactory.getLogger(TrustDbFactory.DeferredCloseTimerTask.class);

		@Override
		public void run() {
			synchronized (TrustDbFactory.this) {
				if (deferredCloseTimerTask != DeferredCloseTimerTask.this) {
					logger.debug("run: Aborting, because this is not the current instance, anymore.");
					return;
				}

				if (proxies.isEmpty() && trustDb != null) {
					logger.debug("run: Closing *real* TrustDb instance.");
					trustDb.close();
					trustDb = null;
				}

				deferredCloseTimerTask = null;
			}
		}
	}
}
