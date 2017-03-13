package house.intelli.core.rpc;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import house.intelli.core.Uid;

public class HttpRpcClientTransportTest {

	private RpcContext rpcContext;

	@Before
	public void before() throws Exception {
		rpcContext = new RpcContext(RpcContextMode.CLIENT, new HostId("dummy"));
	}

	@After
	public void after() throws Exception {
		if (rpcContext != null)
			rpcContext.close();
	}

	@Test
	public void posttestserver() throws Exception {
		try (HttpRpcClientTransport transport = new HttpRpcClientTransport()) {
			transport.setRpcContext(rpcContext);
			transport.setServerUrl(new URL("https://posttestserver.com/post.php?dir=intellihouse"));

			Uid uid1 = new Uid();
			System.out.println("uid1=" + uid1);

			try (OutputStream out = transport.createRequestOutputStream()) {
				Writer w = new OutputStreamWriter(out, StandardCharsets.UTF_8);
				w.write(uid1.toString());
				w.close();
			}

			try (InputStream in = transport.createResponseInputStream()) {
				BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
				String line;
				while (null != (line = r.readLine())) {
					System.out.println(line);
				}
			}
			System.out.println();

			Uid uid2 = new Uid();
			System.out.println("uid1=" + uid2);

			try (OutputStream out = transport.createRequestOutputStream()) {
				Writer w = new OutputStreamWriter(out, StandardCharsets.UTF_8);
				w.write(uid2.toString());
				w.close();
			}

			try (InputStream in = transport.createResponseInputStream()) {
				BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
				String line;
				while (null != (line = r.readLine())) {
					System.out.println(line);
				}
			}
			System.out.println();
		}
	}

}
