package house.intelli.core.rpc;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Intermediate response indicating that the real response is deferred.
 * @author mn
 */
@XmlRootElement
public class DeferringResponse extends Response {

	public DeferringResponse() {
	}

}
