package house.intelli.raspi.rpc.keybutton;

import static house.intelli.core.util.AssertUtil.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import house.intelli.core.event.EventQueue;
import house.intelli.core.rpc.AbstractRpcService;
import house.intelli.core.rpc.VoidResponse;
import house.intelli.core.rpc.keybutton.KeyButtonSensorRemotePropagationRequest;
import house.intelli.raspi.KeyButtonSensor;

@Component
public class KeyButtonSensorRemotePropagationService extends AbstractRpcService<KeyButtonSensorRemotePropagationRequest, VoidResponse> {

	private static final Logger logger = LoggerFactory.getLogger(KeyButtonSensorRemotePropagationService.class);

	private ApplicationContext applicationContext;

	public KeyButtonSensorRemotePropagationService() {
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
	public VoidResponse process(KeyButtonSensorRemotePropagationRequest request) throws Exception {
		final String channelId = assertNotNull(request.getChannelId(), "request.channelId");
		logger.debug("process: clientHostId={}, channelId={}", request.getClientHostId(), channelId);

		final Object bean = applicationContext.getBean(channelId);
		if (bean == null)
			throw new IllegalArgumentException("No bean found with beanId=channelId=" + channelId);

		if (! (bean instanceof KeyButtonSensor))
			throw new IllegalArgumentException("Bean with beanId=channelId=" + channelId + " is not an instance of KeyButtonSensor, but: " + bean.getClass().getName());

		final KeyButtonSensorEventNotifier eventNotifier = applicationContext.getBean(KeyButtonSensorEventNotifier.class);
		EventQueue.invokeLater(() -> {
			eventNotifier.addListener(channelId, request.getClientHostId());
		});
		return null;
	}
}
