/**
 * $RCSfile: OnPhoneEvent.java,v $
 * $Revision: 1.2 $
 * $Date: 2005/06/25 02:09:35 $
 *
 * Copyright (C) 1999-2004 Jive Software. All rights reserved.
 *
 * This software is the proprietary information of Jive Software. Use is subject to license terms.
 */
package org.jivesoftware.phone.client;


import org.jivesoftware.phone.client.event.PhoneEventDispatcher;
import org.jivesoftware.phone.client.event.PhoneEventExtensionElement;

/**
 * This event will be dispatched when the user answer's his/her phone.
 * This will also be sent when the user has called someone and the other party has picked up the
 * phone.
 *
 * @author Andrew Wright
 */
public class OnPhoneEvent extends PhoneEventExtensionElement {

    public static final String CALLER_ID_ELEMENT = "callerID";
    public static final String CALLER_ID_NAME_ELEMENT = "callerIDName";

    private String callerID;
    private String callerIDName;

    private Call call;

    public OnPhoneEvent(String callID, String device, String callerID, String callerIDName) {
        super(callID, device);
        this.callerID = callerID;
        this.callerIDName = callerIDName;
    }

    /**
     * Returns the caller Id (phone number representation) of the caller
     *
     * @return The caller id of the caller
     */
    public String getCallerID() {
        return callerID;
    }

    /**
     * Returns the caller Id name (textual representation) of the caller
     *
     * @return The caller id of the caller
     */
    public String getCallerIDName() {
        return callerIDName;
    }

    /**
     * Returns {@link EventStatus#ON_PHONE}
     *
     * @return {@link EventStatus#ON_PHONE}
     */
    public EventStatus getEventStatus() {
        return EventStatus.ON_PHONE;
    }

    /**
     * Returns:
     * &lt;callerID&gt;calleridstring&lt;/callerID&gt;
     *
     * @return a callerID element
     */
    protected String getEventChildXML() {
        return new StringBuilder("<").append(CALLER_ID_ELEMENT).append(">")
                .append(callerID)
                .append("</").append(CALLER_ID_ELEMENT).append(">")
                .append("<").append(CALLER_ID_NAME_ELEMENT).append(">")
                .append(callerIDName)
                .append("</").append(CALLER_ID_NAME_ELEMENT).append(">")
                .toString();
    }

    public Call getCall() {
        return call;
    }

    public void initCall(PhoneEventDispatcher dispatcher) {
        call = new Call(getCallID(), dispatcher);
    }


}
