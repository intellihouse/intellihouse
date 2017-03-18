/*
 * Copyright 2003-2004 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package house.intelli.core.observable;

import java.util.Set;

/**
 * Decorates a <code>Set</code> implementation to observe modifications.
 * <p>
 * Each modifying method call made on this <code>Set</code> is forwarded to a
 * {@link ModificationHandler}.
 * The handler manages the event, notifying listeners and optionally vetoing changes.
 * The default handler is
 * {@link org.apache.commons.events.observable.standard.StandardModificationHandler}.
 * See this class for details of configuration available.
 *
 * @since Commons Events 1.0
 * @version $Revision: 155443 $ $Date: 2005-02-26 20:19:51 +0700 (Sa, 26 Feb 2005) $
 *
 * @author Stephen Colebourne
 */
public class ObservableSet<E> extends ObservableCollection<E> implements Set<E> {
	private static final long serialVersionUID = 1L;

	// Factories
    //-----------------------------------------------------------------------
    /**
     * Factory method to create an observable set.
     * <p>
     * A {@link org.apache.commons.events.observable.standard.StandardModificationHandler} will be created.
     * This can be accessed by {@link #getHandler()} to add listeners.
     *
     * @param set  the set to decorate, must not be null
     * @return the observed Set
     * @throws IllegalArgumentException if the collection is null
     */
    public static <E> ObservableSet<E> decorate(final Set<E> set) {
        return new ObservableSet<E>(set, null);
    }

    /**
     * Factory method to create an observable set using a listener or a handler.
     * <p>
     * A lot of functionality is available through this method.
     * If you don't need the extra functionality, simply implement the
     * {@link org.apache.commons.events.observable.standard.StandardModificationListener}
     * interface and pass it in as the second parameter.
     * <p>
     * Internally, an <code>ObservableSet</code> relies on a {@link ModificationHandler}.
     * The handler receives all the events and processes them, typically by
     * calling listeners. Different handler implementations can be plugged in
     * to provide a flexible event system.
     * <p>
     * The handler implementation is determined by the listener parameter via
     * the registered factories. The listener may be a manually configured
     * <code>ModificationHandler</code> instance.
     * <p>
     * The listener is defined as an Object for maximum flexibility.
     * It does not have to be a listener in the classic JavaBean sense.
     * It is entirely up to the factory and handler as to how the parameter
     * is interpretted. An IllegalArgumentException is thrown if no suitable
     * handler can be found for this listener.
     * <p>
     * A <code>null</code> listener will create a
     * {@link org.apache.commons.events.observable.standard.StandardModificationHandler}.
     *
     * @param set  the set to decorate, must not be null
     * @param listener  set listener, may be null
     * @return the observed set
     * @throws IllegalArgumentException if the set is null
     * @throws IllegalArgumentException if there is no valid handler for the listener
     */
    public static <E> ObservableSet<E> decorate(
            final Set<E> set,
            final Object listener) {

        if (set == null) {
            throw new IllegalArgumentException("Set must not be null");
        }
        return new ObservableSet<E>(set, listener);
    }

    // Constructors
    //-----------------------------------------------------------------------
    /**
     * Constructor that wraps (not copies) and takes a handler.
     * <p>
     * The handler implementation is determined by the listener parameter via
     * the registered factories. The listener may be a manually configured
     * <code>ModificationHandler</code> instance.
     *
     * @param set  the set to decorate, must not be null
     * @param listener  the listener, may be null
     * @throws IllegalArgumentException if the set is null
     */
    protected ObservableSet(
            final Set<E> set,
            final Object listener) {
        super(set, listener);
    }

}
