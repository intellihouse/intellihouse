package co.codewizards.raspi1.steca;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class StecaClient implements AutoCloseable {

	private static final Logger logger = LoggerFactory.getLogger(StecaClient.class);

	public abstract void open() throws IOException;

	@Override
	public abstract void close() throws IOException;

	public abstract boolean isOpen();

	protected abstract InputStream getInputStream();

	protected abstract OutputStream getOutputStream();

	public <R> R execute(final Request<R> request) throws IOException {
		if (! isOpen())
			open();

		request.setStecaClient(this);
		try {
			preExecute(request);
			try {
				final R result = request.execute();
				postExecute(request, null);
				return result;
			} catch (Exception x) {
				try {
					postExecute(request, x);
				} catch (Exception pex) {
					logger.error("postExecute failed: " + pex, pex);
				}
				throw x;
			}
		} finally {
			request.setStecaClient(null);
		}
	}

	protected void preExecute(Request<?> request) throws IOException {
	}

	protected void postExecute(Request<?> request, Throwable error) throws IOException {
	}
}
