package house.intelli.core.rpc;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Response type used to mask <code>null</code>.
 * <p>
 * If {@link RpcService#process(Request)} returns <code>null</code>, an instance of {@code NullResponse}
 * is created to transmit the information about the successful completion of the service method.
 * On the client side, the {@code NullResponse} is converted back to <code>null</code>. The API
 * consumer should thus never see a {@code NullResponse} instance.
 *
 * @author mn
 */
@XmlRootElement
public class NullResponse extends Response {

	public NullResponse() {
	}

}
