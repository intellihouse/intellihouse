package house.intelli.core.rpc;

import java.io.InputStream;
import java.io.OutputStream;

public interface RpcServerTransport extends AutoCloseable {

	InputStream createRequestInputStream();

	OutputStream createResponseOutputStream();
}
