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
 * Defines event constants for event handling and matching.
 * <p>
 * The constants in this class are of two types:
 * <ol>
 * <li>Methods - the base definitions (unique bits)
 * <li>Groups - combination definitions (method bits combined)
 * </ol>
 * <p>
 * Only a method constant may be compared using == to an event type.
 * This can include use in a switch statement
 * <p>
 * Any constant may be used for filtering.
 * They may combined using the bitwise OR, <code>|</code>.
 * They may negated using the bitwise NOT, <code>~</code>.
 *
 * @since Commons Events 1.0
 * @version $Revision: 155443 $ $Date: 2005-02-26 20:19:51 +0700 (Sa, 26 Feb 2005) $
 *
 * @author Stephen Colebourne
 */
public class ModificationEventType {

    /** The method add(Object) */
    public static final int ADD =           0x00000001;
    /** The method add(int,Object) */
    public static final int ADD_INDEXED =   0x00000002;
    /** The method add(Object,int) */
    public static final int ADD_NCOPIES =   0x00000004;
    /** The method iterator.add(Object) */
    public static final int ADD_ITERATED =  0x00000008;

    /** The method addAll(Collection) */
    public static final int ADD_ALL =       0x00000010;
    /** The method addAll(int,Collection) */
    public static final int ADD_ALL_INDEXED=0x00000020;

    /** The method remove(Object) */
    public static final int REMOVE =        0x00000100;
    /** The method remove(int) */
    public static final int REMOVE_INDEXED =0x00000200;
    /** The method remove(Object,int) */
    public static final int REMOVE_NCOPIES =0x00000400;
    /** The method remove() */
    public static final int REMOVE_NEXT    =0x00000800;
    /** The method iterator.remove() */
    public static final int REMOVE_ITERATED=0x00001000;

    /** The method removeAll(Collection) */
    public static final int REMOVE_ALL =    0x00002000;
    /** The method retainAll(Collection) */
    public static final int RETAIN_ALL =    0x00004000;
    /** The method clear() */
    public static final int CLEAR =         0x00008000;

    /** The method set(int,Object) */
    public static final int SET_INDEXED =   0x00010000;
    /** The method iterator.set(Object) */
    public static final int SET_ITERATED =  0x00020000;

    /** All add methods */
    public static final int GROUP_ADD = ADD | ADD_INDEXED | ADD_NCOPIES | ADD_ITERATED | ADD_ALL | ADD_ALL_INDEXED;
    /** All methods that change without structure modification */
    public static final int GROUP_CHANGE = SET_INDEXED | SET_ITERATED;
    /** All remove methods */
    public static final int GROUP_REMOVE = REMOVE | REMOVE_INDEXED | REMOVE_NCOPIES | REMOVE_ITERATED | REMOVE_NEXT | REMOVE_ALL;
    /** All retain methods */
    public static final int GROUP_RETAIN = RETAIN_ALL;
    /** All clear methods */
    public static final int GROUP_CLEAR = CLEAR;
    /** All reducing methods (remove, retain and clear) */
    public static final int GROUP_REDUCE = GROUP_REMOVE | GROUP_CLEAR | GROUP_RETAIN;

    /** All indexed methods */
    public static final int GROUP_INDEXED = ADD_INDEXED | ADD_ALL_INDEXED | REMOVE_INDEXED | SET_INDEXED;
    /** All ncopies methods */
    public static final int GROUP_NCOPIES = ADD_NCOPIES | REMOVE_NCOPIES;
    /** All iterated methods */
    public static final int GROUP_ITERATED = ADD_ITERATED | REMOVE_ITERATED | SET_ITERATED;
    /** All 'next' methods */
    public static final int GROUP_NEXT = REMOVE_NEXT;
    /** All bulk methods (xxxAll, clear) */
    public static final int GROUP_BULK =  ADD_ALL | ADD_ALL_INDEXED | REMOVE_ALL | RETAIN_ALL | CLEAR;
    /** All methods that modify the structure */
    public static final int GROUP_STRUCTURE_MODIFIED = GROUP_ADD | GROUP_REDUCE;

    /** All methods sent by a Collection */
    public static final int GROUP_FROM_COLLECTION = ADD | ADD_ALL | REMOVE | REMOVE_ALL | RETAIN_ALL | CLEAR;
    /** All methods sent by a Set */
    public static final int GROUP_FROM_SET = GROUP_FROM_COLLECTION;
    /** All methods sent by a List */
    public static final int GROUP_FROM_LIST = GROUP_FROM_COLLECTION | ADD_INDEXED | ADD_ALL_INDEXED | REMOVE_INDEXED | SET_INDEXED;
    /** All methods sent by a Bag */
    public static final int GROUP_FROM_BAG = GROUP_FROM_COLLECTION | ADD_NCOPIES | REMOVE_NCOPIES;
    /** All methods sent by a Buffer */
    public static final int GROUP_FROM_BUFFER = GROUP_FROM_COLLECTION | REMOVE_NEXT;

    /** No methods */
    public static final int GROUP_NONE = 0x00000000;
    /** All methods */
    public static final int GROUP_ALL = 0xFFFFFFFF;

    /**
     * Constructor.
     */
    protected ModificationEventType() {
        super();
    }

    /**
     * Gets a string version of a method event type.
     *
     * @param methodType  the method event type constant
     * @return a string description
     */
    public static String toString(final int methodType) {
        switch (methodType) {
            case ADD:
            return "Add";
            case ADD_INDEXED:
            return "AddIndexed";
            case ADD_NCOPIES:
            return "AddNCopies";
            case ADD_ITERATED:
            return "AddIterated";
            case ADD_ALL:
            return "AddAll";
            case ADD_ALL_INDEXED:
            return "AddAllIndexed";
            case REMOVE:
            return "Remove";
            case REMOVE_NCOPIES:
            return "RemoveNCopies";
            case REMOVE_INDEXED:
            return "RemoveIndexed";
            case REMOVE_ITERATED:
            return "RemoveIterated";
            case REMOVE_NEXT:
            return "RemoveNext";
            case REMOVE_ALL:
            return "RemoveAll";
            case RETAIN_ALL:
            return "RetainAll";
            case CLEAR:
            return "Clear";
            case SET_INDEXED:
            return "SetIndexed";
            case SET_ITERATED:
            return "SetIterated";
            default:
            return "Unknown";
        }
    }

}
