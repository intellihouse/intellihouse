package house.intelli.core.rpc;

import static java.util.Objects.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import house.intelli.core.io.NoCloseInputStream;
import house.intelli.core.io.NoCloseOutputStream;

public class HttpRpcServerTransport extends JaxbRpcServerTransport implements ServletRpcServerTransport {

	private InputStream inputStream;
	private OutputStream outputStream;

	@Override
	public InputStream getInputStream() {
		return inputStream;
	}
	@Override
	public void setInputStream(InputStream inputStream) {
		this.inputStream = inputStream;
	}
	@Override
	public OutputStream getOutputStream() {
		return outputStream;
	}
	@Override
	public void setOutputStream(OutputStream outputStream) {
		this.outputStream = outputStream;
	}

	@Override
	protected InputStream createRequestInputStream() throws IOException {
		// Must *not* be closable, because the InputStream is not opened by this method.
		return new NoCloseInputStream(requireNonNull(getInputStream(), "inputStream"));
	}

	@Override
	protected OutputStream createResponseOutputStream() throws IOException {
		// Must *not* be closable, because the OutputStream is not opened by this method.
		return new NoCloseOutputStream(requireNonNull(getOutputStream(), "outputStream"));
	}
}
