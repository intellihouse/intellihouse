package house.intelli.core.rpc;

import static house.intelli.core.util.AssertUtil.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Connection implements AutoCloseable {

	private final InputStream inputStream;
	private final OutputStream outputStream;

	public Connection(InputStream inputStream, OutputStream outputStream) {
		this.inputStream = assertNotNull(inputStream, "inputStream");
		this.outputStream = assertNotNull(outputStream, "outputStream");
	}

	public InputStream getInputStream() {
		return inputStream;
	}
	public OutputStream getOutputStream() {
		return outputStream;
	}

	@Override
	public void close() throws IOException {
		// TODO implement!
	}
}
