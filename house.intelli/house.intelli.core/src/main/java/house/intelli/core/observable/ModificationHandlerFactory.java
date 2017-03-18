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

/**
 * Defines a factory for creating ModificationHandler instances.
 * <p>
 * If an application wants to register its own event handler classes, it should
 * do so using this class. This must be done during initialization to be
 * fully thread-safe. There are two steps:
 * <ol>
 * <li>A factory must be created that is an implementation of this class
 * <li>One of the <code>registerFactory</code> methods must be called on ObservableCollection
 * </ol>
 *
 * @since Commons Events 1.0
 * @version $Revision: 155443 $ $Date: 2005-02-26 20:19:51 +0700 (Sa, 26 Feb 2005) $
 *
 * @author Stephen Colebourne
 */
public interface ModificationHandlerFactory {

    /**
     * Creates a handler subclass for the specified listener.
     * <p>
     * The implementation will normally check to see if the listener
     * is of a suitable type, and then cast it. <code>null</code> is
     * returned if this factory does not handle the specified type.
     * <p>
     * The listener is defined in terms of an Object to allow for unusual
     * listeners, such as a Swing model object.
     * <p>
     * The collection the handler is for is passed in to allow for a different
     * handler to be selected for the same listener type based on the collection.
     *
     * @param coll  the collection being decorated
     * @param listener  a listener object to create a handler for
     * @return an instantiated handler with the listener attached,
     *  or null if the listener type is unsuited to this factory
     */
    ModificationHandler createHandler(Collection coll, Object listener);

}
