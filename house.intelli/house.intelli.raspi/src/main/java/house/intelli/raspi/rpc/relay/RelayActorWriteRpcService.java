package house.intelli.raspi.rpc.relay;

import static java.util.Objects.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import house.intelli.core.event.EventQueue;
import house.intelli.core.rpc.AbstractRpcService;
import house.intelli.core.rpc.relay.RelayActorWriteRequest;
import house.intelli.core.rpc.relay.RelayActorWriteResponse;
import house.intelli.raspi.RelayActor;

@Component
public class RelayActorWriteRpcService extends AbstractRpcService<RelayActorWriteRequest, RelayActorWriteResponse> {

	private static final Logger logger = LoggerFactory.getLogger(RelayActorWriteRpcService.class);

	private ApplicationContext applicationContext;

	public RelayActorWriteRpcService() {
		logger.info("<init>");
	}

	public ApplicationContext getApplicationContext() {
		return applicationContext;
	}
	@Autowired
	public void setApplicationContext(ApplicationContext applicationContext) {
		logger.info("setApplicationContext: {}", applicationContext);
		this.applicationContext = applicationContext;
	}

	@Override
	public RelayActorWriteResponse process(RelayActorWriteRequest request) throws Exception {
		final String channelId = requireNonNull(request.getChannelId(), "request.channelId");
		final Object bean = applicationContext.getBean(channelId);
		if (bean == null)
			throw new IllegalArgumentException("No bean found with beanId=channelId=" + channelId);

		if (! (bean instanceof RelayActor))
			throw new IllegalArgumentException("Bean with beanId=channelId=" + channelId + " is not an instance of RelayActor, but: " + bean.getClass().getName());

		final RelayActor relayActor = (RelayActor) bean;

		final RelayActorWriteResponse[] response = new RelayActorWriteResponse[1];
		EventQueue.invokeAndWait(() -> {
			relayActor.setEnergized(request.isEnergized());
			response[0] = new RelayActorWriteResponse();
			response[0].setEnergized(relayActor.isEnergized());
		});
		return response[0];
	}

}
