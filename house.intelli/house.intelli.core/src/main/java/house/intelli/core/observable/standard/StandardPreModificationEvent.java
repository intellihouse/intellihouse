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

import house.intelli.core.observable.ModificationHandler;
import house.intelli.core.observable.ObservableCollection;

/**
 * Event class that encapsulates all the event information for a
 * standard collection event.
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
public class StandardPreModificationEvent extends StandardModificationEvent {

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
    public StandardPreModificationEvent(
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

        super(obsCollection, handler, type, preSize, index,
            object, repeat, previous, view, viewOffset);
    }

}
