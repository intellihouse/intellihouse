package house.intelli.pgp.rpc;

import static house.intelli.core.util.AssertUtil.*;
import static house.intelli.core.util.Util.*;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import house.intelli.core.rpc.ErrorResponse;
import house.intelli.core.rpc.HostId;
import house.intelli.core.rpc.HttpRpcServerTransport;
import house.intelli.core.rpc.Request;
import house.intelli.core.rpc.Response;
import house.intelli.core.rpc.RpcMessage;

public class PgpHttpRpcServerTransport extends HttpRpcServerTransport {

	private static final Logger logger = LoggerFactory.getLogger(PgpHttpRpcServerTransport.class);

	private final PgpTransportSupport pgpTransportSupport = new PgpTransportSupport();

	public PgpHttpRpcServerTransport() {
	}

	@Override
	public Request<?> receiveRequest() throws IOException {
		final HostId localHostId = getRpcContext().getLocalHostId();
		pgpTransportSupport.setServerHostId(localHostId);

		Request<?> req = super.receiveRequest();

		HostId serverHostId = pgpTransportSupport.resolveRealServerHostId(req.getServerHostId());
		HostId clientHostId = pgpTransportSupport.resolveRealServerHostId(req.getClientHostId());

		if (serverHostId.equals(localHostId)) {
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
		final HostId localHostId = getRpcContext().getLocalHostId();
		pgpTransportSupport.setServerHostId(localHostId);

		PgpResponse pgpResponse;
		if (response instanceof PgpResponse) // forward unchanged
			pgpResponse = (PgpResponse) response;
		else { // wrap inside a new PgpResponse
			try {
				pgpResponse = new PgpResponse();
				pgpResponse.copyRequestCoordinates(response);

				byte[] plainResponse = pgpTransportSupport.serializeRpcMessage(response);

				HostId clientHostId = pgpTransportSupport.resolveRealServerHostId(response.getClientHostId());
				HostId serverHostId = pgpTransportSupport.resolveRealServerHostId(response.getServerHostId());

				pgpResponse.setEncryptedResponse(
						pgpTransportSupport.encryptAndSign(plainResponse, serverHostId, clientHostId));
			} catch (Exception x) {
				logger.error("sendResponse: " +x + ' ', x);

				// If the client tries to communicate in plain-text with a Pgp-expecting server,
				// this fails and we send the error in plain-text back.
				if (response instanceof ErrorResponse) {
					super.sendResponse(response);
					return;
				}
				throw x;
			}
		}
		super.sendResponse(pgpResponse);
	}

}
