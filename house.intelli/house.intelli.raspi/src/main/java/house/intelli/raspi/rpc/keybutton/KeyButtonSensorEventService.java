package house.intelli.raspi.rpc.keybutton;

import static house.intelli.core.util.AssertUtil.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import house.intelli.core.event.EventQueue;
import house.intelli.core.rpc.AbstractRpcService;
import house.intelli.core.rpc.VoidResponse;
import house.intelli.core.rpc.keybutton.KeyButtonSensorEventRequest;
import house.intelli.raspi.KeyButtonSensorRemote;
import house.intelli.raspi.RemoteBeanRef;

@Component
public class KeyButtonSensorEventService extends AbstractRpcService<KeyButtonSensorEventRequest, VoidResponse> {

	private static final Logger logger = LoggerFactory.getLogger(KeyButtonSensorEventService.class);

	private ApplicationContext applicationContext;

	private volatile Map<RemoteBeanRef, List<KeyButtonSensorRemote>> remoteBeanRef2KeyButtonSensorRemote;

	public ApplicationContext getApplicationContext() {
		return applicationContext;
	}
	@Autowired
	public void setApplicationContext(ApplicationContext applicationContext) {
		logger.info("setApplicationContext: {}", applicationContext);
		this.applicationContext = applicationContext;
	}

	@Override
	public VoidResponse process(final KeyButtonSensorEventRequest request) throws Exception {
		final RemoteBeanRef remoteBeanRef = new RemoteBeanRef();
		remoteBeanRef.setHostId(assertNotNull(request.getClientHostId(), "request.clientHostId"));
		remoteBeanRef.setBeanId(assertNotNull(request.getChannelId(), "request.channelId"));

		final List<KeyButtonSensorRemote> list = getRemoteBeanRef2KeyButtonSensorRemote().get(remoteBeanRef);
		if (list == null || list.isEmpty())
			return null;

		EventQueue.invokeLater(() -> {
			for (KeyButtonSensorRemote keyButtonSensorRemote : list) {
				keyButtonSensorRemote.setDown(request.isDown());
			}
		});
		return null;
	}

	protected Map<RemoteBeanRef, List<KeyButtonSensorRemote>> getRemoteBeanRef2KeyButtonSensorRemote() {
		Map<RemoteBeanRef, List<KeyButtonSensorRemote>> map = this.remoteBeanRef2KeyButtonSensorRemote;
		if (map == null) {
			map = new HashMap<>();
			Map<String, KeyButtonSensorRemote> beansOfType = applicationContext.getBeansOfType(KeyButtonSensorRemote.class);
			for (KeyButtonSensorRemote keyButtonSensorRemote : beansOfType.values()) {
				List<KeyButtonSensorRemote> list = map.get(keyButtonSensorRemote.getRemoteBeanRef());
				if (list == null) {
					list = new ArrayList<>(1);
					map.put(keyButtonSensorRemote.getRemoteBeanRef(), list);
				}
				list.add(keyButtonSensorRemote);
			}
			this.remoteBeanRef2KeyButtonSensorRemote = map;
		}
		return map;
	}
}
