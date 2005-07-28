/**
 * $RCSfile: PhoneActionPacket.java,v $
 * $Revision: 1.5 $
 * $Date: 2005/07/05 18:41:09 $
 *
 * Copyright (C) 1999-2004 Jive Software. All rights reserved.
 *
 * This software is the proprietary information of Jive Software. Use is subject to license terms.
 */
package org.jivesoftware.phone.client.action;

import org.jivesoftware.smack.packet.IQ;

/**
 * Base class for all phone-actions
 *
 * @author Andrew Wright
 */
public abstract class PhoneActionPacket extends IQ {

    public static enum ActionType {
        /**
         * When the action is dialing a new extension
         */
        DIAL,
        /**
         * When the action is forwarding a call to a another extension
         */
        FORWARD,
        /**
         * When the action is inviting a third person into the call
         */
        INVITE
    }

    /**
     * namespace for the phone schema
     */
    public static final String NAMESPACE = "http://jivesoftware.com/xmlns/phone";

    public static final String CHILD_ELEMENT_NAME = "phone-action";

    private String id = null;

    protected PhoneActionPacket() {
        super.setType(IQ.Type.SET);
    }

    protected PhoneActionPacket(String id) {
        super.setType(IQ.Type.SET);
        this.id = id;
    }

    /**
     * Returns a phone-action element
     *
     * @return a phone-action element
     */
    public final String getChildElementXML() {

        StringBuffer buffer = new StringBuffer("<")
                .append(CHILD_ELEMENT_NAME)
                .append(" xmlns=\"")
                .append(NAMESPACE)
                .append("\"  type=\"")
                .append(getActionType())
                .append( "\"");


        if(id != null) {
            buffer.append(" id=\"")
                    .append(id)
                    .append("\" ");
        }


        buffer.append(" >");

        String childElement = getActionChildElementXML();

        if (childElement != null) {
            buffer.append(childElement);
        }

        buffer.append("</")
                .append(CHILD_ELEMENT_NAME)
                .append(">");

        return buffer.toString();


    }

    /**
     * All the child content that should be inserted as a child to
     * the phone-action element
     *
     * @return content that should be the child of phone-action element
     */
    protected abstract String getActionChildElementXML();

    /**
     * the phone-action type
     *
     * @return the phone action type
     */
    public abstract ActionType getActionType();

}
