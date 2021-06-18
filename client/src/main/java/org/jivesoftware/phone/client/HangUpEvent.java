/**
 * $RCSfile: HangUpEvent.java,v $
 * $Revision: 1.5 $
 * $Date: 2005/06/25 02:09:35 $
 *
 * Copyright (C) 1999-2004 Jive Software. All rights reserved.
 *
 * This software is the proprietary information of Jive Software. Use is subject to license terms.
 */
package org.jivesoftware.phone.client;


import org.jivesoftware.phone.client.event.PhoneEventExtensionElement;

/**
 * Event is dispatched when the current xmpp user hangs up his phone
 *
 * @author Andrew Wright
 */
public class HangUpEvent extends PhoneEventExtensionElement {

    public HangUpEvent(String callID, String device) {
        super(callID, device);
    }

    /**
     * Returns {@link EventStatus#HANG_UP}
     *
     * @return {@link EventStatus#HANG_UP}
     */
    public EventStatus getEventStatus() {
        return EventStatus.HANG_UP;
    }

    /**
     * Always returns null, there is no child element for this event
     *
     * @return Returns null
     */
    protected String getEventChildXML() {
        return null;
    }

}
