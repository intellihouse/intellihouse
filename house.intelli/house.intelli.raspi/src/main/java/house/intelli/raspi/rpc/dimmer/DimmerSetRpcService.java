package house.intelli.raspi.rpc.dimmer;

import static house.intelli.core.util.AssertUtil.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import house.intelli.core.event.EventQueue;
import house.intelli.core.rpc.AbstractRpcService;
import house.intelli.core.rpc.dimmer.DimmerSetRequest;
import house.intelli.core.rpc.dimmer.DimmerSetResponse;
import house.intelli.raspi.DimmerActor;

@Component
public class DimmerSetRpcService extends AbstractRpcService<DimmerSetRequest, DimmerSetResponse> {

	private static final Logger logger = LoggerFactory.getLogger(DimmerSetRpcService.class);

	private ApplicationContext applicationContext;

	public DimmerSetRpcService() {
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
	public DimmerSetResponse process(DimmerSetRequest request) throws Exception {
		final String channelId = assertNotNull(request.getChannelId(), "request.channelId");
		Object bean = applicationContext.getBean(channelId);
		if (bean == null)
			throw new IllegalArgumentException("No bean found with beanId=channelId=" + channelId);

		if (! (bean instanceof DimmerActor))
			throw new IllegalArgumentException("Bean with beanId=channelId=" + channelId + " is not an instance of DimmerActor, but: " + bean.getClass().getName());

		DimmerSetResponse[] response = new DimmerSetResponse[1];
		EventQueue.invokeAndWait(new Runnable() {
			@Override
			public void run() {
				@SuppressWarnings("resource") // not newly opened! it exists in the applicationContext.
				DimmerActor dimmerActor = (DimmerActor) bean;
				dimmerActor.setDimmerValue(request.getDimmerValue());

				response[0] = new DimmerSetResponse();
				response[0].setDimmerValue(dimmerActor.getDimmerValue());
			}
		});
		return response[0];
	}

}
