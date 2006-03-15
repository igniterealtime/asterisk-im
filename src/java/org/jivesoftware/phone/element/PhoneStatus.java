/**
 * $RCSfile: PhoneStatus.java,v $
 * $Revision: 1.3 $
 * $Date: 2005/06/30 21:39:09 $
 *
 * Copyright (C) 1999-2004 Jive Software. All rights reserved.
 *
 * This software is the proprietary information of Jive Software. Use is subject to license terms.
 */
package org.jivesoftware.phone.element;

import org.dom4j.Namespace;
import org.dom4j.tree.BaseElement;
import org.jivesoftware.phone.util.PhoneConstants;

/**
 * @author Andrew Wright
 */
public class PhoneStatus extends BaseElement {

    private static final Namespace NAMESPACE = new Namespace(null, PhoneConstants.NAMESPACE);

    public static final String ELEMENT_NAME = "phone-status";

    public static enum Status {
        ON_PHONE,
        AVAILABLE
    }


    public PhoneStatus() {
        super(ELEMENT_NAME, NAMESPACE);
    }

    public PhoneStatus(Status status) {
        this();
        setStatus(status);
    }

    public void setStatus(Status status) {
        addAttribute("status", status.name());
    }

    public Status getStatus() {
        String status = attributeValue("status");
        return Status.valueOf(status);
    }

}
