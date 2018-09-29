package house.intelli.core.rpc.echo;

import static java.util.Objects.*;
import static house.intelli.core.util.StringUtil.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import house.intelli.core.rpc.AbstractRpcService;
import house.intelli.core.util.ReflectionUtil;

public class EchoRpcService extends AbstractRpcService<EchoRequest, EchoResponse> {

	private static final Logger logger = LoggerFactory.getLogger(EchoRpcService.class);

	@Override
	public EchoResponse process(EchoRequest request) throws Exception {
		requireNonNull(request, "request");

		logger.info("process: {}" , request);

		if (request.getSleep() > 0)
			Thread.sleep(request.getSleep());

		String throwExceptionClassName = request.getThrowExceptionClassName();
		if (! isEmpty(throwExceptionClassName)) {
			logger.info("process: throwing: {}" , throwExceptionClassName);

			@SuppressWarnings("unchecked")
			Class<? extends Exception> exceptionClass = (Class<? extends Exception>) Class.forName(throwExceptionClassName);

			Exception exception;
			try {
				exception = ReflectionUtil.invokeConstructor(exceptionClass, request.getPayload());
			} catch (Exception x) {
				exception = ReflectionUtil.invokeConstructor(exceptionClass);
			}
			throw exception;
		}

		EchoResponse response = new EchoResponse();
		response.setPayload(request.getPayload());
		logger.info("process: {}" , response);
		return response;
	}
}
