package house.intelli.core.rpc;

public class HttpRpcServerTransportProvider extends AbstractRpcServerTransportProvider {
	@Override
	protected RpcServerTransport _createRpcServerTransport() {
		return new HttpRpcServerTransport();
	}
}
