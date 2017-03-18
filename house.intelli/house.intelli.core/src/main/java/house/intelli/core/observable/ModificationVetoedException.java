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

/**
 * Exception thrown when a modification to a collection is vetoed.
 * It extends IllegalArgumentException for compatibility with the collections API.
 *
 * @since Commons Events 1.0
 * @version $Revision: 155443 $ $Date: 2005-02-26 20:19:51 +0700 (Sa, 26 Feb 2005) $
 *
 * @author Stephen Colebourne
 */
public class ModificationVetoedException extends IllegalArgumentException {

    /** The source event */
    protected final ModificationEvent event;

    // Constructor
    //-----------------------------------------------------------------------
    /**
     * Constructor.
     *
     * @param message  the text message, may be null
     * @param event  the observed event, should not be null
     */
    public ModificationVetoedException(final String message, final ModificationEvent event) {
        super((message == null ? "Modification vetoed" : message));
        this.event = event;
    }

    // Event access
    //-----------------------------------------------------------------------
    /**
     * Gets the event that caused the veto.
     *
     * @return the event
     */
    public ModificationEvent getEvent() {
        return event;
    }

}
