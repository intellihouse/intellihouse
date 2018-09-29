package house.intelli.raspi.pv.steca;

import static java.util.Objects.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract base class for REST requests.
 * <p>
 * Implementors are encouraged to sub-class {@code AbstractRequest} or {@link VoidRequest} instead of
 * directly implementing {@link Request}.
 *
 * @author Marco หงุ่ยตระกูล-Schulze - marco at codewizards dot co
 *
 * @param <R> the response type, i.e. the type of the object sent from the server back to the client.
 */
public abstract class AbstractRequest<R> implements Request<R> {
	private static final Logger logger = LoggerFactory.getLogger(AbstractRequest.class);

	private StecaClient stecaClient;

	@Override
	public StecaClient getStecaClient() {
		return stecaClient;
	}

	@Override
	public void setStecaClient(final StecaClient client) {
		this.stecaClient = client;
	}

	/**
	 * Gets the {@link StecaClient} or throws an exception, if it was not assigned.
	 * <p>
	 * Implementors of {@link Request}s are encouraged to use this method instead of {@link #getStecaClient()} in their
	 * {@link #execute()} method.
	 * @return the {@link StecaClient}. Never <code>null</code>.
	 */
	protected StecaClient getStecaClientOrFail() {
		final StecaClient client = getStecaClient();
		requireNonNull(client, "stecaClient");
		return client;
	}
}
