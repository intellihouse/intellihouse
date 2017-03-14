package house.intelli.core.rpc;

import house.intelli.core.service.ServiceRegistry;

/**
 * Service processing requests of a certain {@link #getRequestType() type}.
 * <p>
 * Implementations should sub-class {@link AbstractRpcService}.
 * <p>
 * Implementations are <b>not thread-safe!</b> Therefore, the {@link ServiceRegistry}
 * {@linkplain #clone() clones} each service before it is used.
 *
 * @author mn
 *
 * @param <REQ> the type of the requests being processed by this service.
 * @param <RES> the type of the responses being sent back to the client. Use {@link VoidResponse}
 * to indicate a void-method-service (never returning a result).
 */
public interface RpcService<REQ extends Request, RES extends Response> {

	Class<REQ> getRequestType();

	Class<RES> getResponseType();

	/**
	 * Gets the priority of this service. The greater this number, the higher the priority.
	 * <p>
	 * For each incoming request, the service implementation with the highest priority is chosen.
	 * The implementation in {@link AbstractRpcService} returns 0 by default. Thus, in order to
	 * override a service, a value &gt;= 1 must be returned by the overriding service implementation.
	 * @return the priority of this service.
	 */
	int getPriority();

	/**
	 * Process the given {@code request}.
	 * @param request the request to be processed. Never <code>null</code>.
	 * @return the result of the operation. May be <code>null</code> for void-methods (in this case).
	 * @throws Exception
	 */
	RES process(REQ request) throws Exception;

	RpcContext getRpcContext();

	void setRpcContext(RpcContext rpcContext);

	/**
	 * Clone this {@code RpcService} instance.
	 * <p>
	 * Every invocation of a service is done with a separate instance. Therefore, the
	 * {@link ServiceRegistry} invokes {@code clone()} before returning an instance.
	 * @return a clone of this instance. Never <code>null</code>.
	 */
	RpcService<REQ, RES> clone();

}
