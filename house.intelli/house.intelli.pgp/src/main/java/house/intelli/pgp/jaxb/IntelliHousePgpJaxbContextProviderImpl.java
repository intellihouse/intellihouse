package house.intelli.pgp.jaxb;

import house.intelli.core.jaxb.AbstractIntelliHouseJaxbContextProvider;
import house.intelli.pgp.rpc.PgpRequest;
import house.intelli.pgp.rpc.PgpResponse;

public class IntelliHousePgpJaxbContextProviderImpl extends AbstractIntelliHouseJaxbContextProvider {

	@Override
	public Class<?>[] getClassesToBeBound() {
		return new Class<?>[] {
			PgpRequest.class,
			PgpResponse.class
		};
	}

}
