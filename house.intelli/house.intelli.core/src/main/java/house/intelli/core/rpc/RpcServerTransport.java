package house.intelli.core.rpc;

import java.io.IOException;

public interface RpcServerTransport extends AutoCloseable {

	RpcContext getRpcContext();
	void setRpcContext(RpcContext rpcContext);

	Request receiveRequest() throws IOException;

	void sendResponse(Response response) throws IOException;
}
