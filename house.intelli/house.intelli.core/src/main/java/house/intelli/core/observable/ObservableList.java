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
import java.util.List;
import java.util.ListIterator;

import org.apache.commons.collections4.iterators.AbstractListIteratorDecorator;

/**
 * Decorates a <code>List</code> implementation to observe modifications.
 * <p>
 * Each modifying method call made on this <code>List</code> is forwarded to a
 * {@link ModificationHandler}.
 * The handler manages the event, notifying listeners and optionally vetoing changes.
 * The default handler is
 * {@link org.apache.commons.events.observable.standard.StandardModificationHandler StandardModificationHandler}.
 * See this class for details of configuration available.
 * <p>
 * All indices on events returned by <code>subList</code> are relative to the
 * base <code>List</code>.
 *
 * @since Commons Events 1.0
 * @version $Revision: 155443 $ $Date: 2005-02-26 20:19:51 +0700 (Sa, 26 Feb 2005) $
 *
 * @author Stephen Colebourne
 */
public class ObservableList<E> extends ObservableCollection<E> implements List<E> {
	private static final long serialVersionUID = 1L;

	// Factories
    //-----------------------------------------------------------------------
    /**
     * Factory method to create an observable list.
     * <p>
     * A {@link org.apache.commons.events.observable.standard.StandardModificationHandler} will be created.
     * This can be accessed by {@link #getHandler()} to add listeners.
     *
     * @param list  the list to decorate, must not be null
     * @return the observed List
     * @throws IllegalArgumentException if the list is null
     */
    public static <E> ObservableList<E> decorate(final List<E> list) {
        return new ObservableList<E>(list, null);
    }

    /**
     * Factory method to create an observable list using a listener or a handler.
     * <p>
     * A lot of functionality is available through this method.
     * If you don't need the extra functionality, simply implement the
     * {@link org.apache.commons.events.observable.standard.StandardModificationListener}
     * interface and pass it in as the second parameter.
     * <p>
     * Internally, an <code>ObservableList</code> relies on a {@link ModificationHandler}.
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
     * @param list  the list to decorate, must not be null
     * @param listener  list listener, may be null
     * @return the observed list
     * @throws IllegalArgumentException if the list is null
     * @throws IllegalArgumentException if there is no valid handler for the listener
     */
    public static <E> ObservableList<E> decorate(
            final List<E> list,
            final Object listener) {

        if (list == null) {
            throw new IllegalArgumentException("List must not be null");
        }
        return new ObservableList<E>(list, listener);
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
     * @param list  the list to decorate, must not be null
     * @param listener  the listener, may be null
     * @throws IllegalArgumentException if the list is null
     */
    protected ObservableList(
            final List<E> list,
            final Object listener) {
        super(list, listener);
    }

    /**
     * Constructor used by subclass views, such as subList.
     *
     * @param handler  the handler to use, must not be null
     * @param list  the subList to decorate, must not be null
     * @throws IllegalArgumentException if the list is null
     */
    protected ObservableList(
            final ModificationHandler<E> handler,
            final List<E> list) {
        super(handler, list);
    }

    /**
     * Typecast the collection to a List.
     *
     * @return the wrapped collection as a List
     */
    private List<E> getList() {
        return (List<E>) decorated();
    }

    // List API
    //-----------------------------------------------------------------------
    @Override
	public E get(int index) {
        return getList().get(index);
    }

    @Override
	public int indexOf(Object object) {
        return getList().indexOf(object);
    }

    @Override
	public int lastIndexOf(Object object) {
        return getList().lastIndexOf(object);
    }

    //-----------------------------------------------------------------------
    @Override
	public void add(int index, E object) {
        if (handler.preAddIndexed(index, object)) {
            getList().add(index, object);
            handler.postAddIndexed(index, object);
        }
    }

    @Override
	public boolean addAll(int index, Collection<? extends E> coll) {
        boolean result = false;
        if (handler.preAddAllIndexed(index, coll)) {
            result = getList().addAll(index, coll);
            handler.postAddAllIndexed(index, coll, result);
        }
        return result;
    }

    @Override
	public E remove(int index) {
        E result = null;
        if (handler.preRemoveIndexed(index)) {
            result = getList().remove(index);
            handler.postRemoveIndexed(index, result);
        }
        return result;
    }

    @Override
	public E set(int index, E object) {
        E result = null;
        if (handler.preSetIndexed(index, object)) {
            result = getList().set(index, object);
            handler.postSetIndexed(index, object, result);
        }
        return result;
    }

    @Override
	public ListIterator<E> listIterator() {
        return new ObservableListIterator<E>(getList().listIterator());
    }

    @Override
	public ListIterator<E> listIterator(int index) {
        return new ObservableListIterator<E>(getList().listIterator(index));
    }

    /**
     * Returns a subList view on the original base <code>List</code>.
     * <p>
     * Changes to the subList affect the underlying List. Change events will
     * return change indices relative to the underlying List, not the subList.
     *
     * @param fromIndex  inclusive start index of the range
     * @param toIndex  exclusive end index of the range
     * @return the subList view
     */
    @Override
	public List<E> subList(int fromIndex, int toIndex) {
        List<E> subList = getList().subList(fromIndex, toIndex);
        return new ObservableList<E>(subList, getHandler().createSubListHandler(fromIndex, toIndex));
    }

    // ListIterator
    //-----------------------------------------------------------------------
    /**
     * Inner class ListIterator for the ObservableList.
     */
    protected class ObservableListIterator<F> extends AbstractListIteratorDecorator<F> {

        protected F last;

        protected ObservableListIterator(ListIterator<F> iterator) {
            super(iterator);
        }

        @Override
		public F next() {
            last = super.next();
            return last;
        }

        @Override
		public F previous() {
            last = getListIterator().previous();
            return last;
        }

        @Override
		public void remove() {
            int index = getListIterator().previousIndex();
            if (handler.preRemoveIterated(index, last)) {
            	getListIterator().remove();
                handler.postRemoveIterated(index, last);
            }
        }

        @Override
		public void add(F object) {
            int index = getListIterator().nextIndex();
            if (handler.preAddIterated(index, object)) {
            	getListIterator().add(object);
                handler.postAddIterated(index, object);
            }
        }

        @Override
		public void set(F object) {
            int index = getListIterator().previousIndex();
            if (handler.preSetIterated(index, object, last)) {
            	getListIterator().set(object);
                handler.postSetIterated(index, object, last);
            }
        }
    }

}
