package house.intelli.core.rpc;

import static house.intelli.core.util.AssertUtil.*;

import java.io.InputStream;
import java.io.OutputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import house.intelli.core.Uid;
import house.intelli.core.jaxb.IntelliHouseJaxbContext;

public class RpcClient {

	private final RpcContext rpcContext;
	private final RpcClientTransportProvider rpcClientTransportProvider;

	protected RpcClient(final RpcContext rpcContext) {
		this.rpcContext = assertNotNull(rpcContext, "rpcContext");
		this.rpcClientTransportProvider = assertNotNull(this.rpcContext.getRpcClientTransportProvider(), "rpcContext.rpcClientTransportProvider");
	}

	public <REQ extends Request, RES extends Response> RES invoke(REQ request) throws RpcException {
		assertNotNull(request, "request");
		assertNotNull(request.getClientHostId(), "request.clientChannelId");
		assertNotNull(request.getServerHostId(), "request.serverChannelId");

		if (request.getRequestId() == null)
			request.setRequestId(new Uid());

		if (request.getTimeout() == Request.TIMEOUT_UNDEFINED)
			request.setTimeout(RpcConst.DEFAULT_REQUEST_TIMEOUT);

		try {
			JAXBContext jaxbContext = IntelliHouseJaxbContext.getJaxbContext();
			try (RpcClientTransport rpcClientTransport = rpcClientTransportProvider.createRpcClientTransport()) {
//				rpcClientTransport.setRequestTimeout(request.getTimeout());
				Marshaller marshaller = jaxbContext.createMarshaller();
				try (OutputStream outputStream = rpcClientTransport.createRequestOutputStream()) {
					marshaller.marshal(request, outputStream);
				}

				Object unmarshalled;
				Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
				try (InputStream inputStream = rpcClientTransport.createResponseInputStream()) {
					unmarshalled = unmarshaller.unmarshal(inputStream);
				}
				if (unmarshalled instanceof ErrorResponse) {
					ErrorResponse errorResponse = (ErrorResponse) unmarshalled;
					Error error = assertNotNull(errorResponse.getError(), "errorResponse.error");
					RemoteExceptionUtil.throwOriginalExceptionIfPossible(error);
					throw new RemoteException(error);
				}
				@SuppressWarnings("unchecked")
				RES response = (RES) unmarshalled;
				return response;
			}
		} catch (RpcException x) {
			throw x;
		} catch (RuntimeException x) {
			throw x;
		} catch (Exception x) {
			throw new RpcException(x);
		}
	}
}