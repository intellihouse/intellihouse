package house.intelli.pgp.rpc;

import static java.util.Objects.*;
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

	private Request<?> rawRequest;

	public PgpHttpRpcServerTransport() {
	}

	@Override
	public Request<?> receiveRequest() throws IOException {
		final HostId localHostId = getRpcContext().getLocalHostId();
		pgpTransportSupport.setServerHostId(localHostId);

		Request<?> req = super.receiveRequest();
		setRawRequest(requireNonNull(req, "req"));

		HostId serverHostId = pgpTransportSupport.resolveRealServerHostId(req.getServerHostId());
		HostId clientHostId = pgpTransportSupport.resolveRealServerHostId(req.getClientHostId());

		if (serverHostId.equals(localHostId)) {
			logger.debug("receiveRequest: Decrypting request: {}", req);

			if (! (req instanceof PgpRequest)) // We reject unencrypted communication!
				throw new IllegalStateException("Client sent plain-text request: " + req);

			PgpRequest pgpRequest = (PgpRequest) req;

			byte[] plainRequest = pgpTransportSupport.decryptAndVerifySignature(pgpRequest.getEncryptedRequest(), clientHostId, serverHostId);

			RpcMessage rpcMessage = pgpTransportSupport.deserializeRpcMessage(plainRequest);
			Request<?> request = (Request<?>) rpcMessage;

			logger.debug("receiveRequest: Decrypted request: {}", request);

			// Only accept messages where the signed content of sender+recipient matches the outer envelope data!
			if (! equal(pgpRequest.getClientHostId(), request.getClientHostId()))
				throw new IOException(String.format("pgpRequest.clientHostId != rawRequest.clientHostId :: %s != %s",
						pgpRequest.getClientHostId(), request.getClientHostId()));

			if (! equal(pgpRequest.getServerHostId(), request.getServerHostId()))
				throw new IOException(String.format("pgpRequest.serverHostId != rawRequest.serverHostId :: %s != %s",
						pgpRequest.getServerHostId(), request.getServerHostId()));

			return request;
		}
		else {
			logger.debug("receiveRequest: Relaying request: {}", req);
			return req;
		}
	}

	@Override
	public void sendResponse(Response response) throws IOException {
		requireNonNull(response, "response");

		final Request<?> rawRequest = getRawRequest();
		if (rawRequest != null)
			response.copyRequestCoordinates(rawRequest);

		if (! (rawRequest instanceof PgpRequest)) {
			// If the client sent a plain-text rawRequest (which we didn't process), we send the error-response in
			// plain-text, so that the client knows what he's doing wrong -- that doesn't expose any secret
			// information and is very helpful to the person trying to set up things.
			if (! (response instanceof ErrorResponse))
					throw new IllegalStateException("response should be an ErrorResponse when the client attempts to communicate unencrypted data!");

			super.sendResponse(response);
			return;
		}

		final HostId localHostId = getRpcContext().getLocalHostId();
		pgpTransportSupport.setServerHostId(localHostId);

		PgpResponse pgpResponse;
		if (response instanceof PgpResponse) { // forward unchanged
			logger.debug("sendResponse: Relaying response: {}", response);
			pgpResponse = (PgpResponse) response;
		}
		else { // wrap inside a new PgpResponse
			logger.debug("sendResponse: Encrypting response: {}", response);
			try {
				pgpResponse = new PgpResponse();
				pgpResponse.copyRequestCoordinates(response);

				byte[] plainResponse = pgpTransportSupport.serializeRpcMessage(response);

				HostId clientHostId = pgpTransportSupport.resolveRealServerHostId(response.getClientHostId());
//				HostId serverHostId = pgpTransportSupport.resolveRealServerHostId(response.getServerHostId());

				// If it is encrypted + signed, it must always be signed by us -- even if we forward e.g. an error-message for someone else (which might be plain-text).
				HostId serverHostId = localHostId;
				pgpResponse.setServerHostId(pgpTransportSupport.resolveAliasHostId(serverHostId));

				pgpResponse.setEncryptedResponse(
						pgpTransportSupport.encryptAndSign(plainResponse, serverHostId, clientHostId));

				logger.debug("sendResponse: Encrypted response: {}", pgpResponse);
			} catch (Exception x) {
				logger.error("sendResponse: " +x + ' ', x);

//				// If the client tries to communicate in plain-text with a Pgp-expecting server,
//				// this fails and we send the error in plain-text back.
//				if (response instanceof ErrorResponse) { // TODO reduce this as far as possible!
//					super.sendResponse(response);
//					return;
//				}
				throw x;
			}
		}
		super.sendResponse(pgpResponse);
	}

	protected void setRawRequest(Request<?> request) {
		if (this.rawRequest != null)
			throw new IllegalStateException("this.request already assigned!");

		this.rawRequest = requireNonNull(request, "rawRequest");
	}

	protected Request<?> getRawRequest() {
		return rawRequest;
	}

}
