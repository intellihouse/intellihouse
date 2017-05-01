package house.intelli.raspi.rpc.relay;

import static house.intelli.core.util.AssertUtil.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import house.intelli.core.event.EventQueue;
import house.intelli.core.rpc.AbstractRpcService;
import house.intelli.core.rpc.relay.RelayActorReadRequest;
import house.intelli.core.rpc.relay.RelayActorReadResponse;
import house.intelli.raspi.RelayActor;

@Component
public class RelayActorReadRpcService extends AbstractRpcService<RelayActorReadRequest, RelayActorReadResponse> {

	private static final Logger logger = LoggerFactory.getLogger(RelayActorReadRpcService.class);

	private ApplicationContext applicationContext;

	public RelayActorReadRpcService() {
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
	public RelayActorReadResponse process(RelayActorReadRequest request) throws Exception {
		final String channelId = assertNotNull(request.getChannelId(), "request.channelId");
		final Object bean = applicationContext.getBean(channelId);
		if (bean == null)
			throw new IllegalArgumentException("No bean found with beanId=channelId=" + channelId);

		if (! (bean instanceof RelayActor))
			throw new IllegalArgumentException("Bean with beanId=channelId=" + channelId + " is not an instance of RelayActor, but: " + bean.getClass().getName());

		final RelayActor relayActor = (RelayActor) bean;

		final RelayActorReadResponse[] response = new RelayActorReadResponse[1];
		EventQueue.invokeAndWait(() -> {
			response[0] = new RelayActorReadResponse();
			response[0].setEnergized(relayActor.isEnergized());
		});
		return response[0];
	}

}
