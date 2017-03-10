package house.intelli.core.rpc;

import static house.intelli.core.util.AssertUtil.*;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PollInverseRequestsThread extends Thread {

	private static final long SLEEP_ON_ERROR_MIN = 250L;
	private static final long SLEEP_ON_ERROR_MAX = 10000L;

	private static final Logger logger = LoggerFactory.getLogger(PollInverseRequestsThread.class);

	private final ExecutorService executorService = Executors.newCachedThreadPool(); // TODO use fixed-size and make size configurable?!

	private final RpcContext rpcContext;

	private volatile boolean interrupted;

	public PollInverseRequestsThread(final RpcContext rpcContext) {
		this.rpcContext = assertNotNull(rpcContext, "rpcContext");
		this.setName(getClass().getSimpleName() + '-' + rpcContext.getLocalHostId());
	}

	public RpcContext getRpcContext() {
		return rpcContext;
	}

	@Override
	public void run() {
		long sleepOnError = SLEEP_ON_ERROR_MIN;
		while (! isInterrupted()) {
			if (rpcContext.getRpcClientTransportProvider() != null)
				break;

			logger.info("run: rpcContext.rpcClientTransportProvider not yet assigned! Going to retry later.");
			try {
				sleep(sleepOnError); // prevent flooding the log
			} catch (InterruptedException e) {
				// ignore
			}
			sleepOnError = Math.min(sleepOnError * 2, SLEEP_ON_ERROR_MAX);
		}

		sleepOnError = SLEEP_ON_ERROR_MIN;

		while (! isInterrupted()) {
			try {
				try (RpcClient rpcClient = rpcContext.createRpcClient()) {
					PollInverseRequestsRequest request = new PollInverseRequestsRequest();
					request.setServerHostId(HostId.SERVER);
					PollInverseRequestsResponse response = rpcClient.invoke(request);

					// We pass these requests to an Executor. This thread must not block!
					for (Request inverseRequest : response.getInverseRequests())
						putInverseRequest(inverseRequest);
				}
				sleepOnError = SLEEP_ON_ERROR_MIN;
			} catch (Throwable x) {
				logger.error("run:" + x, x);
				try {
					sleep(sleepOnError); // prevent too many errors, hammering onto the server and spamming the log
				} catch (InterruptedException e) {
					// ignore
				}
				sleepOnError = Math.min(sleepOnError * 2, SLEEP_ON_ERROR_MAX);
			}
		}
	}

	protected void putInverseRequest(final Request inverseRequest) {
		assertNotNull(inverseRequest, "inverseRequest");

		executorService.submit(new Runnable() {
			@Override
			public void run() {
				try {
					Response inverseResponse;
					if (rpcContext.isServerLocal(inverseRequest))
						inverseResponse = processLocally(inverseRequest);
					else
						throw new UnsupportedOperationException("Inverse-channel does not support forwarding!");

					putInverseResponse(inverseResponse);
				} catch (Throwable x) {
					Error error = RemoteExceptionUtil.createError(x);
					ErrorResponse errorResponse = new ErrorResponse(error);
					errorResponse.copyRequestCoordinates(inverseRequest);
					putInverseResponse(errorResponse);
				}
			}
		});
	}

	protected Response processLocally(final Request inverseRequest) throws Exception {
		assertNotNull(inverseRequest, "inverseRequest");
		return rpcContext.getRpcServiceExecutor().processLocally(inverseRequest);
	}

	protected void putInverseResponse(final Response inverseResponse) {
		assertNotNull(inverseResponse, "inverseResponse");
		try (RpcClient rpcClient = rpcContext.createRpcClient()) {
			PutInverseResponseRequest request = new PutInverseResponseRequest();
			request.setServerHostId(HostId.SERVER);
			request.setInverseResponse(inverseResponse);
			rpcClient.invoke(request);
		} catch (Exception x) {
			logger.error("putInverseResponse: " + x, x);
			// TODO handle this otherwise, too? we should retry later!!!
		}
	}

	@Override
	public void interrupt() {
		interrupted = true;
		super.interrupt();
	}

	@Override
	public boolean isInterrupted() {
		return interrupted;
	}
}
