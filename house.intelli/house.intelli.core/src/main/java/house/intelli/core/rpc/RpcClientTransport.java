package house.intelli.core.rpc;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface RpcClientTransport extends AutoCloseable {
//	static class Context {
//		private final long requestTimeout;
//
//		public Context(long requestTimeout) {
//			if (requestTimeout < 0)
//				throw new IllegalArgumentException("requestTimeout < 0");
//
//			this.requestTimeout = requestTimeout;
//		}
//
//		public long getRequestTimeout() {
//			return requestTimeout;
//		}
//	}

	RpcContext getRpcContext();
	void setRpcContext(RpcContext rpcContext);

//	long getRequestTimeout();
//	void setRequestTimeout(long timeout);

	OutputStream createRequestOutputStream() throws IOException;

	InputStream createResponseInputStream() throws IOException;
}
