package house.intelli.raspi.rpc.keybutton;

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
import house.intelli.core.rpc.keybutton.KeyButtonSensorEventRequest;
import house.intelli.raspi.KeyButtonSensor;

@Component
public class KeyButtonSensorEventNotifier {

	private static final Logger logger = LoggerFactory.getLogger(KeyButtonSensorEventNotifier.class);

	private ApplicationContext applicationContext;

	private List<KeyButtonSensor> keyButtonSensors = Collections.emptyList();

	private RpcContext rpcContext;

	private final ExecutorService executorService = Executors.newCachedThreadPool();

//	private final IdentityHashMap<KeyButtonSensor, String> keyButtonSensor2BeanId = new IdentityHashMap<>();

	private final Map<String, Set<HostId>> beanId2listenerHostIds = new HashMap<>();

	private final PropertyChangeListener downPropertyChangeListener = new PropertyChangeListener() {
		@Override
		public void propertyChange(PropertyChangeEvent event) {
			assertEventThread();
//			if (keyButtonSensor2BeanId.isEmpty()) {
//				Map<String, KeyButtonSensor> beanId2KeyButtonSensor = applicationContext.getBeansOfType(KeyButtonSensor.class);
//				for (Map.Entry<String, KeyButtonSensor> me : beanId2KeyButtonSensor.entrySet())
//					keyButtonSensor2BeanId.put(me.getValue(), me.getKey());
//			}
			final KeyButtonSensor keyButtonSensor = (KeyButtonSensor) event.getSource();
			final boolean down = keyButtonSensor.isDown();
//			final String beanId = keyButtonSensor2BeanId.get(keyButtonSensor);
			final String beanId = keyButtonSensor.getBeanName();
			if (beanId == null) {
				logger.error("downPropertyChangeListener.propertyChange: beanId not found for " + keyButtonSensor);
				return;
			}

			Set<HostId> listenerHostIds = beanId2listenerHostIds.get(beanId);
			logger.debug("downPropertyChangeListener.propertyChange: beanId={}, down={}, listenerHostIds={}", beanId, down, listenerHostIds);
			if (listenerHostIds == null)
				return;

			for (HostId listenerHostId : listenerHostIds) {
				executorService.submit(new Runnable() {
					@Override
					public void run() {
						try {
							invokeKeyButtonSensorEventRequest(listenerHostId, beanId, down);
						} catch (Exception x) {
							logger.error("downPropertyChangeListener.propertyChange.run: " + x + ' ', x);
						}
					}
				});
			}
		}
	};

	protected void invokeKeyButtonSensorEventRequest(final HostId listenerHostId, final String beanId, final boolean down) throws RpcException {
		logger.debug("invokeKeyButtonSensorEventRequest: listenerHostId={}, beanId={}, down={}", listenerHostId, beanId, down);
		KeyButtonSensorEventRequest request = new KeyButtonSensorEventRequest();
		request.setServerHostId(listenerHostId);
		request.setChannelId(beanId); // SENDER id!
		request.setDown(down);
		try (RpcClient rpcClient = rpcContext.createRpcClient()) {
			rpcClient.invoke(request);
		}
	}

	public KeyButtonSensorEventNotifier() {
		logger.info("<init>");
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

	public List<KeyButtonSensor> getKeyButtonSensors() {
		return keyButtonSensors;
	}

	@Autowired(required = false)
	public void setKeyButtonSensors(List<KeyButtonSensor> keyButtonSensors) {
		assertEventThread();
		logger.debug("setKeyButtonSensors: keyButtonSensors={}", keyButtonSensors);

		List<KeyButtonSensor> oldKeyButtonSensors = this.keyButtonSensors;
		for (KeyButtonSensor keyButtonSensor : oldKeyButtonSensors)
			keyButtonSensor.removePropertyChangeListener(KeyButtonSensor.PropertyEnum.down, downPropertyChangeListener);

		if (keyButtonSensors == null)
			this.keyButtonSensors = Collections.emptyList();
		else {
			for (KeyButtonSensor keyButtonSensor : keyButtonSensors)
				keyButtonSensor.addPropertyChangeListener(KeyButtonSensor.PropertyEnum.down, downPropertyChangeListener);

			this.keyButtonSensors = Collections.unmodifiableList(new ArrayList<>(keyButtonSensors));
		}
	}
}
