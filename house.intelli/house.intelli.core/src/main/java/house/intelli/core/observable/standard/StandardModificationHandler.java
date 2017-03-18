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

import house.intelli.core.observable.ModificationEventType;
import house.intelli.core.observable.ModificationHandler;
import house.intelli.core.observable.ModificationHandlerFactory;
import house.intelli.core.observable.ObservableCollection;

/**
 * The standard implementation of a <code>ModificationHandler</code> that
 * sends standard JavaBean style events to listeners.
 * <p>
 * The information gathered by this implementation is all that is available
 * as parameters or return values.
 * In addition, the <code>size</code> method is used on the collection.
 * All objects used are the real objects from the method calls, not clones.
 * <p>
 * Each listener can be filtered. There are separate filters for pre and post
 * modification events.
 *
 * @since Commons Events 1.0
 * @version $Revision: 155443 $ $Date: 2005-02-26 20:19:51 +0700 (Sa, 26 Feb 2005) $
 *
 * @author Stephen Colebourne
 */
public class StandardModificationHandler<E> extends ModificationHandler<E> {

    /** The singleton factory */
    public static final ModificationHandlerFactory FACTORY = new Factory();

    /** A reusable empty holders array. */
    protected static final PreHolder[] EMPTY_PRE_HOLDERS = new PreHolder[0];
    /** A reusable empty holders array. */
    protected static final PostHolder[] EMPTY_POST_HOLDERS = new PostHolder[0];

    /** The event mask as to which event types to send on pre events. */
    protected int preMask = ModificationEventType.GROUP_NONE;
    /** The event mask as to which event types to send on post events. */
    protected int postMask = ModificationEventType.GROUP_NONE;

    /** The event listeners. */
    protected PreHolder[] preHolder = EMPTY_PRE_HOLDERS;
    /** The event listeners. */
    protected PostHolder[] postHolder = EMPTY_POST_HOLDERS;
    /**
     * Temporary store for the size.
     * This makes the class thread-unsafe, but you should sync collections anyway.
     */
    protected int preSize;

    // Constructors
    //-----------------------------------------------------------------------
    /**
     * Constructor the creates the handler but leaves it invalid.
     * <p>
     * The handler can only be used after it has been properly initialized.
     * This is normally done automatically by
     * {@link ObservableCollection#decorate(Collection, Object)}.
     */
    public StandardModificationHandler() {
        super();
    }

    /**
     * Constructor the creates the handler but leaves it invalid.
     * <p>
     * The handler can only be used after it has been properly initialized.
     * This is normally done automatically by
     * {@link ObservableCollection#decorate(Collection, Object)}.
     *
     * @param pre  the pre listener
     * @param preMask  the mask for the pre listener
     * @param post  the post listener
     * @param postMask  the mask for the post listener
     */
    public StandardModificationHandler(
            StandardPreModificationListener pre, int preMask,
            StandardPostModificationListener post, int postMask) {
        super();
        if (pre != null) {
            preHolder = new PreHolder[1];
            preHolder[0] = new PreHolder(pre, preMask);
            this.preMask = preMask;
        }
        if (post != null) {
            postHolder = new PostHolder[1];
            postHolder[0] = new PostHolder(post, postMask);
            this.postMask = postMask;
        }
    }

    // Pre Listeners
    //----------------------------------------------------------------------
    /**
     * Gets an array of all the pre listeners active in the handler.
     * <p>
     * All listeners will be instances of StandardPreModificationListener.
     *
     * @return the listeners
     */
    @Override
	public synchronized Object[] getPreModificationListeners() {
        Object[] lnrs = new Object[preHolder.length];
        for (int i = 0; i < preHolder.length; i++) {
            lnrs[i] = preHolder[i].listener;
        }
        return lnrs;
    }

    /**
     * Adds a listener to the handler for pre modification events.
     * <p>
     * No error occurs if the listener is <code>null</code>.
     *
     * @param listener  the listener to add, may be null (ignored)
     * @throws ClassCastException if the listener is not a StandardPreModificationListener
     */
    @Override
	public void addPreModificationListener(Object listener) {
        addPreModificationListener((StandardPreModificationListener) listener, -1);
    }

