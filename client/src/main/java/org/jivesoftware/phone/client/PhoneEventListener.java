/**
 * $RCSfile: PhoneEventListener.java,v $
 * $Revision: 1.1.1.1 $
 * $Date: 2005/06/20 22:12:11 $
 *
 * Copyright (C) 1999-2004 Jive Software. All rights reserved.
 *
 * This software is the proprietary information of Jive Software. Use is subject to license terms.
 */
package org.jivesoftware.phone.client;

/**
 * Implement this listener and register with {@link org.jivesoftware.phone.client.PhoneClient}
 * to receive phone events.
 *
 * @author Andrew Wright
 */
public interface PhoneEventListener {

    /**
     * Called by Event dispatcher when a new phone event is created.
     * This method will be called for all types of phone events.
     *
     * For instance, to catch a HangUP Event do the following
     * <code>
     *   if(event instanceOf HangUpEvent) {
     *      HangUpEvent hangUp = (HangUp) event;
     *      ....
     *   }
     * </code>
     *
     * @param event
     */
    void handle(PhoneEvent event);

}
