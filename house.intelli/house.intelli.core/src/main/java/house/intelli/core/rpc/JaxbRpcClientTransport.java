package house.intelli.core.rpc;

import static house.intelli.core.util.AssertUtil.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import house.intelli.core.jaxb.IntelliHouseJaxbContext;

public abstract class JaxbRpcClientTransport extends AbstractRpcClientTransport {
	private JAXBContext jaxbContext;

	protected abstract OutputStream createRequestOutputStream() throws IOException;

	protected abstract InputStream createResponseInputStream() throws IOException;

	@Override
	public void sendRequest(final Request request) throws IOException {
		assertNotNull(request, "request");
		JAXBContext jaxbContext = getJaxbContext();
		try (OutputStream outputStream = createRequestOutputStream()) {
			Marshaller marshaller = jaxbContext.createMarshaller();
			marshaller.marshal(request, outputStream);
		} catch (JAXBException e) {
			throw new IOException(e);
		}
	}

	@Override
	public Response receiveResponse() throws IOException {
		JAXBContext jaxbContext = getJaxbContext();
		Object unmarshalled;
		try (InputStream inputStream = createResponseInputStream()) {
			Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
			unmarshalled = unmarshaller.unmarshal(inputStream);
		} catch (JAXBException e) {
			throw new IOException(e);
		}
		return (Response) unmarshalled;
	}

	protected JAXBContext getJaxbContext() {
		if (jaxbContext == null)
			jaxbContext = IntelliHouseJaxbContext.getJaxbContext();

		return jaxbContext;
	}
}
