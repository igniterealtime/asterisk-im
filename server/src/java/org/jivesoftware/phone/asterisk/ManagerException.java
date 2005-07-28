/**
 * $RCSfile: ManagerException.java,v $
 * $Revision: 1.1 $
 * $Date: 2005/06/20 23:07:47 $
 *
 * Copyright (C) 1999-2004 Jive Software. All rights reserved.
 *
 * This software is the proprietary information of Jive Software. Use is subject to license terms.
 */
package org.jivesoftware.phone.asterisk;

/**
 * Generic exception thrown for various reasons by the Manager api
 *
 * @author Andrew Wright
 */
public class ManagerException extends Exception {

    public ManagerException() {
        super();
    }

    public ManagerException(String s) {
        super(s);
    }

    public ManagerException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public ManagerException(Throwable throwable) {
        super(throwable);
    }
}
