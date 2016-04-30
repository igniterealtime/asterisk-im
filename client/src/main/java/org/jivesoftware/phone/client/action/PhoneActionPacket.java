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

    public static final class ActionType {
        /**
         * When the action is dialing a new extension
         */
        public static final ActionType DIAL = new ActionType("DIAL");

        /**
         * When the action is forwarding a call to a another extension
         */
        public static final ActionType FORWARD = new ActionType("FORWARD");

        /**
         * When the action is inviting a third person into the call
         */
        public static final ActionType INVITE = new ActionType("INVITE");

        private String name;

        public ActionType(String name) {
            this.name = name;
        }

        public String name() {
            return name;
        }

        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            final ActionType that = (ActionType) o;

            if (name != null ? !name.equals(that.name) : that.name != null) return false;

            return true;
        }

        public int hashCode() {
            return (name != null ? name.hashCode() : 0);
        }
    }

    /**
     * namespace for the phone schema
     */
    public static final String NAMESPACE = "http://jivesoftware.com/xmlns/phone";

    public static final String CHILD_ELEMENT_NAME = "phone-action";

    private String id = null;

    protected PhoneActionPacket() {
        super( CHILD_ELEMENT_NAME, NAMESPACE);
        super.setType(Type.set);
    }

    protected PhoneActionPacket(String id) {
        super( CHILD_ELEMENT_NAME, NAMESPACE);
        super.setType(Type.set);
        this.id = id;
    }

    /**
     * Returns a phone-action element
     *
     * @return a phone-action element
     */
    protected IQChildElementXmlStringBuilder getIQChildElementBuilder(IQChildElementXmlStringBuilder buffer) {

        buffer.rightAngleBracket();
        buffer.append("<")
                .append(CHILD_ELEMENT_NAME)
                .append(" xmlns=\"")
                .append(NAMESPACE)
                .append("\"  type=\"")
                .append(getActionType().name())
                .append("\"");


        if (id != null) {
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

        return buffer;


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
