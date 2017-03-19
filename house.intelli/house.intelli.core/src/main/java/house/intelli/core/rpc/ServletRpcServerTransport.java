package house.intelli.core.rpc;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * {@link RpcServerTransport}s used in servlets and similar situations.
 * <p>
 * These situations are characterized by both {@link InputStream} and {@link OutputStream}
 * already existing and not being created by the {@code RpcServerTransport}.
 * @author mn
 */
public interface ServletRpcServerTransport extends RpcServerTransport {

	InputStream getInputStream();

	void setInputStream(InputStream inputStream);

	OutputStream getOutputStream();

	void setOutputStream(OutputStream outputStream);

}
