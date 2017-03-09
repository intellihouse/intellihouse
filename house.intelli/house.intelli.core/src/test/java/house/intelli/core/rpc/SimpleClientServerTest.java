package house.intelli.core.rpc;

import org.junit.Before;
import org.junit.Test;

import house.intelli.core.Uid;

public class SimpleClientServerTest extends AbstractRpcTest {

	private HostId clientHostId;

	private HostId serverHostId;

	@Before
	public void before() throws Exception {
		clientHostId = new HostId("client-" + new Uid());
		serverHostId = new HostId("server-" + new Uid());
	}

	@Test
	public void clientInvokesServiceOnServer() throws Exception {
		RpcContext clientRpcContext = new RpcContext(clientHostId);
		RpcContext serverRpcContext = new RpcContext(serverHostId);

//		clientRpcContext.setRpcClientTransportProvider(rpcClientTransportProvider);

	}

}
