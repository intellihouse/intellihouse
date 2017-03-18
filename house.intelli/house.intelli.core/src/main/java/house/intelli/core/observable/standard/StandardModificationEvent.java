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
package house.intelli.core.observable.standard;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections4.Bag;

import house.intelli.core.observable.ModificationEvent;
import house.intelli.core.observable.ModificationEventType;
import house.intelli.core.observable.ModificationHandler;
import house.intelli.core.observable.ObservableCollection;

/**
 * Event class that encapsulates the event information for a
 * standard collection event. Two subclasses are provided, one for
 * pre and one for post events.
 * <p>
 * The information stored in this event is all that is available as
 * parameters or return values.
 * In addition, the <code>size</code> method is used on the collection.
 * All objects used are the real objects from the method calls, not clones.
 *
 * @since Commons Events 1.0
 * @version $Revision: 155443 $ $Date: 2005-02-26 20:19:51 +0700 (Sa, 26 Feb 2005) $
 *
 * @author Stephen Colebourne
 */
public class StandardModificationEvent extends ModificationEvent {

    /** The size before the event */
    protected final int preSize;
    /** The index of the change */
    protected final int index;
    /** The object of the change */
    protected final Object object;
    /** The number of changes */
    protected final int repeat;
    /** The result of the method call */
    protected final Object previous;
    /** The view that the event came from, null if none */
    protected final ObservableCollection view;
    /** The offset index within the main collection of the view, -1 if none */
    protected final int viewOffset;

    // Constructor
    //-----------------------------------------------------------------------
    /**
     * Constructor.
     *
     * @param obsCollection  the event source
     * @param handler  the handler
     * @param type  the event type
     * @param preSize  the size before the change
     * @param index  the index that changed
     * @param object  the value that changed
     * @param repeat  the number of repeats
     * @param previous  the previous value being removed/replaced
     * @param view  the view collection, null if event from main collection
     * @param viewOffset  the offset within the main collection of the view, -1 if unknown
     */
    public StandardModificationEvent(
        final ObservableCollection obsCollection,
        final ModificationHandler handler,
        final int type,
        final int preSize,
        final int index,
        final Object object,
        final int repeat,
        final Object previous,
        final ObservableCollection view,
        final int viewOffset) {

        super(obsCollection, handler, type);
        this.preSize = preSize;
        this.index = index;
        this.object = object;
        this.repeat = repeat;
        this.previous = previous;
        this.view = view;
        this.viewOffset = viewOffset;
    }

    // Change info
    //-----------------------------------------------------------------------
    /**
     * Gets the index of the change.
     * <p>
     * This is <code>-1</code> when not applicable. Typically only used
     * for {@link java.util.List} events.
     *
     * @return the change index
     */
    public int getChangeIndex() {
        return index;
    }

    /**
     * Gets the object that was added/removed/set.
     * <p>
     * This is <code>null</code> when not applicable, such as for clear().
     *
     * @return the changing object
     */
    public Object getChangeObject() {
        return object;
    }

    /**
     * Gets the collection of changed objects.
     * <p>
     * For clear, it is an empty list.
     * For bulk operations, it is the collection.
     * For non-bulk operations, it is a size one list.
     *
     * @return the changing collection, never null
     */
    public Collection getChangeCollection() {
        if (object == null) {
            return Collections.EMPTY_LIST;
        } else if (isType(ModificationEventType.GROUP_BULK)) {
            if (object instanceof Collection) {
                return (Collection) object;
            } else {
                throw new IllegalStateException(
                    "Bulk operations must involve a Collection, but was " + object.getClass().getName());
            }
        } else {
            return Collections.singletonList(object);
        }
    }

    /**
     * Gets the number of times the object was added/removed.
     * <p>
     * This is normally <code>1</code>, but will be used for
     * {@link org.apache.commons.collections.Bag Bag} events.
     *
     * @return the repeat
     */
    public int getChangeRepeat() {
        return repeat;
    }

    /**
     * Gets the previous value that is being replaced or removed.
     * <p>
     * This is only returned if the value definitely was previously in the
     * collection. Bulk operatons will not return this.
     *
     * @return the previous value that was removed/replaced
     */
    public Object getPrevious() {
        return previous;
    }

    // Size info
    //-----------------------------------------------------------------------
    /**
     * Gets the size before the change.
     *
     * @return the size before the change
     */
    public int getPreSize() {
        return preSize;
    }

    // View info
    //-----------------------------------------------------------------------
    /**
     * Gets the view, <code>null</code> if none.
     * <p>
     * A view is a subSet, headSet, tailSet, subList and so on.
     *
     * @return the view
     */
    public ObservableCollection getView() {
        return view;
    }

    /**
     * Checks whether the event originated from a view.
     *
     * @return true if event came from a view
     */
    public boolean isView() {
        return (view != null);
    }

    /**
     * Gets the view offset, <code>-1</code> if no view or unknown offset.
     * <p>
     * This refers to the index of the start of the view within the main collection.
     *
     * @return the view offset
     */
    public int getViewOffset() {
        return viewOffset;
    }

    // Event type
    //-----------------------------------------------------------------------
    /**
     * Checks to see if the event is an add event (add/addAll).
     *
     * @return true if of the specified type
     */
    public boolean isTypeAdd() {
        return (type & ModificationEventType.GROUP_ADD) > 0;
    }

    /**
     * Checks to see if the event is a remove event (remove/removeAll/retainAll/clear).
     *
     * @return true if of the specified type
     */
    public boolean isTypeReduce() {
        return (type & ModificationEventType.GROUP_REDUCE) > 0;
    }

    /**
     * Checks to see if the event is a change event (set).
     *
     * @return true if of the specified type
     */
    public boolean isTypeChange() {
        return (type & ModificationEventType.GROUP_CHANGE) > 0;
    }

    /**
     * Checks to see if the event is a bulk event (addAll/removeAll/retainAll/clear).
     *
     * @return true if of the specified type
     */
    public boolean isTypeBulk() {
        return (type & ModificationEventType.GROUP_BULK) > 0;
    }

    /**
     * Checks to see if the event is of the specified type.
     * <p>
     * This is any combination of constants from {@link ModificationEventType}.
     *
     * @param eventType  an event type constant
     * @return true if of the specified type
     */
    public boolean isType(final int eventType) {
        return (type & eventType) > 0;
    }

    // toString
    //-----------------------------------------------------------------------
    /**
     * Gets a debugging string version of the event.
     *
     * @return a debugging string
     */
    public String toString() {
        StringBuffer buf = new StringBuffer(64);
        buf.append("ModificationEvent[type=");
        buf.append(ModificationEventType.toString(type));
        if (index >= 0) {
            buf.append(",index=");
            buf.append(index);
        }
        if (type != ModificationEventType.CLEAR) {
            buf.append(",object=");
            if (object instanceof List) {
                buf.append("List:size:");
                buf.append(((List) object).size());
            } else if (object instanceof Set) {
                buf.append("Set:size:");
                buf.append(((Set) object).size());
            } else if (object instanceof Bag) {
                buf.append("Bag:size:");
                buf.append(((Bag) object).size());
            } else if (object instanceof Collection) {
                buf.append("Collection:size:");
                buf.append(((Collection) object).size());
            } else if (object instanceof Map) {
                buf.append("Map:size:");
                buf.append(((Map) object).size());
            } else if (object instanceof Object[]) {
                buf.append("Array:size:");
                buf.append(((Object[]) object).length);
            } else if (object == null) {
                buf.append("null");
            } else {
                buf.append(object.toString());
            }
        }
        buf.append(']');
        return buf.toString();
    }

}
