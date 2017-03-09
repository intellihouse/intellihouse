package house.intelli.core.jaxb;

import house.intelli.core.rpc.echo.EchoRequest;
import house.intelli.core.rpc.echo.EchoResponse;

public class IntelliHouseJaxbContextProviderImpl extends AbstractIntelliHouseJaxbContextProvider {

	@Override
	public Class<?>[] getClassesToBeBound() {
		// TODO get Request and Response types from the RpcServices!
//		List<Class<?>> classes = new ArrayList<>();
//
//
//
//		return classes.toArray(new Class<?>[classes.size()]);
		return new Class<?>[] {
			EchoRequest.class,
			EchoResponse.class
		};
	}

}
