package house.intelli.core.rpc.inverse;

import static house.intelli.core.util.AssertUtil.*;

import java.util.List;

import house.intelli.core.rpc.AbstractRpcService;
import house.intelli.core.rpc.Request;
import house.intelli.core.rpc.RpcConst;
import house.intelli.core.rpc.RpcContext;

public class PollInverseRequestsRpcService extends AbstractRpcService<PollInverseRequestsRequest, PollInverseRequestsResponse> {

	@Override
	public PollInverseRequestsResponse process(PollInverseRequestsRequest request) throws Exception {
		assertNotNull(request, "request");
		final RpcContext rpcContext = assertNotNull(getRpcContext(), "rpcContext");

		final List<Request> requests = rpcContext.getInverseRequestRegistry().pollRequests(request.getClientHostId(),
				RpcConst.LOW_LEVEL_TIMEOUT * 90 / 100); // 90% of this timeout in order to avoid unnecessary deferring

		PollInverseRequestsResponse response = new PollInverseRequestsResponse();
		response.setRequests(requests);
		return response;
	}

}
