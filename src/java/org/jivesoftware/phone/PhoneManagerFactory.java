/**
 * $RCSfile: PhoneManagerFactory.java,v $
 * $Revision: 1.9 $
 * $Date: 2005/06/29 17:37:29 $
 *
 * Copyright (C) 1999-2004 Jive Software. All rights reserved.
 *
 * This software is the proprietary information of Jive Software. Use is subject to license terms.
 */
package org.jivesoftware.phone;

import org.jivesoftware.phone.asterisk.AsteriskPhoneManager;
import org.jivesoftware.phone.database.DbPhoneDAO;


/**
 * Used for acquiring instances of {@link PhoneManager}
 *
 * @author Andrew Wright
 */
public final class PhoneManagerFactory {

    private static PhoneManager phoneManager;

    private PhoneManagerFactory() {
    }

    public static void init(PhoneManager phoneManager)  {
        PhoneManagerFactory.phoneManager = phoneManager;
    }


    /**
     * Returns a new instance of the phone manager
     *
     * @return new phone manager instance
     */
    public static PhoneManager getPhoneManager() {
        return phoneManager;
    }

}
