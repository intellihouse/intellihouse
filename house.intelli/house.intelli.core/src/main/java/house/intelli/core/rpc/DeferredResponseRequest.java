package house.intelli.core.rpc;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Request used to obtain the real response, after having received a {@link DeferringResponse}.
 * @author mn
 */
@XmlRootElement
public class DeferredResponseRequest extends Request {

	public DeferredResponseRequest() {
	}

}
