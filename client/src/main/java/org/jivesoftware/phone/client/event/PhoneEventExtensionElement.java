/**
 * $RCSfile: PhoneEventExtensionElement.java,v $
 * $Revision: 1.6 $
 * $Date: 2005/06/25 21:02:15 $
 *
 * Copyright (C) 1999-2004 Jive Software. All rights reserved.
 *
 * This software is the proprietary information of Jive Software. Use is subject to license terms.
 */
package org.jivesoftware.phone.client.event;

import org.jivesoftware.phone.client.PhoneEvent;
import org.jivesoftware.smack.packet.ExtensionElement;

/**
 * Base class for all phone-event packets
 *
 * @author Andrew Wright
 */
public abstract class PhoneEventExtensionElement implements PhoneEvent, ExtensionElement {

    /**
     * namespace for the phone schema
     */
    public static final String NAMESPACE = "http://jivesoftware.com/xmlns/phone";

    public static final String ELEMENT_NAME = "phone-event";

    private String device;
    private String callID = null;

    protected PhoneEventExtensionElement(String device) {
        this.device = device;
    }

    protected PhoneEventExtensionElement(String callID, String device) {
        this.device = device;
        this.callID = callID;
    }

    public String toXML() {

        StringBuffer buffer = new StringBuffer()
                .append("<")
                .append(ELEMENT_NAME)
                .append(" type=\"")
                .append(getEventStatus());


        if (callID != null) {
            buffer.append("\" callID=\"")
                    .append(callID)
                    .append("\" ");
        }

        buffer.append(" >");

        String child = getEventChildXML();

        if (child != null) {
            buffer.append(child);
        }

        buffer.append("</")
                .append(ELEMENT_NAME)
                .append(">");


        return buffer.toString();
    }

    /**
     * Returns phone-event
     *
     * @return "phone-event"
     */
    public String getElementName() {
        return ELEMENT_NAME;
    }

    /**
     * Returns "http://jivesoftware.com/xmlns/phone"
     *
     * @return "http://jivesoftware.com/xmlns/phone"
     */
    public String getNamespace() {
        return NAMESPACE;
    }

    public String getDevice() {
        return this.device;
    }

    public String getCallID() {
        return callID;
    }

    /**
     * Subclasses must implement this to return any child content that must be placed
     * in the packet extension
     *
     * @return child xml content
     */
    protected abstract String getEventChildXML();

}
