package house.intelli.pgp.rpc;

import static house.intelli.core.util.AssertUtil.*;

import java.net.URL;

import house.intelli.core.rpc.HostId;
import house.intelli.core.rpc.HttpRpcClientTransportProvider;
import house.intelli.core.rpc.RpcClientTransport;

public class PgpHttpRpcClientTransportProvider extends HttpRpcClientTransportProvider {

	private HostId serverHostId;

	public HostId getServerHostId() {
		return serverHostId;
	}
	public void setServerHostId(HostId serverHostId) {
		this.serverHostId = serverHostId;
	}

	@Override
	protected RpcClientTransport _createRpcClientTransport() {
		URL serverUrl = assertNotNull(getActualServerUrl(), "serverUrl");
		PgpHttpRpcClientTransport result = new PgpHttpRpcClientTransport();
		result.setServerUrl(serverUrl);
		result.setServerHostId(assertNotNull(getServerHostId(), "serverHostId"));
		return result;
	}
}
