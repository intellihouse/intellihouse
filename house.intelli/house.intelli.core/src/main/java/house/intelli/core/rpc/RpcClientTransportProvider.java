package house.intelli.core.rpc;

public interface RpcClientTransportProvider {

	RpcContext getRpcContext();
	void setRpcContext(RpcContext rpcContext);

	RpcClientTransport createRpcClientTransport();

}
