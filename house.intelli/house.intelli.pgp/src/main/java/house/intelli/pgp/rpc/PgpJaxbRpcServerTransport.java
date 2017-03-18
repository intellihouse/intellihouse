package house.intelli.pgp.rpc;

import static house.intelli.core.util.AssertUtil.*;
import static house.intelli.core.util.Util.*;

import java.io.IOException;

import house.intelli.core.rpc.HostId;
import house.intelli.core.rpc.JaxbRpcServerTransport;
import house.intelli.core.rpc.Request;
import house.intelli.core.rpc.Response;
import house.intelli.core.rpc.RpcMessage;

public abstract class PgpJaxbRpcServerTransport extends JaxbRpcServerTransport {

	private final PgpTransportSupport pgpTransportSupport = new PgpTransportSupport();

	@Override
	public Request<?> receiveRequest() throws IOException {
		Request<?> req = super.receiveRequest();

		HostId serverHostId = pgpTransportSupport.resolveRealServerHostId(req.getServerHostId());
		HostId clientHostId = pgpTransportSupport.resolveRealServerHostId(req.getClientHostId());

		if (serverHostId.equals(getRpcContext().getLocalHostId())) {
			PgpRequest pgpRequest = (PgpRequest) assertNotNull(req, "req");

			byte[] plainRequest = pgpTransportSupport.decryptAndVerifySignature(pgpRequest.getEncryptedRequest(), clientHostId);

			RpcMessage rpcMessage = pgpTransportSupport.deserializeRpcMessage(plainRequest);
			Request<?> request = (Request<?>) rpcMessage;

			// Only accept messages where the signed content of sender+recipient matches the outer envelope data!
			if (! equal(pgpRequest.getClientHostId(), request.getClientHostId()))
				throw new IOException(String.format("pgpRequest.clientHostId != request.clientHostId :: %s != %s",
						pgpRequest.getClientHostId(), request.getClientHostId()));

			if (! equal(pgpRequest.getServerHostId(), request.getServerHostId()))
				throw new IOException(String.format("pgpRequest.serverHostId != request.serverHostId :: %s != %s",
						pgpRequest.getServerHostId(), request.getServerHostId()));

			return request;
		}
		else
			return req;
	}

	@Override
	public void sendResponse(Response response) throws IOException {
		assertNotNull(response, "response");
		PgpResponse pgpResponse;
		if (response instanceof PgpResponse) // forward unchanged
			pgpResponse = (PgpResponse) response;
		else { // wrap inside a new PgpResponse
			pgpResponse = new PgpResponse();
			pgpResponse.copyRequestCoordinates(response);

			byte[] plainResponse = pgpTransportSupport.serializeRpcMessage(response);

			HostId clientHostId = pgpTransportSupport.resolveRealServerHostId(response.getClientHostId());
			HostId serverHostId = pgpTransportSupport.resolveRealServerHostId(response.getServerHostId());

			pgpResponse.setEncryptedResponse(
					pgpTransportSupport.encryptAndSign(plainResponse, serverHostId, clientHostId));
		}
		super.sendResponse(pgpResponse);
	}

}
