package house.intelli.core.rpc;

public interface RpcClientTransportProvider extends Cloneable {

	RpcContext getRpcContext();
	void setRpcContext(RpcContext rpcContext);

	RpcClientTransport createRpcClientTransport();

	RpcClientTransportProvider clone();

}
