package house.intelli.core.rpc;

import static house.intelli.core.rpc.RpcConst.*;
import static house.intelli.core.util.AssertUtil.*;

import java.io.InputStream;
import java.io.OutputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import house.intelli.core.Uid;
import house.intelli.core.jaxb.IntelliHouseJaxbContext;

public class RpcServer {

	private final RpcContext rpcContext;

	protected RpcServer(final RpcContext rpcContext) {
		this.rpcContext = assertNotNull(rpcContext, "rpcContext");
	}

	public void receiveAndProcessRequest(final RpcServerTransport rpcServerTransport) throws RpcException {
		assertNotNull(rpcServerTransport, "rpcServerTransport");
		if (rpcServerTransport.getRpcContext() != this.rpcContext)
			throw new IllegalArgumentException("rpcServerTransport.rpcContext != this.rpcContext");

		try {
			JAXBContext jaxbContext = IntelliHouseJaxbContext.getJaxbContext();
			Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

			Request request = null;
			Response response = null;
			try {
				Object unmarshalled;
				try (InputStream inputStream = rpcServerTransport.createRequestInputStream()) {
					unmarshalled = unmarshaller.unmarshal(inputStream);
				}

				if (unmarshalled instanceof Request) {
					request = (Request) unmarshalled;
					response = process(request);
				}
				else
					throw new RpcException("Client sent an instance of ");

			} catch (Exception x) {
				Error error = RemoteExceptionUtil.createError(x);
				response = new ErrorResponse(error);

				if (request != null)
					response.copyRequestCoordinates(request);
			}
			assertNotNull(response, "response");

			Marshaller marshaller = jaxbContext.createMarshaller();
			try (OutputStream outputStream = rpcServerTransport.createResponseOutputStream()) {
				marshaller.marshal(response, outputStream);
			}
		} catch (RpcException x) {
			throw x;
		} catch (RuntimeException x) {
			throw x;
		} catch (Exception x) {
			throw new RpcException(x);
		}
	}

	protected Response process(final Request request) {
		assertNotNull(request, "request");
		final Uid requestId = assertNotNull(request.getRequestId(), "request.requestId");
		final HostId serverHostId = request.getServerHostId();
		if (HostId.CENTRAL.equals(serverHostId)
				|| rpcContext.getLocalHostId().equals(serverHostId)) {
			final RpcServiceExecutor rpcServiceExecutor = rpcContext.getRpcServiceExecutor();
			if (! (request instanceof DeferredResponseRequest)) // not putting this! we're fetching a response for an old request.
				rpcServiceExecutor.putRequest(request);

			Response response = rpcServiceExecutor.pollResponse(requestId, LOW_LEVEL_TIMEOUT);
			if (response == null) {
				response = new DeferringResponse();
				response.copyRequestCoordinates(request); // warning! this might be a DeferredResponseRequest -- not the original request! but currently, this does not matter as the data copied is the same.
			}

			return response;
		}
		throw new UnsupportedOperationException("NYI");
	}

}