    /**
     * Adds a pre listener to the list held in the handler.
     * <p>
     * No error occurs if the listener is <code>null</code>.
     *
     * @param listener  the listener to add, may be null (ignored)
     * @param mask  the mask for events (0 for none, -1 for all)
     */
    public synchronized void addPreModificationListener(StandardPreModificationListener listener, int mask) {
        if (listener != null) {
            int oldSize = preHolder.length;
            PreHolder[] array = new PreHolder[oldSize + 1];
            System.arraycopy(preHolder, 0, array, 0, oldSize);
            array[oldSize] = new PreHolder(listener, mask);
            preHolder = array;
            calculatePreMask();
        }
    }

    /**
     * Removes a pre listener to the list held in the handler.
     * <p>
     * No error occurs if the listener is not in the list or the type
     * of the listener is incorrect.
     * The listener is matched using ==.
     *
     * @param listener  the listener to remove, may be null (ignored)
     */
    @Override
	public synchronized void removePreModificationListener(Object listener) {
        if (listener != null) {
            switch (preHolder.length) {
                case 0:
                return;

                case 1:
                if (preHolder[0].listener == listener) {
                    preHolder = EMPTY_PRE_HOLDERS;
                    calculatePreMask();
                }
                return;

                default:
                PreHolder[] array = new PreHolder[preHolder.length - 1];
                boolean match = false;
                for (int i = 0; i < preHolder.length; i++) {
                    if (match) {
                        array[i - 1] = preHolder[i];
                    } else if (preHolder[i].listener == listener) {
                        match = true;
                    } else {
                        array[i] = preHolder[i];
                    }
                }
                preHolder = array;
                calculatePreMask();
                return;
            }
        }
    }

    /**
     * Sets the masks of a listener.
     * <p>
     * No error occurs if the listener is not in the list.
     * The listener is matched using ==.
     *
     * @param listener  the listener to change, may be null
     * @param mask  the new mask (0 for none, -1 for all)
     */
    public synchronized void setPreModificationListenerMask(StandardPreModificationListener listener, int mask) {
        if (listener != null) {
            for (int i = 0; i < preHolder.length; i++) {
                if (preHolder[i].listener == listener) {
                    preHolder[i].mask = mask;
                    calculatePreMask();
                    break;
                }
            }
        }
    }

    /**
     * Calculate the combined masks.
     */
    protected void calculatePreMask() {
        preMask = ModificationEventType.GROUP_NONE;
        for (int i = 0; i < preHolder.length; i++) {
            preMask |= preHolder[i].mask;
        }
    }

    protected static class PreHolder {
        final StandardPreModificationListener listener;
        int mask;

        PreHolder(StandardPreModificationListener listener, int mask) {
            this.listener = listener;
            this.mask = mask;
        }

        @Override
		public String toString() {
            return "[" + listener + "," + ModificationEventType.toString(mask) + "]";
        }

    }

    // Post Listeners
    //----------------------------------------------------------------------
    /**
     * Gets an array of all the post listeners active in the handler.
     * <p>
     * All listeners will be instances of StandardModificationListener.
     *
     * @return the listeners
     */
    @Override
	public synchronized Object[] getPostModificationListeners() {
        Object[] lnrs = new Object[postHolder.length];
        for (int i = 0; i < postHolder.length; i++) {
            lnrs[i] = postHolder[i].listener;
        }
        return lnrs;
    }

    /**
     * Adds a listener to the handler for post modification events.
     * <p>
     * No error occurs if the listener is <code>null</code>.
     *
     * @param listener  the listener to add, may be null (ignored)
     * @throws ClassCastException if the listener is not a StandardPreModificationListener
     */
    @Override
	public void addPostModificationListener(Object listener) {
        addPostModificationListener((StandardPostModificationListener) listener, -1);
    }

