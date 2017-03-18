package house.intelli.core.rpc;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import house.intelli.core.jaxb.IntelliHouseJaxbContext;

public abstract class JaxbRpcServerTransport extends AbstractRpcServerTransport {
	private JAXBContext jaxbContext;

	protected abstract InputStream createRequestInputStream() throws IOException;

	protected abstract OutputStream createResponseOutputStream() throws IOException;

	@Override
	public Request<?> receiveRequest() throws IOException {
		JAXBContext jaxbContext = getJaxbContext();
		Object unmarshalled;
		try (InputStream inputStream = createRequestInputStream()) {
			Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
			unmarshalled = unmarshaller.unmarshal(inputStream);
		} catch (JAXBException x) {
			throw new IOException(x);
		}
		return (Request<?>) unmarshalled;
	}

	@Override
	public void sendResponse(Response response) throws IOException {
		JAXBContext jaxbContext = getJaxbContext();
		try (OutputStream outputStream = createResponseOutputStream()) {
			Marshaller marshaller = jaxbContext.createMarshaller();
			marshaller.marshal(response, outputStream);
		} catch (JAXBException x) {
			throw new IOException(x);
		}
	}

	protected JAXBContext getJaxbContext() {
		if (jaxbContext == null)
			jaxbContext = IntelliHouseJaxbContext.getJaxbContext();

		return jaxbContext;
	}
}
