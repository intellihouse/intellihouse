package house.intelli.core.rpc;

import static house.intelli.core.util.AssertUtil.*;

import java.io.InputStream;
import java.io.OutputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import house.intelli.core.jaxb.IntelliHouseJaxbContext;

public class RpcServer {

//	private RpcServerTransport rpcServerTransport;
//
//	public RpcServer(RpcServerTransport rpcServerTransport) {
//		this.rpcServerTransport = assertNotNull(rpcServerTransport, "rpcServerTransport");
//	}

	public void receiveAndProcessRequest(RpcServerTransport rpcServerTransport) throws RpcException {
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
			}
			assertNotNull(response, "response");

			if (request != null)
				copyRequestCoordinates(response, request);

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

	protected void copyRequestCoordinates(Response response, Request request) {
		assertNotNull(response, "response");
		assertNotNull(request, "request");
		response.setClient(request.getClient());
		response.setRequestId(request.getRequestId());
		response.setServer(request.getServer());
	}

	protected Response process(Request request) {
		assertNotNull(request, "request");
//		if (RpcMessage.CENTER.equals(request.getServer())) {
//
//		}
		throw new UnsupportedOperationException("NYI");
	}



}
