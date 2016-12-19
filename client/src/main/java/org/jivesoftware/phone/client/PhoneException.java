/**
 * $RCSfile: PhoneException.java,v $
 * $Revision: 1.1 $
 * $Date: 2005/06/25 02:09:35 $
 *
 * Copyright (C) 1999-2004 Jive Software. All rights reserved.
 *
 * This software is the proprietary information of Jive Software. Use is subject to license terms.
 */
package org.jivesoftware.phone.client;

import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.XMPPError;

/**
 * @author Andrew Wright
 */
public class PhoneException extends XMPPException {

    public PhoneException() {
        super();
    }

    public PhoneException(String message) {
        super(message);
    }

    public PhoneException(Throwable wrappedThrowable) {
        super(wrappedThrowable);
    }

    public PhoneException(XMPPError error) {
        super(error);
    }

    public PhoneException(String message, Throwable wrappedThrowable) {
        super(message, wrappedThrowable);
    }

    public PhoneException(String message, XMPPError error, Throwable wrappedThrowable) {
        super(message, error, wrappedThrowable);
    }

    public PhoneException(String message, XMPPError error) {
        super(message, error);    
    }


}
