package house.intelli.pgp.rpc;

import static java.util.Objects.*;

import java.net.URL;

import house.intelli.core.rpc.HostId;
import house.intelli.core.rpc.HttpRpcClientTransportProvider;
import house.intelli.core.rpc.RpcClientTransport;

public class PgpHttpRpcClientTransportProvider extends HttpRpcClientTransportProvider {

	private HostId serverHostId;

	public synchronized HostId getServerHostId() {
		return serverHostId;
	}
	public synchronized void setServerHostId(HostId serverHostId) {
		this.serverHostId = serverHostId;

		if (serverHostId != null)
			PgpRequestService.setServerHostId(serverHostId);
	}

	@Override
	protected RpcClientTransport _createRpcClientTransport() {
		resolveServerHostIdIfNeeded();
		URL serverUrl = requireNonNull(getActualServerUrl(), "serverUrl");
		PgpHttpRpcClientTransport result = new PgpHttpRpcClientTransport();
		result.setServerUrl(serverUrl);
		result.setServerHostId(getServerHostId());
		return result;
	}

	private void resolveServerHostIdIfNeeded() {
		if (getServerHostId() == null) {
			String host = requireNonNull(getServerUrl(), "serverUrl").getHost();
			setServerHostId(new HostId(host));
		}
	}
}
