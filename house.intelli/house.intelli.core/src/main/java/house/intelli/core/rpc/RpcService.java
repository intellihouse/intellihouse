package house.intelli.core.rpc;

/**
 * Service processing requests of a certain {@link #getRequestType() type}.
 * <p>
 * Implementations should sub-class AbstractRpc
 *
 * @author mn
 *
 * @param <REQ>
 * @param <RES>
 */
public interface RpcService<REQ extends Request, RES extends Response> {

	Class<REQ> getRequestType();

	Class<RES> getResponseType();

	int getPriority();

	RES process(REQ request) throws Exception;

	RpcContext getRpcContext();

	void setRpcContext(RpcContext rpcContext);

}
