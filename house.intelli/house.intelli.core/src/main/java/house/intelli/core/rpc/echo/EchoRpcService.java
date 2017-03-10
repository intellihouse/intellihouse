package house.intelli.core.rpc.echo;

import static house.intelli.core.util.AssertUtil.*;
import static house.intelli.core.util.StringUtil.*;

import house.intelli.core.rpc.AbstractRpcService;
import house.intelli.core.util.ReflectionUtil;

public class EchoRpcService extends AbstractRpcService<EchoRequest, EchoResponse> {

	@Override
	public EchoResponse process(EchoRequest request) throws Exception {
		assertNotNull(request, "request");

		if (request.getSleep() > 0)
			Thread.sleep(request.getSleep());

		String throwExceptionClassName = request.getThrowExceptionClassName();
		if (! isEmpty(throwExceptionClassName)) {
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
		return response;
	}
}
