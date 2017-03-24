package house.intelli.pgp.jaxb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import house.intelli.core.jaxb.AbstractIntelliHouseJaxbContextProvider;
import house.intelli.pgp.rpc.PgpRequest;
import house.intelli.pgp.rpc.PgpResponse;
import house.intelli.pgp.rpc.SessionRequest;

public class IntelliHousePgpJaxbContextProviderImpl extends AbstractIntelliHouseJaxbContextProvider {
	private static final Logger logger = LoggerFactory.getLogger(IntelliHousePgpJaxbContextProviderImpl.class);

	public IntelliHousePgpJaxbContextProviderImpl() {
		logger.info("<init>");
	}

	@Override
	public Class<?>[] getClassesToBeBound() {
		return new Class<?>[] {
			PgpRequest.class,
			PgpResponse.class,
			SessionRequest.class
		};
	}

}
