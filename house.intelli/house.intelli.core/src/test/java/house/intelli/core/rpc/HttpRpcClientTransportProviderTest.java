package house.intelli.core.rpc;

import static org.assertj.core.api.Assertions.*;

import java.net.URL;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class HttpRpcClientTransportProviderTest {

	private RpcContext rpcContext;

	@Before
	public void before() throws Exception {
		rpcContext = new RpcContext(RpcContextMode.CLIENT);
	}

	@After
	public void after() throws Exception {
		if (rpcContext != null)
			rpcContext.close();
	}

	@Test
	public void fullServerUrl() throws Exception {
		HttpRpcClientTransportProvider rpcClientTransportProvider = new HttpRpcClientTransportProvider();
		rpcClientTransportProvider.setServerUrl(new URL("http://localhost:8080/intellihouse/RPC"));
		rpcContext.setRpcClientTransportProvider(rpcClientTransportProvider);
		assertThat(rpcClientTransportProvider.getActualServerUrl().toString()).isEqualTo("http://localhost:8080/intellihouse/RPC");
	}

	@Test
	public void baseServerUrlWithoutFinalSlash() throws Exception {
		HttpRpcClientTransportProvider rpcClientTransportProvider = new HttpRpcClientTransportProvider();
		rpcClientTransportProvider.setServerUrl(new URL("http://localhost:8080"));
		rpcContext.setRpcClientTransportProvider(rpcClientTransportProvider);
		assertThat(rpcClientTransportProvider.getActualServerUrl().toString()).isEqualTo("http://localhost:8080/intellihouse/RPC");
	}

	@Test
	public void baseServerUrlWithFinalSlash() throws Exception {
		HttpRpcClientTransportProvider rpcClientTransportProvider = new HttpRpcClientTransportProvider();
		rpcClientTransportProvider.setServerUrl(new URL("http://localhost:8080/"));
		rpcContext.setRpcClientTransportProvider(rpcClientTransportProvider);
		assertThat(rpcClientTransportProvider.getActualServerUrl().toString()).isEqualTo("http://localhost:8080/intellihouse/RPC");
	}

}
