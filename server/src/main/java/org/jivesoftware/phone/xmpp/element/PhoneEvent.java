/**
 * $RCSfile: PhoneEvent.java,v $
 * $Revision: 1.4 $
 * $Date: 2005/06/30 21:39:38 $
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
public class PhoneEvent extends BaseElement {

    private static final Namespace NAMESPACE = new Namespace(null, PhoneConstants.NAMESPACE);
    private static final long serialVersionUID = 2051032622352291397L;

    public static enum Type {
        ON_PHONE,
        HANG_UP,
        RING,
        DIALED
    }

    public static final String ELEMENT_NAME = "phone-event";

    public PhoneEvent() {
        super(ELEMENT_NAME, NAMESPACE);
    }

    public PhoneEvent(String callID, Type type, String device) {
        this();
        setCallID(callID);
        setType(type);
        setDevice(device);
    }

    public void setCallID(String id) {
        if (id == null) {
            return;
        }

        addAttribute("callID", id);
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
