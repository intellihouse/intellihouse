package house.intelli.pgp.rpc;

import house.intelli.core.rpc.HttpRpcServerTransportProvider;
import house.intelli.core.rpc.RpcServerTransport;

public class PgpHttpRpcServerTransportProvider extends HttpRpcServerTransportProvider {

	@Override
	protected RpcServerTransport _createRpcServerTransport() {
		return new PgpHttpRpcServerTransport();
	}
}
