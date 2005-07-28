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


import org.jivesoftware.phone.client.event.PhoneEventPacketExtension;
import org.jivesoftware.phone.client.event.PhoneEventDispatcher;

/**
 * Event is thrown when the user's phone is ringing.
 * 
 * @author Andrew Wright
 */
public class RingEvent extends PhoneEventPacketExtension {

    public static final String CALLER_ID_ELEMENT = "callerID";

    private String callerID;

    private Call call;


    public RingEvent(String callID, String device, String callerID) {
        super(callID, device);
        this.callerID = callerID;
    }

    /**
     * Returns Caller id of caller
     *
     * @return caller id of the caller
     */
    public String getCallerID() {
        return callerID;
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
        return new StringBuffer("<")
                .append(CALLER_ID_ELEMENT)
                .append(">")
                .append(callerID)
                .append("</")
                .append(CALLER_ID_ELEMENT)
                .append(">")
                .toString();
    }

    public Call getCall() {
        return call;
    }

    public void initCall(PhoneEventDispatcher dispatcher)  {
        call = new Call(getCallID(), dispatcher);
    }

}
