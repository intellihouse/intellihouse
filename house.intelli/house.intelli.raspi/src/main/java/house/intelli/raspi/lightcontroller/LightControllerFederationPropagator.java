package house.intelli.raspi.lightcontroller;

import static house.intelli.core.util.AssertUtil.*;

import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import house.intelli.core.event.EventQueue;
import house.intelli.core.rpc.RemoteBeanRef;
import house.intelli.core.rpc.RpcClient;
import house.intelli.core.rpc.RpcContext;
import house.intelli.core.rpc.RpcException;
import house.intelli.core.rpc.lightcontroller.LightControllerFederationPropagationRequest;
import house.intelli.raspi.LightControllerImpl;

@Component
public class LightControllerFederationPropagator implements ApplicationContextAware {

	private static final Logger logger = LoggerFactory.getLogger(LightControllerFederationPropagator.class);

	private static final long PROPAGATION_PERIOD = 60L * 1000L;

	private List<LightControllerImpl> lightControllers = Collections.emptyList();

	private final Timer timer = new Timer("LightControllerFederationPropagator.timer", true);

	private RpcContext rpcContext;

	private ApplicationContext applicationContext;

	private final TimerTask timerTask = new TimerTask() {
		@Override
		public void run() {
			try {
				propagate();
			} catch (Exception x) {
				logger.error("timerTask.run: " + x.toString(), x);
			}
		}
	};

	public LightControllerFederationPropagator() {
		logger.debug("<init>");
		timer.schedule(timerTask, PROPAGATION_PERIOD, PROPAGATION_PERIOD);
	}

	public RpcContext getRpcContext() {
		return rpcContext;
	}

	@Autowired
	public void setRpcContext(RpcContext rpcContext) {
		this.rpcContext = rpcContext;
	}

	public ApplicationContext getApplicationContext() {
		return applicationContext;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	public List<LightControllerImpl> getLightControllers() {
		return lightControllers;
	}

	@Autowired(required = false)
	public void setLightControllers(List<LightControllerImpl> lightControllers) {
		assertEventThread();
		logger.debug("setLightControllers: lightControllers={}", lightControllers);
		this.lightControllers = lightControllers == null ? Collections.emptyList() : lightControllers;
	}

	protected void propagate() {
		for (LightControllerImpl lightController : getLightControllers()) {
			if (lightController.getCollectedFederatedLightControllers().isEmpty())
				continue;

			try {
				propagate(lightController);
			} catch (Exception x) {
				logger.error("propagate: " + x.toString(), x);
			}
		}
	}

	protected void propagate(LightControllerImpl lightController) throws RpcException {
		assertNotNull(lightController, "lightController");
		for (RemoteBeanRef remoteBeanRef : lightController.getCollectedFederatedLightControllers()) {
			try {
				propagate(lightController, remoteBeanRef);
			} catch (Exception x) {
				logger.error("propagate: " + x.toString(), x);

				if (! lightController.getFederatedLightControllers().contains(remoteBeanRef))
					lightController.getCollectedFederatedLightControllers().remove(remoteBeanRef);
			}
		}
	}

	protected void propagate(LightControllerImpl lightController, RemoteBeanRef remoteBeanRef) throws RpcException {
		assertNotNull(lightController, "lightController");
		assertNotNull(remoteBeanRef, "remoteBeanRef");

		logger.debug("propagate: {} => {}", lightController.getBeanName(), remoteBeanRef);

		final LightControllerEventNotifier eventNotifier = applicationContext.getBean(LightControllerEventNotifier.class);
		EventQueue.invokeLater(() -> {
			eventNotifier.addListener(lightController.getBeanName(), remoteBeanRef.getHostId());
		});

		LightControllerFederationPropagationRequest request = new LightControllerFederationPropagationRequest();
		request.setServerHostId(remoteBeanRef.getHostId()); // TO host
		request.setChannelId(remoteBeanRef.getBeanId()); // TO bean
		request.setSourceBeanId(assertNotNull(lightController.getBeanName(), "lightController.beanName")); // FROM bean
		request.setFederatedLightControllers(lightController.getCollectedFederatedLightControllers());
		request.setLightControllerState(lightController.getState());
		try (RpcClient rpcClient = rpcContext.createRpcClient()) {
			rpcClient.invoke(request);
		}
	}

}
