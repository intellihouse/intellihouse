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

import java.util.Collection;
import java.util.Iterator;

import org.apache.commons.collections4.collection.AbstractCollectionDecorator;
import org.apache.commons.collections4.iterators.AbstractIteratorDecorator;

import house.intelli.core.observable.standard.StandardModificationHandler;

/**
 * Decorates a <code>Collection</code> implementation to observe modifications.
 * <p>
 * Each modifying method call made on this <code>Collection</code> is forwarded to a
 * {@link ModificationHandler}.
 * The handler manages the event, notifying listeners and optionally vetoing changes.
 * The default handler is {@link StandardModificationHandler}.
 * See this class for details of configuration available.
 *
 * @since Commons Events 1.0
 * @version $Revision: 155443 $ $Date: 2005-02-26 20:19:51 +0700 (Sa, 26 Feb 2005) $
 *
 * @author Stephen Colebourne
 */
public class ObservableCollection<E> extends AbstractCollectionDecorator<E> {
	private static final long serialVersionUID = 1L;

	/** The list of registered factories, checked in reverse order */
    private static ModificationHandlerFactory[] factories = new ModificationHandlerFactory[] {
        ModificationHandler.FACTORY,
        StandardModificationHandler.FACTORY
    };

    /** The handler to delegate event handling to */
    protected final ModificationHandler<E> handler;

    // ObservableCollection factories
    //-----------------------------------------------------------------------
    /**
     * Factory method to create an observable collection.
     * <p>
     * A {@link StandardModificationHandler} will be created.
     * This can be accessed by {@link #getHandler()} to add listeners.
     *
     * @param coll  the collection to decorate, must not be null
     * @return the observed collection
     * @throws IllegalArgumentException if the collection is null
     */
    public static <E> ObservableCollection<E> decorate(final Collection<E> coll) {
        return new ObservableCollection<E>(coll, null);
    }

    /**
     * Factory method to create an observable collection using a listener or a handler.
     * <p>
     * A lot of functionality is available through this method.
     * If you don't need the extra functionality, simply implement the
     * {@link org.apache.commons.events.observable.standard.StandardModificationListener}
     * interface and pass it in as the second parameter.
     * <p>
     * Internally, an <code>ObservableCollection</code> relies on a {@link ModificationHandler}.
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
     * A <code>null</code> listener will create a {@link StandardModificationHandler}.
     *
     * @param coll  the collection to decorate, must not be null
     * @param listener  collection listener, may be null
     * @return the observed collection
     * @throws IllegalArgumentException if the collection is null
     * @throws IllegalArgumentException if there is no valid handler for the listener
     */
    public static <E> ObservableCollection<E> decorate(
            final Collection<E> coll,
            final Object listener) {

        if (coll == null) {
            throw new IllegalArgumentException("Collection must not be null");
        }
        return new ObservableCollection<E>(coll, listener);
    }

