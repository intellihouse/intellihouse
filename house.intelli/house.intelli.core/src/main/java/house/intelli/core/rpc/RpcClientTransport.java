package house.intelli.core.rpc;

import java.io.IOException;

public interface RpcClientTransport extends AutoCloseable {
	RpcContext getRpcContext();
	void setRpcContext(RpcContext rpcContext);

	void sendRequest(Request request) throws IOException;

	Response receiveResponse() throws IOException;
}
