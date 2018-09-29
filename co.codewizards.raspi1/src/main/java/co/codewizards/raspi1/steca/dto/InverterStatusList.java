package co.codewizards.raspi1.steca.dto;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class InverterStatusList {

	private List<InverterStatus> inverterStatuses;

	public InverterStatusList() {
	}

	@XmlElement(name="inverterStatus")
	public List<InverterStatus> getInverterStatuses() {
		return inverterStatuses;
	}
	public void setInverterStatuses(List<InverterStatus> inverterStatuses) {
		this.inverterStatuses = inverterStatuses;
	}
}
