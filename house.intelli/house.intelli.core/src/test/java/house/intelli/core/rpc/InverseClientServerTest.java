package house.intelli.core.rpc;

import static org.assertj.core.api.Assertions.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import house.intelli.core.Uid;
import house.intelli.core.rpc.echo.EchoRequest;
import house.intelli.core.rpc.echo.EchoResponse;
import house.intelli.core.rpc.mocktransport.MockRpcClientTransportProvider;

public class InverseClientServerTest extends AbstractRpcTest {

	private HostId clientHostId;

	private HostId serverHostId;

	private RpcContext clientRpcContext;

	private RpcContext serverRpcContext;

	@Before
	public void before() throws Exception {
		clientHostId = new HostId("client-" + new Uid());
		serverHostId = new HostId("server-" + new Uid());

		clientRpcContext = new RpcContext(RpcContextMode.CLIENT, clientHostId);
		serverRpcContext = new RpcContext(RpcContextMode.SERVER, serverHostId);

		MockRpcClientTransportProvider rpcClientTransportProvider = new MockRpcClientTransportProvider();
		rpcClientTransportProvider.setServerRpcContext(serverRpcContext);
		clientRpcContext.setRpcClientTransportProvider(rpcClientTransportProvider);
	}

	@After
	public void after() throws Exception {
		if (clientRpcContext != null)
			clientRpcContext.close();

		if (serverRpcContext != null)
			serverRpcContext.close();
	}

	@Test
	public void serverInvokesNormalServiceOnClient() throws Exception {
		EchoRequest echoRequest = new EchoRequest();
		echoRequest.setServerHostId(clientHostId);
		echoRequest.setPayload("bla bla trallala");

		RpcClient rpcClient = serverRpcContext.createRpcClient();
		Response response = rpcClient.invoke(echoRequest);

		assertThat(response).isNotNull().isInstanceOf(EchoResponse.class);

		EchoResponse echoResponse = (EchoResponse) response;
		assertThat(echoResponse.getPayload()).isEqualTo("bla bla trallala");
	}

	@Test
	public void serverInvokesLongRunningServiceOnClient() throws Exception {
		long startTimestamp = System.currentTimeMillis();
		EchoRequest echoRequest = new EchoRequest();
		echoRequest.setServerHostId(clientHostId);
		Uid payloadUid = new Uid();
		echoRequest.setPayload(payloadUid.toString());

		long sleep = 5L * 60 * 1000; // 5 minutes
		echoRequest.setSleep(sleep);

		RpcClient rpcClient = serverRpcContext.createRpcClient();
		Response response = rpcClient.invoke(echoRequest);

		assertThat(response).isNotNull().isInstanceOf(EchoResponse.class);

		EchoResponse echoResponse = (EchoResponse) response;
		assertThat(echoResponse.getPayload()).isEqualTo(payloadUid.toString());

		assertThat(System.currentTimeMillis() - startTimestamp).isGreaterThanOrEqualTo(sleep);
	}

	@Test
	public void serverInvokesLongRunningServiceOnClientAndEncountersTimeout() throws Exception {
		long startTimestamp = System.currentTimeMillis();
		EchoRequest echoRequest = new EchoRequest();
		echoRequest.setServerHostId(clientHostId);
		Uid payloadUid = new Uid();
		echoRequest.setPayload(payloadUid.toString());

		long sleep = 5L * 60 * 1000; // 5 minutes
		echoRequest.setSleep(sleep);

		long timeout = 2L * 60 * 1000; // 2 minutes.
		timeout = timeout + random.nextInt(60 * 1000); // + a random time between 0 and 60 seconds
		echoRequest.setTimeout(timeout);

		RpcClient rpcClient = serverRpcContext.createRpcClient();

		try {
			Response response = rpcClient.invoke(echoRequest);
			fail("Received response instead of timeout: " + response);
		} catch (RpcTimeoutException x) {
			// fine!
		}

		long duration = System.currentTimeMillis() - startTimestamp;
		assertThat(duration).isGreaterThanOrEqualTo(timeout);
		assertThat(duration).isLessThanOrEqualTo(timeout + (5 * 1000L));
	}

	@Test
	public void serverInvokesServiceOnClientAndExpectsRemoteException() throws Exception {
		EchoRequest echoRequest = new EchoRequest();
		echoRequest.setServerHostId(clientHostId);
		Uid payloadUid = new Uid();
		echoRequest.setPayload(payloadUid.toString());
		echoRequest.setThrowExceptionClassName(NumberFormatException.class.getName());

		RpcClient rpcClient = serverRpcContext.createRpcClient();
		try {
			Response response = rpcClient.invoke(echoRequest);
			fail("Received response instead of exception: " + response);
		} catch (NumberFormatException x) {
			// fine!
			assertThat(x.getMessage()).isEqualTo(payloadUid.toString());
			assertThat(x.getCause()).isNotNull().isInstanceOf(RemoteException.class);
		}
	}
}
