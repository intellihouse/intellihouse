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

import house.intelli.core.observable.ModificationListener;

/**
 * A listener for the <code>StandardModificationHandler</code> that is called
 * when a collection has been changed.
 *
 * @since Commons Events 1.0
 * @version $Revision: 155443 $ $Date: 2005-02-26 20:19:51 +0700 (Sa, 26 Feb 2005) $
 *
 * @author Stephen Colebourne
 */
public interface StandardPostModificationListener extends ModificationListener {

    /**
     * A collection modification occurred.
     * <p>
     * This method should be processed quickly, as with all event handling.
     * It should also avoid modifying the event source (the collection).
     * Finally it should avoid throwing an exception.
     *
     * @param event  the event detail
     */
    public void modificationOccurred(StandardPostModificationEvent event);

}
