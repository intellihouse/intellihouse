package house.intelli.raspi.lightcontroller;

import static house.intelli.core.util.AssertUtil.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import house.intelli.core.event.EventQueue;
import house.intelli.core.rpc.AbstractRpcService;
import house.intelli.core.rpc.RemoteBeanRef;
import house.intelli.core.rpc.VoidResponse;
import house.intelli.core.rpc.lightcontroller.LightControllerFederationPropagationRequest;
import house.intelli.raspi.LightControllerImpl;

@Component
public class LightControllerFederationPropagationService extends AbstractRpcService<LightControllerFederationPropagationRequest, VoidResponse> {

	private static final Logger logger = LoggerFactory.getLogger(LightControllerFederationPropagationService.class);

	private ApplicationContext applicationContext;

	public LightControllerFederationPropagationService() {
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
	public VoidResponse process(final LightControllerFederationPropagationRequest request) throws Exception {
		final String channelId = assertNotNull(request.getChannelId(), "request.channelId");
		final String sourceBeanId = assertNotNull(request.getSourceBeanId(), "request.sourceBeanId");
		logger.debug("process: clientHostId={}, sourceBeanId={}, channelId={}, federatedLightControllers={}",
				request.getClientHostId(), sourceBeanId, channelId, request.getFederatedLightControllers());

		final Object bean = applicationContext.getBean(channelId);
		if (bean == null)
			throw new IllegalArgumentException("No bean found with beanId=channelId=" + channelId);

		if (! (bean instanceof LightControllerImpl))
			throw new IllegalArgumentException("Bean with beanId=channelId=" + channelId + " is not an instance of LightControllerImpl, but: " + bean.getClass().getName());


		@SuppressWarnings("resource")
		final LightControllerImpl lightController = (LightControllerImpl) bean;
		lightController.getCollectedFederatedLightControllers().addAll(request.getFederatedLightControllers());

		final RemoteBeanRef sourceRemoteBeanRef = new RemoteBeanRef();
		sourceRemoteBeanRef.setHostId(request.getClientHostId());
		sourceRemoteBeanRef.setBeanId(sourceBeanId);

		final boolean added = lightController.getCollectedFederatedLightControllers().add(sourceRemoteBeanRef);

		final LightControllerEventNotifier eventNotifier = applicationContext.getBean(LightControllerEventNotifier.class);
		EventQueue.invokeLater(() -> {
			eventNotifier.addListener(channelId, request.getClientHostId());

			if (added)
				lightController.setState(request.getLightControllerState());
		});
		return null;
	}
}
