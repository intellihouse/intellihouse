package house.intelli.raspi.rpc.dimmer;

import static house.intelli.core.util.AssertUtil.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import house.intelli.core.event.EventQueue;
import house.intelli.core.rpc.AbstractRpcService;
import house.intelli.core.rpc.dimmer.DimmerActorWriteRequest;
import house.intelli.core.rpc.dimmer.DimmerActorWriteResponse;
import house.intelli.raspi.DimmerActor;

@Component
public class DimmerActorWriteRpcService extends AbstractRpcService<DimmerActorWriteRequest, DimmerActorWriteResponse> {

	private static final Logger logger = LoggerFactory.getLogger(DimmerActorWriteRpcService.class);

	private ApplicationContext applicationContext;

	public DimmerActorWriteRpcService() {
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
	public DimmerActorWriteResponse process(DimmerActorWriteRequest request) throws Exception {
		final String channelId = assertNotNull(request.getChannelId(), "request.channelId");
		final Object bean = applicationContext.getBean(channelId);
		if (bean == null)
			throw new IllegalArgumentException("No bean found with beanId=channelId=" + channelId);

		if (! (bean instanceof DimmerActor))
			throw new IllegalArgumentException("Bean with beanId=channelId=" + channelId + " is not an instance of DimmerActor, but: " + bean.getClass().getName());

		final DimmerActor dimmerActor = (DimmerActor) bean;

		final DimmerActorWriteResponse[] response = new DimmerActorWriteResponse[1];
		EventQueue.invokeAndWait(() -> {
			dimmerActor.setDimmerValue(request.getDimmerValue());
			response[0] = new DimmerActorWriteResponse();
			response[0].setDimmerValue(dimmerActor.getDimmerValue());
		});
		return response[0];
	}

}
