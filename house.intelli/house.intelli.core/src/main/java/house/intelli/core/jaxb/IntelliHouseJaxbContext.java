package house.intelli.core.jaxb;

import java.util.HashSet;
import java.util.Iterator;
import java.util.ServiceLoader;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

public class IntelliHouseJaxbContext {

	private static class JaxbContextHolder {
		private static final JAXBContext jaxbContext;
		static {
			final Set<Class<?>> collectedClassesToBeBound = new HashSet<Class<?>>();
			final ServiceLoader<IntelliHouseJaxbContextProvider> serviceLoader = ServiceLoader.load(IntelliHouseJaxbContextProvider.class);
			for (final Iterator<IntelliHouseJaxbContextProvider> it = serviceLoader.iterator(); it.hasNext(); ) {
				final IntelliHouseJaxbContextProvider provider = it.next();
				final Class<?>[] classesToBeBound = provider.getClassesToBeBound();
				if (classesToBeBound != null) {
					for (final Class<?> clazz : classesToBeBound)
						collectedClassesToBeBound.add(clazz);
				}
			}
			try {
				final Class<?>[] ca = collectedClassesToBeBound.toArray(new Class[collectedClassesToBeBound.size()]);
				jaxbContext = JAXBContext.newInstance(ca);
			} catch (JAXBException x) {
				throw new RuntimeException(x);
			}
		}
	}

	public static JAXBContext getJaxbContext() {
		return JaxbContextHolder.jaxbContext;
	}
}
