/**
 * $RCSfile: PhoneActionException.java,v $
 * $Revision: 1.2 $
 * $Date: 2005/06/25 02:09:35 $
 *
 * Copyright (C) 1999-2004 Jive Software. All rights reserved.
 *
 * This software is the proprietary information of Jive Software. Use is subject to license terms.
 */
package org.jivesoftware.phone.client;

import org.jivesoftware.smack.packet.XMPPError;

/**
 * Thrown when there is a problem trying to execute a phone action
 *
 * @author Andrew Wright
 */
public class PhoneActionException extends PhoneException {

    public PhoneActionException() {
        super();
    }

    public PhoneActionException(String message) {
        super(message);
    }

    public PhoneActionException(String message, Throwable wrappedThrowable) {
        super(message, wrappedThrowable);
    }
}
