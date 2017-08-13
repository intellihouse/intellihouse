package house.intelli.raspi.rpc.keybutton;

import static house.intelli.core.util.AssertUtil.*;

import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import house.intelli.core.rpc.RpcClient;
import house.intelli.core.rpc.RpcContext;
import house.intelli.core.rpc.RpcException;
import house.intelli.core.rpc.keybutton.KeyButtonSensorRemotePropagationRequest;
import house.intelli.raspi.KeyButtonSensorRemote;
import house.intelli.raspi.RemoteBeanRef;

/**
 * Registers listeners for all {@link KeyButtonSensorRemote} objects in the {@link KeyButtonSensorEventNotifier}
 * holding the real physical key-buttons.
 * @author mn
 */
@Component
public class KeyButtonSensorRemotePropagator {

	private static final Logger logger = LoggerFactory.getLogger(KeyButtonSensorRemotePropagator.class);

	private static final long PROPAGATION_PERIOD = 60L * 1000L;

	private List<KeyButtonSensorRemote> keyButtonSensorRemotes = Collections.emptyList();

	private final Timer timer = new Timer("KeyButtonSensorRemotePropagator.timer", true);

	private RpcContext rpcContext;

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

	public KeyButtonSensorRemotePropagator() {
		timer.schedule(timerTask, PROPAGATION_PERIOD, PROPAGATION_PERIOD);
	}

	public RpcContext getRpcContext() {
		return rpcContext;
	}

	@Autowired
	public void setRpcContext(RpcContext rpcContext) {
		this.rpcContext = rpcContext;
	}

	public List<KeyButtonSensorRemote> getKeyButtonSensorRemotes() {
		return keyButtonSensorRemotes;
	}

	@Autowired(required = false)
	public void setKeyButtonSensorRemotes(List<KeyButtonSensorRemote> keyButtonSensorRemotes) {
		assertEventThread();
		logger.debug("setKeyButtonSensors: keyButtonSensorRemotes={}", keyButtonSensorRemotes);

		if (keyButtonSensorRemotes == null)
			this.keyButtonSensorRemotes = Collections.emptyList();
		else
			this.keyButtonSensorRemotes = keyButtonSensorRemotes;
	}

	protected void propagate() {
		for (KeyButtonSensorRemote keyButtonSensorRemote : getKeyButtonSensorRemotes()) {
			try {
				propagate(keyButtonSensorRemote);
			} catch (Exception x) {
				logger.error("propagate: " + x.toString(), x);
			}
		}
	}

	protected void propagate(KeyButtonSensorRemote keyButtonSensorRemote) throws RpcException {
		assertNotNull(keyButtonSensorRemote, "keyButtonSensorRemote");

		RemoteBeanRef remoteBeanRef = keyButtonSensorRemote.getRemoteBeanRef();
		assertNotNull(remoteBeanRef, "keyButtonSensorRemote.remoteBeanRef");

		logger.debug("propagate: {}", remoteBeanRef);

		KeyButtonSensorRemotePropagationRequest request = new KeyButtonSensorRemotePropagationRequest();
		request.setServerHostId(remoteBeanRef.getHostId());
		request.setChannelId(remoteBeanRef.getBeanId());
		try (RpcClient rpcClient = rpcContext.createRpcClient()) {
			rpcClient.invoke(request);
		}
	}
}
