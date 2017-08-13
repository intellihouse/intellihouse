package house.intelli.pgp.rpc;

import static house.intelli.core.util.AssertUtil.*;

import house.intelli.core.rpc.HttpRpcServerTransportProvider;
import house.intelli.core.rpc.RpcServerTransport;

public class PgpHttpRpcServerTransportProvider extends HttpRpcServerTransportProvider {

	private boolean lateInitDone;

	@Override
	protected RpcServerTransport _createRpcServerTransport() {
		if (! lateInitDone) {
			PgpRequestService.setServerHostId(assertNotNull(getRpcContext().getLocalHostId(), "rpcContext.localHostId"));
			lateInitDone = true;
		}
		return new PgpHttpRpcServerTransport();
	}
}
