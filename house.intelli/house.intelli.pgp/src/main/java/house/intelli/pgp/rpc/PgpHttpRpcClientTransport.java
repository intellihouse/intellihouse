package house.intelli.pgp.rpc;

import static house.intelli.core.util.AssertUtil.*;
import static house.intelli.core.util.Util.*;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import house.intelli.core.Uid;
import house.intelli.core.rpc.ErrorResponse;
import house.intelli.core.rpc.HostId;
import house.intelli.core.rpc.HttpRpcClientTransport;
import house.intelli.core.rpc.Request;
import house.intelli.core.rpc.Response;
import house.intelli.core.rpc.RpcMessage;

public class PgpHttpRpcClientTransport extends HttpRpcClientTransport {
	private static final Logger logger = LoggerFactory.getLogger(PgpHttpRpcClientTransport.class);

	private final PgpTransportSupport pgpTransportSupport = new PgpTransportSupport();

	public PgpHttpRpcClientTransport() {
	}

	public HostId getServerHostId() {
		return pgpTransportSupport.getServerHostId();
	}
	public void setServerHostId(HostId serverHostId) {
		pgpTransportSupport.setServerHostId(serverHostId);
	}

	@Override
	public void sendRequest(final Request<?> request) throws IOException {
		assertNotNull(request, "request");
		final PgpRequest pgpRequest;
		if (request instanceof PgpRequest) { // forward unchanged
			logger.debug("sendRequest: Relaying request: {}", request);
			pgpRequest = (PgpRequest) request;
		}
		else { // wrap inside a new PgpRequest
			logger.debug("sendRequest: Encrypting request: {}", request);
			pgpRequest = new PgpRequest();
			pgpRequest.copyRequestCoordinates(request);
			pgpRequest.setRequestId(new Uid());

			byte[] plainRequest = pgpTransportSupport.serializeRpcMessage(request);

//			HostId clientHostId = pgpTransportSupport.resolveRealServerHostId(request.getClientHostId());
			HostId serverHostId = pgpTransportSupport.resolveRealServerHostId(request.getServerHostId());

		// If it is encrypted + signed, it must always be signed by us -- even if we forward e.g. an error-message for someone else (which might be plain-text).
			HostId clientHostId = HostId.getLocalHostId();
			pgpRequest.setClientHostId(pgpTransportSupport.resolveAliasHostId(clientHostId));

			pgpRequest.setEncryptedRequest(
					pgpTransportSupport.encryptAndSign(plainRequest, clientHostId, serverHostId));

			logger.debug("sendRequest: Encrypted request: {}", pgpRequest);
		}
		super.sendRequest(pgpRequest);
	}

	@Override
	public Response receiveResponse() throws IOException {
		final Response res = super.receiveResponse();
		assertNotNull(res, "res");
		pgpTransportSupport.handleSessionNotFoundException(res);

		HostId serverHostId = pgpTransportSupport.resolveRealServerHostId(res.getServerHostId());
		HostId clientHostId = pgpTransportSupport.resolveRealServerHostId(res.getClientHostId());

		if (clientHostId.equals(getRpcContext().getLocalHostId())) {
			if (res instanceof ErrorResponse) {
				logger.debug("receiveResponse: Returning error-response directly: {}", res);
				return res;
			}

			logger.debug("receiveResponse: Decrypting response: {}", res);
			PgpResponse pgpResponse = (PgpResponse) res;

			byte[] plainResponse = pgpTransportSupport.decryptAndVerifySignature(pgpResponse.getEncryptedResponse(), serverHostId, clientHostId);

			RpcMessage rpcMessage = pgpTransportSupport.deserializeRpcMessage(plainResponse);
			Response response = (Response) rpcMessage;

			logger.debug("receiveResponse: Decrypted response: {}", response);
			pgpTransportSupport.handleSessionNotFoundException(response);

			// Only accept messages where the signed content of sender+recipient matches the outer envelope data!
			if (! equal(pgpResponse.getClientHostId(), response.getClientHostId()))
				throw new IOException(String.format("pgpResponse.clientHostId != response.clientHostId :: %s != %s",
						pgpResponse.getClientHostId(), response.getClientHostId()));

			if (! equal(pgpResponse.getServerHostId(), response.getServerHostId())) {
				// In case of an error, the server (= next node) might send + sign the pgpResponse
				// instead of the actual peer.
				if ((response instanceof ErrorResponse) && HostId.SERVER.equals(pgpResponse.getServerHostId()))
					return response;

				throw new IOException(String.format("pgpResponse.serverHostId != response.serverHostId :: %s != %s",
						pgpResponse.getServerHostId(), response.getServerHostId()));
			}

			return response;
		}
		else {
			logger.debug("receiveResponse: Relaying response: {}", res);
			return res;
		}
	}
}