    // Register for ModificationHandlerFactory
    //-----------------------------------------------------------------------
    /**
     * Registers a handler factory to be used for looking up a listener to
     * a handler.
     * <p>
     * This method is used to add your own event handler to the supplied ones.
     * Registering the factory will enable the {@link #decorate(Collection, Object)}
     * method to create your handler.
     * <p>
     * Each handler added becomes the first in the lookup chain. Thus it is
     * possible to override the default setup.
     * Obviously this should be done with care in a shared web environment!
     * <p>
     * This method is not guaranteed to be threadsafe.
     * It should only be called during initialization.
     * Problems will occur if two threads call this method at the same time.
     *
     * @param factory  the factory to add, may be null
     */
    public static void registerFactory(final ModificationHandlerFactory factory) {
        if (factory != null) {
            // add at end, as checked in reverse order
            ModificationHandlerFactory[] array = new ModificationHandlerFactory[factories.length + 1];
            System.arraycopy(factories, 0, array, 0, factories.length);
            array[factories.length] = factory;
            factories = array;  // atomic operation
        }
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
     * @param coll  the collection to decorate, must not be null
     * @param listener  the observing handler, may be null
     * @throws IllegalArgumentException if the collection is null
     */
    protected ObservableCollection(
            final Collection<E> coll,
            final Object listener) {
        super(coll);
        this.handler = createHandler(coll, listener);
        this.handler.init(this, coll);
    }

    /**
     * Constructor used by subclass views, such as subList.
     *
     * @param handler  the observing handler, may be null
     * @param coll  the collection to decorate, must not be null
     * @throws IllegalArgumentException if the collection is null
     */
    protected ObservableCollection(
            final ModificationHandler handler,
            final Collection<E> coll) {
        super(coll);
        this.handler = handler;
    }

    /**
     * Creates a handler subclass based on the specified listener.
     * <p>
     * The method is defined in terms of an Object to allow for unusual
     * listeners, such as a Swing model object.
     *
     * @param listener  a listener object to create a handler for
     * @return an instantiated handler with the listener attached
     * @throws IllegalArgumentException if no suitable handler
     */
    protected ModificationHandler<E> createHandler(final Collection<E> coll, final Object listener) {
        if (listener == null) {
            return new StandardModificationHandler<E>();
        }
        ModificationHandlerFactory[] array = factories;  // atomic operation
        for (int i = array.length - 1; i >= 0 ; i--) {
            ModificationHandler handler = array[i].createHandler(coll, listener);
            if (handler != null) {
                return handler;
            }
        }
        throw new IllegalArgumentException("Unrecognised listener type: " +
            (listener == null ? "null" : listener.getClass().getName()));
    }

    // Handler access
    //-----------------------------------------------------------------------
    /**
     * Gets the handler that is observing this collection.
     *
     * @return the observing handler, never null
     */
    public ModificationHandler<E> getHandler() {
        return handler;
    }

    // Collection
    //-----------------------------------------------------------------------
    @Override
	public boolean add(E object) {
        boolean result = false;
        if (handler.preAdd(object)) {
            result = decorated().add(object);
            handler.postAdd(object, result);
        }
        return result;
    }

    @Override
	public boolean addAll(Collection<? extends E> coll) {
        boolean result = false;
        if (handler.preAddAll(coll)) {
            result = decorated().addAll(coll);
            handler.postAddAll(coll, result);
        }
        return result;
    }

    @Override
	public void clear() {
        if (handler.preClear()) {
        	decorated().clear();
            handler.postClear();
        }
    }

    @Override
	public Iterator<E> iterator() {
        return new ObservableIterator<E>(decorated().iterator());
    }

    @Override
	public boolean remove(Object object) {
        boolean result = false;
        if (handler.preRemove(object)) {
            result = decorated().remove(object);
            handler.postRemove(object, result);
        }
        return result;
    }

    @Override
	public boolean removeAll(Collection<?> coll) {
        boolean result = false;
        if (handler.preRemoveAll(coll)) {
            result = decorated().removeAll(coll);
            handler.postRemoveAll(coll, result);
        }
        return result;
    }

    @Override
	public boolean retainAll(Collection<?> coll) {
        boolean result = false;
        if (handler.preRetainAll(coll)) {
            result = decorated().retainAll(coll);
            handler.postRetainAll(coll, result);
        }
        return result;
    }

    // Iterator
    //-----------------------------------------------------------------------
    /**
     * Inner class Iterator for the ObservableCollection.
     */
    protected class ObservableIterator<F> extends AbstractIteratorDecorator<F> {

        protected int lastIndex = -1;
        protected F last;

        protected ObservableIterator(Iterator<F> iterator) {
            super(iterator);
        }

        @Override
		public F next() {
            last = super.next();
            lastIndex++;
            return last;
        }

        @Override
		public void remove() {
            if (handler.preRemoveIterated(lastIndex, last)) {
                getIterator().remove();
                handler.postRemoveIterated(lastIndex, last);
                lastIndex--;
            }
        }
    }

}
