package house.intelli.pgp.rpc;

import static house.intelli.core.util.Util.*;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import house.intelli.core.rpc.AbstractRpcService;
import house.intelli.core.rpc.DeferringResponse;
import house.intelli.core.rpc.HostId;
import house.intelli.core.rpc.Request;
import house.intelli.core.rpc.Response;
import house.intelli.core.rpc.RpcMessage;
import house.intelli.core.rpc.RpcServiceExecutor;

public class PgpRequestService extends AbstractRpcService<PgpRequest, PgpResponse> {
	private static final Logger logger = LoggerFactory.getLogger(PgpRequestService.class);

	// TODO this is ugly! Is there no better way?!
	private static HostId serverHostId;

	private final PgpTransportSupport pgpTransportSupport = new PgpTransportSupport();
	{
		pgpTransportSupport.setServerHostId(serverHostId);
	}

	public static HostId getServerHostId() {
		return serverHostId;
	}
	public static void setServerHostId(HostId serverHostId) {
		PgpRequestService.serverHostId = serverHostId;
	}

	@Override
	public PgpResponse process(PgpRequest req) throws Exception {
		final HostId localHostId = getRpcContext().getLocalHostId();
		final RpcServiceExecutor rpcServiceExecutor = getRpcContext().getRpcServiceExecutor();

		HostId serverHostId = pgpTransportSupport.resolveRealServerHostId(req.getServerHostId());
		HostId clientHostId = pgpTransportSupport.resolveRealServerHostId(req.getClientHostId());

		Response response;
		if (serverHostId.equals(localHostId)) {
			logger.debug("process: Decrypting request: {}", req);

			byte[] plainRequest = pgpTransportSupport.decryptAndVerifySignature(req.getEncryptedRequest(), clientHostId, serverHostId);

			RpcMessage rpcMessage = pgpTransportSupport.deserializeRpcMessage(plainRequest);
			Request<?> request = (Request<?>) rpcMessage;

			logger.debug("process: Decrypted request: {}", request);

			// Only accept messages where the signed content of sender+recipient matches the outer envelope data!
			if (! equal(req.getClientHostId(), request.getClientHostId()))
				throw new IOException(String.format("pgpRequest.clientHostId != rawRequest.clientHostId :: %s != %s",
						req.getClientHostId(), request.getClientHostId()));

			if (! equal(req.getServerHostId(), request.getServerHostId()))
				throw new IOException(String.format("pgpRequest.serverHostId != rawRequest.serverHostId :: %s != %s",
						req.getServerHostId(), request.getServerHostId()));

			rpcServiceExecutor.putRequest(request);
			response = rpcServiceExecutor.pollResponse(request.getRequestId(), request.getTimeout());
			if (response == null) {
				response = new DeferringResponse();
				response.copyRequestCoordinates(request); // warning! this might be a DeferredResponseRequest -- not the original request! but currently, this does not matter as the data copied is the same.
			}
		}
		else {
			logger.debug("process: Relaying request: {}", req);
			rpcServiceExecutor.putRequest(req);
			response = rpcServiceExecutor.pollResponse(req.getRequestId(), req.getTimeout());
			if (response == null) {
				response = new DeferringResponse();
				response.copyRequestCoordinates(req); // warning! this might be a DeferredResponseRequest -- not the original request! but currently, this does not matter as the data copied is the same.
			}
		}

		if (response instanceof PgpResponse)
			return (PgpResponse) response;

		logger.debug("process: Encrypting response: {}", response);
		try {
			PgpResponse pgpResponse = new PgpResponse();
			pgpResponse.copyRequestCoordinates(response);

			byte[] plainResponse = pgpTransportSupport.serializeRpcMessage(response);

			// If it is encrypted + signed, it must always be signed by us -- even if we forward e.g. an error-message for someone else (which might be plain-text).
			serverHostId = localHostId;
			pgpResponse.setServerHostId(pgpTransportSupport.resolveAliasHostId(serverHostId));

			pgpResponse.setEncryptedResponse(
					pgpTransportSupport.encryptAndSign(plainResponse, serverHostId, clientHostId));

			logger.debug("process: Encrypted response: {}", pgpResponse);
			return pgpResponse;
		} catch (Exception x) {
			logger.error("process: " +x + ' ', x);
			throw x;
		}
	}

}
