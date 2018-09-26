package co.codewizards.raspi1.steca;

import java.io.IOException;

/**
 * Steca request sending data to / querying data from / invoking logic in the Steca inverter+charger.
 * <p>
 * Every request should be a separate class implementing this interface. It should be instantiated for
 * an individual invocation, parameterised (usually directly via the constructor) and passed to
 * {@link StecaClient#execute(Request)}.
 * <p>
 * Objects of this type are therefore short-lived: They normally are only used for one single invocation and
 * forgotten afterwards. In most cases, anonymous instances are directly passed to the
 * {@code StecaClient.execute(...)} method as shown in this example:
 * <p>
 * <pre>return getStecaClient().execute(new DoThisAndThatOnInverter(param1, param2));</pre>
 * <p>
 * Implementations of this interface are <i>not</i> thread-safe.
 * <p>
 * <b>Important:</b> Please do <i>not</i> directly implement this interface! It is recommended
 * to sub-class {@link AbstractRequest}.
 *
 * @author Marco หงุ่ยตระกูล-Schulze - marco at codewizards dot co
 *
 * @param <R> the response type, i.e. the type of the object sent from the server back to the client.
 */
public interface Request<R> {

	/**
	 * Gets the {@code StecaClient}.
	 * <p>
	 * {@link StecaClient#execute(Request)} assigns this property, before invoking
	 * {@link #execute()}. After the invocation, this property is cleared, again.
	 * @return the {@code StecaClient}. Never <code>null</code> during
	 * {@linkplain #execute() execution} (but otherwise it normally is <code>null</code>).
	 * @see #setStecaClient(StecaClient)
	 */
	StecaClient getStecaClient();

	/**
	 * Sets the {@code StecaClient}.
	 * @param client the {@code StecaClient}. May be <code>null</code>.
	 * @see #getStecaClient()
	 */
	void setStecaClient(StecaClient client);

	/**
	 * Execute the actual request.
	 * <p>
	 * <b>Important:</b> You should never invoke this method directly! Instead, pass the {@code Request} to
	 * {@link StecaClient#execute(Request)}.
	 * @return the response from the server. May be <code>null</code>. Depending on
	 * {@link #isResultNullable()} a <code>null</code> result is considered an error and causes an exception.
	 * @throws IOException in case, IO with the device failed.
	 */
	R execute() throws IOException;

}
