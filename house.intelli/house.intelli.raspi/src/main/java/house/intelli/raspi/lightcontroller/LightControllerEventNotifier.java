package house.intelli.raspi.lightcontroller;

import static house.intelli.core.util.AssertUtil.*;
import static java.util.Objects.*;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import house.intelli.core.rpc.HostId;
import house.intelli.core.rpc.RpcClient;
import house.intelli.core.rpc.RpcContext;
import house.intelli.core.rpc.RpcException;
import house.intelli.core.rpc.lightcontroller.LightControllerEventRequest;
import house.intelli.core.rpc.lightcontroller.LightControllerState;
import house.intelli.raspi.LightController;
import house.intelli.raspi.LightControllerImpl;

@Component
public class LightControllerEventNotifier {

	private static final Logger logger = LoggerFactory.getLogger(LightControllerEventNotifier.class);

	private ApplicationContext applicationContext;

	private List<LightControllerImpl> lightControllers = Collections.emptyList();

	private RpcContext rpcContext;

	private final ExecutorService executorService = Executors.newCachedThreadPool();

	private final Map<String, Set<HostId>> beanId2listenerHostIds = new HashMap<>();

	private LightControllerImpl ignoredLightController;

	private final PropertyChangeListener statePropertyChangeListener = new PropertyChangeListener() {
		@Override
		public void propertyChange(PropertyChangeEvent event) {
			assertEventThread();
			final LightControllerImpl lightController = (LightControllerImpl) event.getSource();
			if (ignoredLightController == lightController)
				return;

			final LightControllerState state = lightController.getState();
			final String beanId = lightController.getBeanName();
			if (beanId == null) {
				logger.error("statePropertyChangeListener.propertyChange: beanId not found for " + lightController);
				return;
			}

			Set<HostId> listenerHostIds = beanId2listenerHostIds.get(beanId);
			logger.debug("statePropertyChangeListener.propertyChange: beanId={}, state={}, listenerHostIds={}", beanId, state, listenerHostIds);
			if (listenerHostIds == null)
				return;

			for (final HostId listenerHostId : listenerHostIds) {
				executorService.submit(new Runnable() {
					@Override
					public void run() {
						try {
							invokeLightControllerEventRequest(listenerHostId, beanId, state);
						} catch (Exception x) {
							logger.error("statePropertyChangeListener.propertyChange.run: " + x + ' ', x);
						}
					}
				});
			}
		}
	};

	protected void invokeLightControllerEventRequest(final HostId listenerHostId, final String beanId, final LightControllerState state) throws RpcException {
		logger.debug("invokeKeyButtonSensorEventRequest: listenerHostId={}, beanId={}, state={}", listenerHostId, beanId, state);
		LightControllerEventRequest request = new LightControllerEventRequest();
		request.setServerHostId(listenerHostId);
		request.setChannelId(beanId);
		request.setLightControllerState(state);
		try (RpcClient rpcClient = rpcContext.createRpcClient()) {
			rpcClient.invoke(request);
		}
	}

	public LightControllerEventNotifier() {
		logger.info("<init>");
	}

	public LightControllerImpl getIgnoredLightController() {
		assertEventThread();
		return ignoredLightController;
	}
	public void setIgnoredLightController(LightControllerImpl ignoredLightController) {
		assertEventThread();
		this.ignoredLightController = ignoredLightController;
	}

	public void addListener(final String beanId, final HostId listenerHostId) {
		requireNonNull(beanId, "beanId");
		requireNonNull(listenerHostId, "listenerHostId");
		assertEventThread();
		Set<HostId> listenerHostIds = beanId2listenerHostIds.get(beanId);
		if (listenerHostIds == null) {
			listenerHostIds = new HashSet<>(1);
			beanId2listenerHostIds.put(beanId, listenerHostIds);
		}
		listenerHostIds.add(listenerHostId);
	}

	public ApplicationContext getApplicationContext() {
		return applicationContext;
	}

	@Autowired
	public void setApplicationContext(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	public RpcContext getRpcContext() {
		return rpcContext;
	}

	@Autowired
	public void setRpcContext(RpcContext rpcContext) {
		this.rpcContext = rpcContext;
	}

	public List<LightControllerImpl> getLightControllers() {
		return lightControllers;
	}

	@Autowired(required = false)
	public void setLightControllers(List<LightControllerImpl> lightControllers) {
		assertEventThread();
		logger.debug("setLightControllers: lightControllers={}", lightControllers);

		List<LightControllerImpl> oldLightControllers = this.lightControllers;
		for (LightControllerImpl lightController : oldLightControllers)
			lightController.removePropertyChangeListener(LightController.PropertyEnum.state, statePropertyChangeListener);

		if (lightControllers == null)
			this.lightControllers = Collections.emptyList();
		else {
			for (LightControllerImpl lightController : lightControllers)
				lightController.addPropertyChangeListener(LightController.PropertyEnum.state, statePropertyChangeListener);

			this.lightControllers = Collections.unmodifiableList(new ArrayList<>(lightControllers));
		}
	}
}
