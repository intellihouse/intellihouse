package house.intelli.raspi.rpc.relay;

import static house.intelli.core.util.AssertUtil.*;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
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
import house.intelli.core.rpc.relay.RelayActorEventRequest;
import house.intelli.raspi.RelayActor;

@Component
public class RelayActorEventNotifier {

	private static final Logger logger = LoggerFactory.getLogger(RelayActorEventNotifier.class);

	private ApplicationContext applicationContext;

	private List<RelayActor> relayActors = Collections.emptyList();

	private RpcContext rpcContext;

	private final ExecutorService executorService = Executors.newCachedThreadPool();

	private final IdentityHashMap<RelayActor, String> relayActor2BeanId = new IdentityHashMap<>();

	private final PropertyChangeListener relayValuePropertyChangeListener = new PropertyChangeListener() {
		@Override
		public void propertyChange(PropertyChangeEvent event) {
			assertEventThread();
			if (relayActor2BeanId.isEmpty()) {
				Map<String, RelayActor> beanId2RelayActor = applicationContext.getBeansOfType(RelayActor.class);
				for (Map.Entry<String, RelayActor> me : beanId2RelayActor.entrySet())
					relayActor2BeanId.put(me.getValue(), me.getKey());
			}
			final RelayActor relayActor = (RelayActor) event.getSource();
			final boolean energized = relayActor.isEnergized();
			final String beanId = relayActor2BeanId.get(relayActor);
			if (beanId == null) {
				logger.error("relayValuePropertyChangeListener.propertyChange: beanId not found for " + relayActor);
				return;
			}
			logger.debug("relayValuePropertyChangeListener.propertyChange: beanId={}, energized={}", beanId, energized);

			executorService.submit(new Runnable() {
				@Override
				public void run() {
					try {
						invokeRelayActorEventRequest(beanId, energized);
					} catch (Exception x) {
						logger.error("relayValuePropertyChangeListener.propertyChange.run: " + x + ' ', x);
					}
				}
			});
		}
	};

	protected void invokeRelayActorEventRequest(final String beanId, final boolean energized) throws RpcException {
		logger.debug("invokeRelayActorEventRequest: beanId={}, energized={}", beanId, energized);
		RelayActorEventRequest request = new RelayActorEventRequest();
		request.setServerHostId(HostId.SERVER);
		request.setChannelId(beanId);
		request.setEnergized(energized);
		try (RpcClient rpcClient = rpcContext.createRpcClient()) {
			rpcClient.invoke(request);
		}
	}

	public RelayActorEventNotifier() {
		logger.info("<init>");
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

	public List<RelayActor> getRelayActors() {
		return relayActors;
	}

	@Autowired
	public void setRelayActors(List<RelayActor> relayActors) {
		assertEventThread();
		logger.debug("setRelayActors: relayActors={}", relayActors);

		List<RelayActor> oldRelayActors = this.relayActors;
		for (RelayActor relayActor : oldRelayActors)
			relayActor.removePropertyChangeListener(RelayActor.PropertyEnum.energized, relayValuePropertyChangeListener);

		if (relayActors == null)
			this.relayActors = Collections.emptyList();
		else {
			for (RelayActor relayActor : relayActors)
				relayActor.addPropertyChangeListener(RelayActor.PropertyEnum.energized, relayValuePropertyChangeListener);

			this.relayActors = Collections.unmodifiableList(new ArrayList<>(relayActors));
		}
	}
}
