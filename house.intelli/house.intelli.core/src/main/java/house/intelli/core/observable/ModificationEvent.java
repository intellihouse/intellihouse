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
import java.util.EventObject;

/**
 * Base event class extended by each class that encapsulates event information.
 * <p>
 * This class can be used as is, but generally it is subclassed.
 *
 * @since Commons Events 1.0
 * @version $Revision: 155443 $ $Date: 2005-02-26 20:19:51 +0700 (Sa, 26 Feb 2005) $
 *
 * @author Stephen Colebourne
 */
public class ModificationEvent<E> extends EventObject {

    /** The source collection */
    protected final ObservableCollection<E> collection;
    /** The handler */
    protected final ModificationHandler handler;
    /** The event code */
    protected final int type;

    // Constructor
    //-----------------------------------------------------------------------
    /**
     * Constructor.
     *
     * @param obsCollection  the event source
     * @param handler  the handler
     * @param type  the event type
     */
    public ModificationEvent(
        final ObservableCollection<E> obsCollection,
        final ModificationHandler handler,
        final int type) {

        super(obsCollection);
        this.collection = obsCollection;
        this.handler = handler;
        this.type = type;
    }

    // Basic info
    //-----------------------------------------------------------------------
    /**
     * Gets the collection the event is reporting on.
     * <p>
     * Using this collection will bypass any decorators that have been added
     * to the <code>ObservableCollection</code>. For example, if a synchronized
     * decorator was added it will not be called by changes to this collection.
     * <p>
     * For the synchronization case, you are normally OK however. If you
     * process the event in the same thread as the original change then your
     * code will be protected by the original synchronized decorator and this
     * collection may be used freely.
     *
     * @return the collection
     */
    public ObservableCollection<E> getObservedCollection() {
        return collection;
    }

    /**
     * Gets the base collection underlying the observable collection.
     * <p>
     * Using this collection will bypass the event sending mechanism.
     * It will also bypass any other decorators, such as synchronization.
     * Use with care.
     *
     * @return the collection
     */
    public Collection<E> getBaseCollection() {
        return handler.getBaseCollection();
    }

    /**
     * Gets the handler of the events.
     *
     * @return the handler
     */
    public ModificationHandler getHandler() {
        return handler;
    }

    /**
     * Gets the event type constant.
     * <p>
     * This is one of the <i>method</i> constants from {@link ModificationEventType}.
     *
     * @return the method event type constant
     */
    public int getType() {
        return type;
    }

    // toString
    //-----------------------------------------------------------------------
    /**
     * Gets a debugging string version of the event.
     *
     * @return a debugging string
     */
    @Override
	public String toString() {
        StringBuffer buf = new StringBuffer(64);
        buf.append("ModificationEvent[type=");
        buf.append(ModificationEventType.toString(type));
        buf.append(']');
        return buf.toString();
    }

}
