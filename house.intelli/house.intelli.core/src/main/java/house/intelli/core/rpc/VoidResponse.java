package house.intelli.core.rpc;

/**
 * Response type indicating that a {@link RpcService} represents a void-method, i.e. never returns a value.
 * <p>
 * {@link VoidResponse} cannot be instantiated! {@link RpcService#process(Request)} should return <code>null</code>.
 * @author mn
 */
public final class VoidResponse extends Response {
	private VoidResponse() {
	}
}
