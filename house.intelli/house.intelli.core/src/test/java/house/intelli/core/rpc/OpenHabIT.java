package house.intelli.core.rpc;

import static org.assertj.core.api.Assertions.*;

import java.net.URL;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import house.intelli.core.rpc.echo.EchoRequest;
import house.intelli.core.rpc.echo.EchoResponse;

@Ignore // Currently only used in development, with running OpenHAB server in IDE.
public class OpenHabIT {

	private RpcContext rpcContext;

	@Before
	public void before() throws Exception {
		rpcContext = new RpcContext(RpcContextMode.CLIENT);

		HttpRpcClientTransportProvider rpcClientTransportProvider = new HttpRpcClientTransportProvider();
		rpcClientTransportProvider.setServerUrl(new URL("http://localhost:8080/intellihouse/RPC"));
		rpcContext.setRpcClientTransportProvider(rpcClientTransportProvider);
	}

	@After
	public void after() throws Exception {
		if (rpcContext != null)
			rpcContext.close();
	}

	@Test
	public void openHabEcho() throws Exception {
		EchoRequest echoRequest = new EchoRequest();
		echoRequest.setServerHostId(HostId.SERVER);
		echoRequest.setPayload("bla bla trallala");

		RpcClient rpcClient = rpcContext.createRpcClient();
		Response response = rpcClient.invoke(echoRequest);

		assertThat(response).isNotNull().isInstanceOf(EchoResponse.class);

		EchoResponse echoResponse = (EchoResponse) response;
		assertThat(echoResponse.getPayload()).isEqualTo("OSGi: bla bla trallala");
	}

}
