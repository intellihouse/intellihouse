package house.intelli.raspi.rpc.dimmer;

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
import house.intelli.core.rpc.dimmer.DimmerActorEventRequest;
import house.intelli.raspi.DimmerActor;

@Component
public class DimmerActorEventNotifier {

	private static final Logger logger = LoggerFactory.getLogger(DimmerActorEventNotifier.class);

	private ApplicationContext applicationContext;

	private List<DimmerActor> dimmerActors = Collections.emptyList();

	private RpcContext rpcContext;

	private final ExecutorService executorService = Executors.newCachedThreadPool();

	private final IdentityHashMap<DimmerActor, String> dimmerActor2BeanId = new IdentityHashMap<>();

	private final PropertyChangeListener dimmerValuePropertyChangeListener = new PropertyChangeListener() {
		@Override
		public void propertyChange(PropertyChangeEvent event) {
			assertEventThread();
			if (dimmerActor2BeanId.isEmpty()) {
				Map<String, DimmerActor> beanId2DimmerActor = applicationContext.getBeansOfType(DimmerActor.class);
				for (Map.Entry<String, DimmerActor> me : beanId2DimmerActor.entrySet())
					dimmerActor2BeanId.put(me.getValue(), me.getKey());
			}
			final DimmerActor dimmerActor = (DimmerActor) event.getSource();
			final int dimmerValue = dimmerActor.getDimmerValue();
			final String beanId = dimmerActor2BeanId.get(dimmerActor);
			if (beanId == null) {
				logger.error("dimmerValuePropertyChangeListener.propertyChange: beanId not found for " + dimmerActor);
				return;
			}
			logger.debug("dimmerValuePropertyChangeListener.propertyChange: beanId={}, dimmerValue={}", beanId, dimmerValue);

			executorService.submit(new Runnable() {
				@Override
				public void run() {
					try {
						invokeDimmerActorEventRequest(beanId, dimmerValue);
					} catch (Exception x) {
						logger.error("dimmerValuePropertyChangeListener.propertyChange.run: " + x + ' ', x);
					}
				}
			});
		}
	};

	protected void invokeDimmerActorEventRequest(final String beanId, final int dimmerValue) throws RpcException {
		logger.debug("invokeDimmerActorEventRequest: beanId={}, dimmerValue={}", beanId, dimmerValue);
		DimmerActorEventRequest request = new DimmerActorEventRequest();
		request.setServerHostId(HostId.SERVER);
		request.setChannelId(beanId);
		request.setDimmerValue(dimmerValue);
		try (RpcClient rpcClient = rpcContext.createRpcClient()) {
			rpcClient.invoke(request);
		}
	}

	public DimmerActorEventNotifier() {
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

	public List<DimmerActor> getDimmerActors() {
		return dimmerActors;
	}

	@Autowired(required = false)
	public void setDimmerActors(List<DimmerActor> dimmerActors) {
		assertEventThread();
		logger.debug("setDimmerActors: dimmerActors={}", dimmerActors);

		List<DimmerActor> oldDimmerActors = this.dimmerActors;
		for (DimmerActor dimmerActor : oldDimmerActors)
			dimmerActor.removePropertyChangeListener(DimmerActor.PropertyEnum.dimmerValue, dimmerValuePropertyChangeListener);

		if (dimmerActors == null)
			this.dimmerActors = Collections.emptyList();
		else {
			for (DimmerActor dimmerActor : dimmerActors)
				dimmerActor.addPropertyChangeListener(DimmerActor.PropertyEnum.dimmerValue, dimmerValuePropertyChangeListener);

			this.dimmerActors = Collections.unmodifiableList(new ArrayList<>(dimmerActors));
		}
	}
}
