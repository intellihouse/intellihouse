package house.intelli.core.rpc;

import static house.intelli.core.rpc.RpcConst.*;
import static house.intelli.core.util.AssertUtil.*;

import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class HttpRpcClientTransport extends JaxbRpcClientTransport {
	private URL serverUrl;

	private HttpURLConnection connection;

	private boolean requestSent;

	public URL getServerUrl() {
		return serverUrl;
	}
	public void setServerUrl(URL serverUrl) {
		this.serverUrl = serverUrl;
	}

	@Override
	protected OutputStream createRequestOutputStream() throws IOException {
		closeConnection();
		final OutputStream out = getConnection().getOutputStream();
		return new FilterOutputStream(out) {
			@Override
			public void close() throws IOException {
				try {
					super.close();
				} finally {
					requestSent = true;
				}
			}
		};
	}

	@Override
	protected InputStream createResponseInputStream() throws IOException {
		if (! requestSent)
			throw new IllegalStateException("request not yet sent!");

		final InputStream in = getConnection().getInputStream();
		return new FilterInputStream(in) {
			@Override
			public void close() throws IOException {
				try {
					super.close();
				} finally {
					closeConnection();
				}
			}
		};
	}

	protected HttpURLConnection getConnection() throws IOException {
		if (connection == null) {
			requestSent = false;
			connection = (HttpURLConnection) assertNotNull(getServerUrl(), "serverUrl").openConnection();
			connection.setConnectTimeout(TRANSPORT_CONNECT_TIMEOUT);
			connection.setReadTimeout(TRANSPORT_READ_TIMEOUT);
			connection.setRequestMethod("POST");
			connection.setRequestProperty("Content-Type", "application/octet-stream");
			connection.setRequestProperty("Connection", "keep-alive");
			connection.setUseCaches(false);
			connection.setDoOutput(true);
			connection.setAllowUserInteraction(false);
			connection.connect();
		}
		return connection;
	}

	protected void closeConnection() {
		if (connection != null) {
			connection.disconnect();
			connection = null;
		}
	}

	@Override
	public void close() {
		closeConnection();
		super.close();
	}
}
