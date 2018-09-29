package house.intelli.pgp.rpc;

import static java.util.Objects.*;

import house.intelli.core.rpc.HttpRpcServerTransportProvider;
import house.intelli.core.rpc.RpcServerTransport;

public class PgpHttpRpcServerTransportProvider extends HttpRpcServerTransportProvider {

	private boolean lateInitDone;

	@Override
	protected RpcServerTransport _createRpcServerTransport() {
		if (! lateInitDone) {
			PgpRequestService.setServerHostId(requireNonNull(getRpcContext().getLocalHostId(), "rpcContext.localHostId"));
			lateInitDone = true;
		}
		return new PgpHttpRpcServerTransport();
	}
}
