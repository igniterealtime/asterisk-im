/**
 * $RCSfile: PhoneEventDispatcher.java,v $
 * $Revision: 1.1.1.1 $
 * $Date: 2005/06/20 22:12:11 $
 *
 * Copyright (C) 1999-2004 Jive Software. All rights reserved.
 *
 * This software is the proprietary information of Jive Software. Use is subject to license terms.
 */

package org.jivesoftware.phone.client.event;

import org.jivesoftware.phone.client.PhoneEvent;
import org.jivesoftware.phone.client.PhoneEventListener;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Event dispatcher for phone events.
 *
 * @author Andrew Wright
 */
public class PhoneEventDispatcher {

    private final List<PhoneEventListener> listeners = new CopyOnWriteArrayList<PhoneEventListener>();

    /**
     * Adds a new listener to this event dispatcher instance
     *
     * @param listener listener to add
     */
    public void addListener(PhoneEventListener listener) {
        if (listener == null) {
            throw new NullPointerException();
        }

        listeners.add(listener);
    }

    /**
     * Removes a listener from the event dispatcher instance
     *
     * @param listener listener to remove
     */
    public void removeListener(PhoneEventListener listener) {
        listeners.remove(listener);
    }


    /**
     * Dispatches an event to all the listeners
     *
     * @param event event to dispatch
     */
    public void dispatchEvent(PhoneEvent event) {
        for (PhoneEventListener listener : listeners) {
            listener.handle(event);
        }
    }
}