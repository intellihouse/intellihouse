package house.intelli.pgp.rpc;

import static house.intelli.core.util.AssertUtil.*;

import java.net.URL;

import house.intelli.core.rpc.HttpRpcClientTransportProvider;
import house.intelli.core.rpc.RpcClientTransport;

public class PgpHttpRpcClientTransportProvider extends HttpRpcClientTransportProvider {

	@Override
	protected RpcClientTransport _createRpcClientTransport() {
		URL serverUrl = assertNotNull(getActualServerUrl(), "serverUrl");
		PgpHttpRpcClientTransport result = new PgpHttpRpcClientTransport();
		result.setServerUrl(serverUrl);
		return result;
	}
}
