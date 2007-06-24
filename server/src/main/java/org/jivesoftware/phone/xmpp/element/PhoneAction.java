/**
 * $RCSfile: PhoneAction.java,v $
 * $Revision: 1.5 $
 * $Date: 2005/07/02 00:22:51 $
 *
 * Copyright (C) 1999-2004 Jive Software. All rights reserved.
 *
 * This software is the proprietary information of Jive Software. Use is subject to license terms.
 */
package org.jivesoftware.phone.xmpp.element;

import org.dom4j.Namespace;
import org.dom4j.tree.BaseElement;
import org.jivesoftware.phone.util.PhoneConstants;

/**
 * @author Andrew Wright
 */
public class PhoneAction extends BaseElement {

    private static final Namespace NAMESPACE = new Namespace(null, PhoneConstants.NAMESPACE);
    private static final long serialVersionUID = -2541186522728239794L;

    public static enum Type {
        /**
         * When the action is dialing a new extension.
         */
        DIAL,
        /**
         * When the action is forwarding a call to a another extension.
         */
        FORWARD,
        /**
         * When the action is inviting a third person into the call.
         */
        INVITE
    }

    public static final String ELEMENT_NAME = "phone-action";

    public PhoneAction() {
        super(ELEMENT_NAME, NAMESPACE);
    }

    public PhoneAction(Type type) {
        this();
        setType(type);
    }

    public String getCallID() {
        return attributeValue("callID");
    }

    public void setType(Type type) {
        if (type == null) {
            return;
        }
        addAttribute("type", type.name());
    }

    public Type getType() {
        String type = attributeValue("type");
        return Type.valueOf(type);
    }

    public void setDevice(String device) {
        addAttribute("device", device);
    }

    public String getDevice() {
        return attributeValue("device");
    }

}
