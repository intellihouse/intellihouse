package house.intelli.core.jaxb;

import java.util.HashSet;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import house.intelli.core.service.ServiceRegistry;

public class IntelliHouseJaxbContext {

	private static final Logger logger = LoggerFactory.getLogger(IntelliHouseJaxbContext.class);

	private static volatile JAXBContext jaxbContext;

	public static JAXBContext getJaxbContext() {
		JAXBContext result = jaxbContext;
		if (result == null) {
			final Set<Class<?>> collectedClassesToBeBound = new HashSet<Class<?>>();

			final ServiceRegistry<IntelliHouseJaxbContextProvider> serviceRegistry = ServiceRegistry.getInstance(IntelliHouseJaxbContextProvider.class);

			serviceRegistry.addListener(event -> reset());

			for (final IntelliHouseJaxbContextProvider provider : serviceRegistry.getServices()) {
				final Class<?>[] classesToBeBound = provider.getClassesToBeBound();
				logger.info("getJaxbContext: provider.class={}, classesToBeBound={}", provider.getClass().getName(), classesToBeBound);
				if (classesToBeBound != null) {
					for (final Class<?> clazz : classesToBeBound)
						collectedClassesToBeBound.add(clazz);
				}
			}
			try {
				final Class<?>[] ca = collectedClassesToBeBound.toArray(new Class[collectedClassesToBeBound.size()]);
				jaxbContext = result = JAXBContext.newInstance(ca);
			} catch (JAXBException x) {
				throw new RuntimeException(x);
			}
		}
		return result;
	}

	public static void reset() {
		jaxbContext = null;
	}
}
