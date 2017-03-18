package house.intelli.pgp.rpc;

import static house.intelli.core.util.AssertUtil.*;

import java.io.IOException;

import house.intelli.core.rpc.JaxbRpcServerTransport;
import house.intelli.core.rpc.Request;
import house.intelli.core.rpc.Response;

public abstract class PgpJaxbRpcServerTransport extends JaxbRpcServerTransport {

	@Override
	public void sendResponse(Response response) throws IOException {
		assertNotNull(response, "response");
		PgpResponse pgpResponse = new PgpResponse();
		pgpResponse.copyRequestCoordinates(response);


		super.sendResponse(pgpResponse);
	}

	@Override
	public Request<?> receiveRequest() throws IOException {
		Request<?> req = super.receiveRequest();
		PgpRequest pgpRequest = (PgpRequest) assertNotNull(req, "req");


//		return r;
		throw new UnsupportedOperationException("NYI");
	}

}
