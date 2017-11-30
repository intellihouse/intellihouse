package house.intelli.core.rpc.lightcontroller;

import java.util.HashSet;
import java.util.Set;

import javax.xml.bind.annotation.XmlRootElement;

import house.intelli.core.rpc.RemoteBeanRef;
import house.intelli.core.rpc.VoidResponse;
import house.intelli.core.rpc.channel.ChannelRequest;

@XmlRootElement
public class LightControllerFederationPropagationRequest extends ChannelRequest<VoidResponse> {

	private String sourceBeanId;

	private Set<RemoteBeanRef> federatedLightControllers;

	private LightControllerState lightControllerState;

	/**
	 * Gets the name of the {@code LightControllerImpl}-bean in the Spring-context of the VM sending this request.
	 * @return name of the {@code LightControllerImpl}-bean in the Spring-context of the VM sending this request.
	 */
	public String getSourceBeanId() {
		return sourceBeanId;
	}
	public void setSourceBeanId(String sourceBeanId) {
		this.sourceBeanId = sourceBeanId;
	}

	public Set<RemoteBeanRef> getFederatedLightControllers() {
		if (federatedLightControllers == null)
			federatedLightControllers = new HashSet<>();

		return federatedLightControllers;
	}
	public void setFederatedLightControllers(Set<RemoteBeanRef> federatedLightControllers) {
		this.federatedLightControllers = federatedLightControllers;
	}

	public LightControllerState getLightControllerState() {
		return lightControllerState;
	}
	public void setLightControllerState(LightControllerState lightControllerState) {
		this.lightControllerState = lightControllerState;
	}
}
