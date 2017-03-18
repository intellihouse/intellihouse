package house.intelli.pgp.rpc;

import static house.intelli.core.util.AssertUtil.*;
import static house.intelli.core.util.Util.*;

import java.io.IOException;

import house.intelli.core.Uid;
import house.intelli.core.rpc.HostId;
import house.intelli.core.rpc.HttpRpcClientTransport;
import house.intelli.core.rpc.Request;
import house.intelli.core.rpc.Response;
import house.intelli.core.rpc.RpcMessage;

public class PgpHttpRpcClientTransport extends HttpRpcClientTransport {

	private final PgpTransportSupport pgpTransportSupport = new PgpTransportSupport();

	public PgpHttpRpcClientTransport() {
	}

	public HostId getServerHostId() {
		return pgpTransportSupport.getServerHostId();
	}
	public void setServerHostId(HostId serverHostId) {
		pgpTransportSupport.setServerHostId(serverHostId);
		resolveServerHostIdIfNeeded(); // is called after this.setServerUrl(...), hence we can resolve.
	}

	@Override
	public void sendRequest(final Request<?> request) throws IOException {
		assertNotNull(request, "request");
		final PgpRequest pgpRequest;
		if (request instanceof PgpRequest) // forward unchanged
			pgpRequest = (PgpRequest) request;
		else { // wrap inside a new PgpRequest
			pgpRequest = new PgpRequest();
			pgpRequest.copyRequestCoordinates(request);
			pgpRequest.setRequestId(new Uid());

			byte[] plainRequest = pgpTransportSupport.serializeRpcMessage(request);

			HostId clientHostId = pgpTransportSupport.resolveRealServerHostId(request.getClientHostId());
			HostId serverHostId = pgpTransportSupport.resolveRealServerHostId(request.getServerHostId());

			pgpRequest.setEncryptedRequest(
					pgpTransportSupport.encryptAndSign(plainRequest, clientHostId, serverHostId));
		}
		super.sendRequest(pgpRequest);
	}

	@Override
	public Response receiveResponse() throws IOException {
		final Response res = super.receiveResponse();

		HostId serverHostId = pgpTransportSupport.resolveRealServerHostId(res.getServerHostId());
		HostId clientHostId = pgpTransportSupport.resolveRealServerHostId(res.getClientHostId());

		if (clientHostId.equals(getRpcContext().getLocalHostId())) {
			PgpResponse pgpResponse = (PgpResponse) assertNotNull(res, "res");

			byte[] plainResponse = pgpTransportSupport.decryptAndVerifySignature(pgpResponse.getEncryptedResponse(), serverHostId);

			RpcMessage rpcMessage = pgpTransportSupport.deserializeRpcMessage(plainResponse);
			Response response = (Response) rpcMessage;

			// Only accept messages where the signed content of sender+recipient matches the outer envelope data!
			if (! equal(pgpResponse.getClientHostId(), response.getClientHostId()))
				throw new IOException(String.format("pgpResponse.clientHostId != response.clientHostId :: %s != %s",
						pgpResponse.getClientHostId(), response.getClientHostId()));

			if (! equal(pgpResponse.getServerHostId(), response.getServerHostId()))
				throw new IOException(String.format("pgpResponse.serverHostId != response.serverHostId :: %s != %s",
						pgpResponse.getServerHostId(), response.getServerHostId()));

			return response;
		}
		else
			return res;
	}

	private void resolveServerHostIdIfNeeded() {
		if (getServerHostId() == null) {
			String host = assertNotNull(getServerUrl(), "serverUrl").getHost();
			setServerHostId(new HostId(host));
		}
	}

}
