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


import org.jivesoftware.phone.client.event.PhoneEventDispatcher;
import org.jivesoftware.phone.client.event.PhoneEventExtensionElement;

/**
 * Event is thrown when the user's phone is ringing.
 *
 * @author Andrew Wright
 */
public class RingEvent extends PhoneEventExtensionElement {

    public static final String CALLER_ID_ELEMENT = "callerID";
    public static final String CALLER_ID_NAME_ELEMENT = "callerIDName";

    private String callerID;
    private String callerIDName;

    private Call call;


    public RingEvent(String callID, String device, String callerID, String callerIDName) {
        super(callID, device);
        this.callerID = callerID;
        this.callerIDName = callerIDName;
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
     * Returns the name of the caller
     *
     * @return the name of the caller
     */
    public String getCallerIDName() {
        return callerIDName;
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
        StringBuilder xml = new StringBuilder("<").append(CALLER_ID_ELEMENT).append(">")
                .append(callerID)
                .append("</").append(CALLER_ID_ELEMENT).append(">");
        if (callerIDName != null && !"".equals(callerIDName)) {
            xml.append("<").append(CALLER_ID_NAME_ELEMENT).append(">")
                    .append(callerIDName != null ? callerIDName : "")
                    .append("</").append(CALLER_ID_NAME_ELEMENT).append(">");
        }
        return xml.toString();
    }

    public Call getCall() {
        return call;
    }

    public void initCall(PhoneEventDispatcher dispatcher) {
        call = new Call(getCallID(), dispatcher);
    }

}
