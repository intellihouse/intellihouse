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

		String path = serverUrl.getPath(); // never returns null according to javadoc
		while (path.endsWith("/"))
			path = path.substring(0, path.length() - 1);

		if (path.isEmpty())
			return appendEncodedPath(serverUrl, "intellihouse/RPC");
		else
			return serverUrl;
	}

	@Override
	protected RpcClientTransport _createRpcClientTransport() {
		URL serverUrl = assertNotNull(getActualServerUrl(), "serverUrl");
		HttpRpcClientTransport result = new HttpRpcClientTransport();
		result.setServerUrl(serverUrl);
		return result;
	}
}
