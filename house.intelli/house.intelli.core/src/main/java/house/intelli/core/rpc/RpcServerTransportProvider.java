package house.intelli.core.rpc;

public interface RpcServerTransportProvider extends Cloneable {

	RpcContext getRpcContext();
	void setRpcContext(RpcContext rpcContext);

	RpcServerTransport createRpcServerTransport();

	RpcServerTransportProvider clone();
}
