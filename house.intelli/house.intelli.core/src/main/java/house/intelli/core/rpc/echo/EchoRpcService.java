package house.intelli.core.rpc.echo;

import house.intelli.core.rpc.AbstractRpcService;

public class EchoRpcService extends AbstractRpcService<EchoRequest, EchoResponse> {

	@Override
	public EchoResponse process(EchoRequest request) throws Exception {
		EchoResponse response = new EchoResponse();
		response.setPayload(request.getPayload());
		return response;
	}
}
