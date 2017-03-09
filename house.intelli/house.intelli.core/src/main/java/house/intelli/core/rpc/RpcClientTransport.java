package house.intelli.core.rpc;

import java.io.InputStream;
import java.io.OutputStream;

public interface RpcClientTransport extends AutoCloseable {

//	private final InputStream inputStream;
//	private final OutputStream outputStream;
//
//	public RpcClientTransport(InputStream inputStream, OutputStream outputStream) {
//		this.inputStream = assertNotNull(inputStream, "inputStream");
//		this.outputStream = assertNotNull(outputStream, "outputStream");
//	}
//
//	public InputStream getInputStream() {
//		return inputStream;
//	}
//	public OutputStream getOutputStream() {
//		return outputStream;
//	}
//
//	@Override
//	public void close() throws IOException {
//		// TODO implement!
//	}
//
//	public void flushRequest() throws IOException {
//		outputStream.flush();
//	}

	OutputStream createRequestOutputStream();

	InputStream createResponseInputStream();
}
