package house.intelli.raspi.lightcontroller;

import static java.util.Objects.*;

import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import house.intelli.core.event.EventQueue;
import house.intelli.core.rpc.AbstractRpcService;
import house.intelli.core.rpc.RemoteBeanRef;
import house.intelli.core.rpc.VoidResponse;
import house.intelli.core.rpc.lightcontroller.LightControllerEventRequest;
import house.intelli.core.rpc.lightcontroller.LightControllerState;
import house.intelli.raspi.LightControllerImpl;

@Component
public class LightControllerEventService extends AbstractRpcService<LightControllerEventRequest, VoidResponse> {

	private static final Logger logger = LoggerFactory.getLogger(LightControllerEventService.class);

	private ApplicationContext applicationContext;

	public ApplicationContext getApplicationContext() {
		return applicationContext;
	}
	@Autowired
	public void setApplicationContext(ApplicationContext applicationContext) {
		logger.info("setApplicationContext: {}", applicationContext);
		this.applicationContext = applicationContext;
	}

	@Override
	public VoidResponse process(final LightControllerEventRequest request) throws Exception {
		final RemoteBeanRef remoteBeanRef = new RemoteBeanRef();
		remoteBeanRef.setHostId(requireNonNull(request.getClientHostId(), "request.clientHostId"));
		remoteBeanRef.setBeanId(requireNonNull(request.getChannelId(), "request.channelId"));
		final LightControllerState state = requireNonNull(request.getLightControllerState(), "request.lightControllerState");
		logger.debug("process: remoteBeanRef={}, state={}", remoteBeanRef, state);

		final List<LightControllerImpl> lightControllers = new LinkedList<>();
		for (LightControllerImpl lightController : applicationContext.getBeansOfType(LightControllerImpl.class).values()) {
			if (lightController.getCollectedFederatedLightControllers().contains(remoteBeanRef)) {
				lightControllers.add(lightController);
			}
		}

		if (lightControllers.isEmpty())
			return null;

		final LightControllerEventNotifier eventNotifier = applicationContext.getBean(LightControllerEventNotifier.class);
		EventQueue.invokeLater(() -> {
			for (final LightControllerImpl lightController : lightControllers) {
				eventNotifier.setIgnoredLightController(lightController);
				try {
					lightController.setState(state);
				} finally {
					eventNotifier.setIgnoredLightController(null);
				}
			}
		});
		return null;
	}
}
