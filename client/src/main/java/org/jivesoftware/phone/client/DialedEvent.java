/**
 * $RCSfile: RingEvent.java,v $
 * $Revision: 1.5 $
 * $Date: 2005/06/30 21:46:08 $
 *
 * Copyright (C) 1999-2004 Jive Software. All rights reserved.
 *
 * This software is the proprietary information of Jive Software. Use is subject to license terms.
 */
package org.jivesoftware.phone.client;


import org.jivesoftware.phone.client.event.PhoneEventExtensionElement;
import org.jivesoftware.phone.client.event.PhoneEventDispatcher;

/**
 * Event is dispatched when the user has dialed and we are waiting for someone to answer, this doesn't seem
 * to be be dispatched when originating (PhoneClient#dial) calls with asterisk. 
 *
 * 
 * @author Andrew Wright
 */
public class DialedEvent extends PhoneEventExtensionElement {

    private Call call;


    public DialedEvent(String callID, String device) {
        super(callID, device);
    }

    /**
     * Returns RING
     *
     * @return RING
     */
    public EventStatus getEventStatus() {
        return EventStatus.RING;
    }

    protected String getEventChildXML() {
       return null;
    }

    public Call getCall() {
        return call;
    }

    public void initCall(PhoneEventDispatcher dispatcher)  {
        call = new Call(getCallID(), dispatcher);
    }

}
