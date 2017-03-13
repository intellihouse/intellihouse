package house.intelli.core.rpc;

import static house.intelli.core.util.AssertUtil.*;

import java.net.URL;

public class HttpRpcClientTransportProvider extends AbstractRpcClientTransportProvider {
	private URL serverUrl;

	public URL getServerUrl() {
		return serverUrl;
	}
	public void setServerUrl(URL serverUrl) {
		this.serverUrl = serverUrl;
	}

	@Override
	protected RpcClientTransport _createRpcClientTransport() {
		URL serverUrl = assertNotNull(getServerUrl(), "serverUrl");
		HttpRpcClientTransport result = new HttpRpcClientTransport();
		result.setServerUrl(serverUrl);
		return result;
	}
}
