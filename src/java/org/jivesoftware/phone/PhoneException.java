/**
 * $RCSfile: PhoneException.java,v $
 * $Revision: 1.1 $
 * $Date: 2005/06/20 22:14:27 $
 *
 * Copyright (C) 1999-2004 Jive Software. All rights reserved.
 *
 * This software is the proprietary information of Jive Software. Use is subject to license terms.
 */
package org.jivesoftware.phone;

/**
 * @author Andrew Wright
 */
public class PhoneException extends Exception {

    public PhoneException() {
        super();
    }

    public PhoneException(String message) {
        super(message);
    }

    public PhoneException(String message, Throwable cause) {
        super(message, cause);
    }

    public PhoneException(Throwable cause) {
        super(cause);    
    }

}