    /**
     * Adds a post listener to the list held in the handler.
     * <p>
     * No error occurs if the listener is <code>null</code>.
     *
     * @param listener  the listener to add, may be null (ignored)
     * @param mask  the mask for events (0 for none, -1 for all)
     */
    public synchronized void addPostModificationListener(StandardPostModificationListener listener, int mask) {
        if (listener != null) {
            int oldSize = postHolder.length;
            PostHolder[] array = new PostHolder[oldSize + 1];
            System.arraycopy(postHolder, 0, array, 0, oldSize);
            array[oldSize] = new PostHolder(listener, mask);
            postHolder = array;
            calculatePostMask();
        }
    }

    /**
     * Removes a post listener to the list held in the handler.
     * <p>
     * No error occurs if the listener is not in the list or the type
     * of the listener is incorrect.
     * The listener is matched using ==.
     *
     * @param listener  the listener to remove, may be null (ignored)
     */
    @Override
	public synchronized void removePostModificationListener(Object listener) {
        if (listener != null) {
            switch (postHolder.length) {
                case 0:
                return;

                case 1:
                if (postHolder[0].listener == listener) {
                    postHolder = EMPTY_POST_HOLDERS;
                    calculatePostMask();
                }
                return;

                default:
                PostHolder[] array = new PostHolder[postHolder.length - 1];
                boolean match = false;
                for (int i = 0; i < postHolder.length; i++) {
                    if (match) {
                        array[i - 1] = postHolder[i];
                    } else if (postHolder[i].listener == listener) {
                        match = true;
                    } else {
                        array[i] = postHolder[i];
                    }
                }
                postHolder = array;
                calculatePostMask();
                return;
            }
        }
    }

    /**
     * Sets the masks of a listener.
     * <p>
     * No error occurs if the listener is not in the list.
     * The listener is matched using ==.
     *
     * @param listener  the listener to change, may be null
     * @param mask  the new mask (0 for none, -1 for all)
     */
    public synchronized void setPostModificationListenerMask(StandardPostModificationListener listener, int mask) {
        if (listener != null) {
            for (int i = 0; i < postHolder.length; i++) {
                if (postHolder[i].listener == listener) {
                    postHolder[i].mask = mask;
                    calculatePostMask();
                    break;
                }
            }
        }
    }

    /**
     * Calculate the combined masks.
     */
    protected void calculatePostMask() {
        postMask = ModificationEventType.GROUP_NONE;
        for (int i = 0; i < postHolder.length; i++) {
            postMask |= postHolder[i].mask;
        }
    }

    protected static class PostHolder {
        final StandardPostModificationListener listener;
        int mask;

        PostHolder(StandardPostModificationListener listener, int mask) {
            this.listener = listener;
            this.mask = mask;
        }

        @Override
		public String toString() {
            return "[" + listener + "," + ModificationEventType.toString(mask) + "]";
        }

    }

    // Pre event sending
    //-----------------------------------------------------------------------
    /**
     * Handles the pre event.
     *
     * @param type  the event type to send
     * @param index  the index where the change starts, the method param or derived
     * @param object  the object that will be added/removed/set, the method param or derived
     * @param repeat  the number of repeats of the add/remove, the method param or derived
     * @param previous  the previous value that will be removed/replaced, must exist in coll
     * @param view  the view collection that the change was actioned on, null if no view
     * @param viewOffset  the offset of the subList view, -1 if unknown
     * @return true to call the decorated collection
     */
    @Override
	protected boolean preEvent(
            int type, int index, Object object,
            int repeat, Object previous, ObservableCollection view, int viewOffset) {

        preSize = getObservedCollection().size();
        return firePreEvent(type, index, object, repeat, previous, view, viewOffset);
    }

