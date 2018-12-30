package house.intelli.core.rpc.pv;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import house.intelli.core.rpc.Request;
import house.intelli.core.rpc.VoidResponse;

@XmlRootElement
public class PvStatusEventRequest extends Request<VoidResponse> {

	private List<PvStatus> pvStatuses;

	public PvStatusEventRequest() {
	}

	@XmlElement(name="pvStatus")
	public List<PvStatus> getPvStatuses() {
		if (pvStatuses == null)
			pvStatuses = new ArrayList<>();

		return pvStatuses;
	}
	public void setPvStatuses(List<PvStatus> pvStatuses) {
		this.pvStatuses = pvStatuses;
	}
}
