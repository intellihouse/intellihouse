package house.intelli.core.jaxb;

import java.util.HashSet;
import java.util.Set;

import house.intelli.core.rpc.DeferredResponseRequest;
import house.intelli.core.rpc.DeferringResponse;
import house.intelli.core.rpc.ErrorResponse;
import house.intelli.core.rpc.NullResponse;
import house.intelli.core.rpc.PollInverseRequestsRequest;
import house.intelli.core.rpc.PollInverseRequestsResponse;
import house.intelli.core.rpc.PutInverseResponseRequest;
import house.intelli.core.rpc.RpcService;
import house.intelli.core.rpc.RpcServiceRegistry;
import house.intelli.core.rpc.dimmer.DimmerActorEventRequest;
import house.intelli.core.rpc.dimmer.DimmerActorReadRequest;
import house.intelli.core.rpc.dimmer.DimmerActorReadResponse;
import house.intelli.core.rpc.dimmer.DimmerActorWriteRequest;
import house.intelli.core.rpc.dimmer.DimmerActorWriteResponse;
import house.intelli.core.rpc.keybutton.KeyButtonSensorEventRequest;
import house.intelli.core.rpc.keybutton.KeyButtonSensorRemotePropagationRequest;
import house.intelli.core.rpc.lightcontroller.LightControllerEventRequest;
import house.intelli.core.rpc.lightcontroller.LightControllerFederationPropagationRequest;
import house.intelli.core.rpc.relay.RelayActorEventRequest;
import house.intelli.core.rpc.relay.RelayActorReadRequest;
import house.intelli.core.rpc.relay.RelayActorReadResponse;
import house.intelli.core.rpc.relay.RelayActorWriteRequest;
import house.intelli.core.rpc.relay.RelayActorWriteResponse;

public class IntelliHouseJaxbContextProviderImpl extends AbstractIntelliHouseJaxbContextProvider {

	@Override
	public Class<?>[] getClassesToBeBound() {
		Set<Class<?>> classes = new HashSet<>();

		// automatically enlist all Request and Response sub-classes used by the RpcServices.
		for (RpcService<?, ?> rpcService : RpcServiceRegistry.getInstance().getRpcServices()) {
			classes.add(rpcService.getRequestType());
			classes.add(rpcService.getResponseType());
		}

		// manually add other classes below...
		// BEGIN framework stuff
		classes.add(DeferredResponseRequest.class);
		classes.add(DeferringResponse.class);
		classes.add(ErrorResponse.class);
		classes.add(NullResponse.class);
		classes.add(PollInverseRequestsRequest.class);
		classes.add(PollInverseRequestsResponse.class);
		classes.add(PutInverseResponseRequest.class);
		// END framework stuff

		// BEGIN RpcService-related DTOs for which there might be no RpcService registered
		classes.add(DimmerActorEventRequest.class);
		classes.add(DimmerActorReadRequest.class);
		classes.add(DimmerActorReadResponse.class);
		classes.add(DimmerActorWriteRequest.class);
		classes.add(DimmerActorWriteResponse.class);

		classes.add(RelayActorEventRequest.class);
		classes.add(RelayActorReadRequest.class);
		classes.add(RelayActorReadResponse.class);
		classes.add(RelayActorWriteRequest.class);
		classes.add(RelayActorWriteResponse.class);

		classes.add(KeyButtonSensorEventRequest.class);
		classes.add(KeyButtonSensorRemotePropagationRequest.class);

		classes.add(LightControllerEventRequest.class);
		classes.add(LightControllerFederationPropagationRequest.class);
		// END RpcService-related DTOs for which there might be no RpcService registered

		return classes.toArray(new Class<?>[classes.size()]);
	}

}