    /**
     * Sends the pre event to the listeners.
     *
     * @param type  the event type to send
     * @param index  the index where the change starts, the method param or derived
     * @param object  the object that will be added/removed/set, the method param or derived
     * @param repeat  the number of repeats of the add/remove, the method param or derived
     * @param previous  the previous value that will be removed/replaced, must exist in coll
     * @param view  the view collection that the change was actioned on, null if no view
     * @param viewOffset  the offset of the subList view, -1 if unknown
     * @return true to call the decorated collection
     */
    protected boolean firePreEvent(
            int type, int index, Object object, int repeat,
            Object previous, ObservableCollection view, int viewOffset) {

        if ((preMask & type) > 0) {
            StandardPreModificationEvent event = null;
            synchronized (this) {
                for (int i = 0; i < preHolder.length; i++) {
                    PreHolder holder = preHolder[i];
                    if ((holder.mask & type) > 0) {
                        if (event == null) {
                            event = new StandardPreModificationEvent(
                                getObservedCollection(), this, type, preSize, index, object,
                                repeat, previous, view, viewOffset);
                        }
                        holder.listener.modificationOccurring(event);
                    }
                }
            }
        }
        return true;
    }

    // Post event sending
    //-----------------------------------------------------------------------
    /**
     * Handles the post event.
     *
     * @param modified  true if the method succeeded in changing the collection
     * @param type  the event type to send
     * @param index  the index where the change starts, the method param or derived
     * @param object  the object that was added/removed/set, the method param or derived
     * @param repeat  the number of repeats of the add/remove, the method param or derived
     * @param previous  the previous value that was removed/replace, must have existed in coll
     * @param view  the view collection that the change was actioned on, null if no view
     * @param viewOffset  the offset of the subList view, -1 if unknown
     */
    @Override
	protected void postEvent(
            boolean modified, int type, int index, Object object,
            int repeat, Object previous, ObservableCollection view, int viewOffset) {

        if (modified) {
            firePostEvent(type, index, object, repeat, previous, view, viewOffset);
        }
    }

    /**
     * Sends the post event to the listeners.
     *
     * @param type  the event type to send
     * @param index  the index where the change starts, the method param or derived
     * @param object  the object that was added/removed/set, the method param or derived
     * @param repeat  the number of repeats of the add/remove, the method param or derived
     * @param previous  the previous value that was removed/replace, must have existed in coll
     * @param view  the view collection that the change was actioned on, null if no view
     * @param viewOffset  the offset of the subList view, -1 if unknown
     */
    protected void firePostEvent(
            int type, int index, Object object, int repeat,
            Object previous, ObservableCollection view, int viewOffset) {

        if ((postMask & type) > 0) {
            StandardPostModificationEvent event = null;
            synchronized (this) {
                for (int i = 0; i < postHolder.length; i++) {
                    PostHolder holder = postHolder[i];
                    if ((holder.mask & type) > 0) {
                        if (event == null) {
                            event = new StandardPostModificationEvent(
                                getObservedCollection(), this, type, preSize, index,
                                object, repeat, previous, view, viewOffset);
                        }
                        holder.listener.modificationOccurred(event);
                    }
                }
            }
        }
    }

    // Event handling
    //-----------------------------------------------------------------------
    /**
     * Send an event after clear() is called.
     * <p>
     * Override to only send event if something actually cleared.
     */
    @Override
	protected void postClear() {
        postEvent(preSize > 0, ModificationEventType.CLEAR, -1, null, 1, null, null, -1);
    }

    // Factory
    //-----------------------------------------------------------------------
    /**
     * Factory implementation for the StandardModificationHandler.
     *
     * @author Stephen Colebourne
     */
    static class Factory implements ModificationHandlerFactory {

        /**
         * Creates a StandardModificationHandler using the listener.
         *
         * @param coll  the collection being decorated
         * @param listener  a listener object to create a handler for
         * @return an instantiated handler with the listener attached,
         *  or null if the listener type is unsuited to this factory
         */
        @Override
		public ModificationHandler createHandler(Collection coll, Object listener) {
            if (listener instanceof StandardPreModificationListener) {
                if (listener instanceof StandardPostModificationListener) {
                    return new StandardModificationHandler(
                        (StandardPreModificationListener) listener, -1,
                        (StandardPostModificationListener) listener, -1);
                } else {
                    return new StandardModificationHandler(
                        (StandardPreModificationListener) listener, -1, null, 0);
                }
            }
            if (listener instanceof StandardPostModificationListener) {
                return new StandardModificationHandler(
                    null, 0, (StandardPostModificationListener) listener, -1);
            }
            return null;
        }
    }

}
