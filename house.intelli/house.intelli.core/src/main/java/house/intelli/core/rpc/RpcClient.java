package house.intelli.core.rpc;

import static house.intelli.core.util.AssertUtil.*;

import java.io.OutputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import house.intelli.core.jaxb.IntelliHouseJaxbContext;

public class RpcClient {

	private ConnectionProvider connectionProvider;

	public <REQ extends Request, RES extends Response> RES invoke(REQ request) throws RpcException {
		try {
			JAXBContext jaxbContext = IntelliHouseJaxbContext.getJaxbContext();
			try (Connection connection = connectionProvider.openConnection()) {
				Marshaller marshaller = jaxbContext.createMarshaller();
				OutputStream outputStream = connection.getOutputStream();
				marshaller.marshal(request, outputStream);
				outputStream.flush();

				Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
				Object unmarshalled = unmarshaller.unmarshal(connection.getInputStream());
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
