package house.intelli.core.rpc;

import static house.intelli.core.util.AssertUtil.*;
import static house.intelli.core.util.UrlUtil.*;

import java.net.URL;

public class HttpRpcClientTransportProvider extends AbstractRpcClientTransportProvider {
	private URL serverUrl;

	public URL getServerUrl() {
		return serverUrl;
	}
	public void setServerUrl(URL serverUrl) {
		this.serverUrl = serverUrl;
	}

	protected URL getActualServerUrl() {
		final URL serverUrl = getServerUrl();
		if (serverUrl == null)
			return null;

		return appendEncodedPath(serverUrl, "intellihouse/RPC");
	}

	@Override
	protected RpcClientTransport _createRpcClientTransport() {
		URL serverUrl = assertNotNull(getActualServerUrl(), "serverUrl");
		HttpRpcClientTransport result = new HttpRpcClientTransport();
		result.setServerUrl(serverUrl);
		return result;
	}
}
