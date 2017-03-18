package house.intelli.pgp.rpc;

import static house.intelli.core.util.AssertUtil.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import house.intelli.core.rpc.HttpRpcClientTransport;
import house.intelli.core.rpc.Request;
import house.intelli.core.rpc.Response;

public class PgpHttpRpcClientTransport extends HttpRpcClientTransport {

	public PgpHttpRpcClientTransport() {
	}

	@Override
	public void sendRequest(Request<?> request) throws IOException {
		assertNotNull(request, "request");
		PgpRequest pgpRequest = new PgpRequest();
		pgpRequest.copyRequestCoordinates(request);

		byte[] plainRequest;
		try {
			ByteArrayOutputStream bout = new ByteArrayOutputStream();
			Marshaller marshaller = getJaxbContext().createMarshaller();
			marshaller.marshal(request, bout);

		} catch (JAXBException e) {
			throw new IOException(e);
		}

		super.sendRequest(pgpRequest);
	}

	@Override
	public Response receiveResponse() throws IOException {
		Response res = super.receiveResponse();
		PgpResponse pgpResponse = (PgpResponse) assertNotNull(res, "res");


//		return response;
		throw new UnsupportedOperationException("NYI");
	}

}
