package house.intelli.core.jaxb;

import java.util.ServiceLoader;

import javax.xml.bind.JAXBContext;

/**
 * {@code IntelliHouseJaxbContextProvider} implementations populate the {@link IntelliHouseJaxbContext}.
 * <p>
 * Implementation classes are registered using the {@link ServiceLoader}.
 * <p>
 * <b>Important:</b> Implementors should subclass {@link AbstractIntelliHouseJaxbContextProvider} instead
 * of directly implementing this interface!
 * @author Marco หงุ่ยตระกูล-Schulze - marco at codewizards dot co
 */
public interface IntelliHouseJaxbContextProvider {
	/**
	 * Gets the classes to be bound in the {@link IntelliHouseJaxbContext}.
	 * <p>
	 * The classes returned here are combined with all other providers' results and then passed
	 * to {@link JAXBContext#newInstance(Class[])}.
	 * @return the classes to be bound in the {@link IntelliHouseJaxbContext}. May be <code>null</code>,
	 * which is equivalent to an empty array.
	 */
	Class<?>[] getClassesToBeBound();
}
