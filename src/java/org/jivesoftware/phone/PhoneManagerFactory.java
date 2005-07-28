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

import org.jivesoftware.phone.database.HibernatePhoneDAO;
import org.jivesoftware.phone.asterisk.AsteriskPhoneManager;

import java.util.logging.Logger;
import java.util.logging.Level;


/**
 * Used for acquiring instances of {@link PhoneManager}
 *
 * @author Andrew Wright
 */
public final class PhoneManagerFactory {

    private static final Logger log = Logger.getLogger(PhoneManagerFactory.class.getName());

    private PhoneManagerFactory() {
    }


    /**
     * Returns a new instance of the phone manager
     *
     * @return new phone manager instance
     */
    public static final PhoneManager getPhoneManager() {
        return new AsteriskPhoneManager(new HibernatePhoneDAO());
    }

    /**
     * Utility method for closing managers that checks to see if the manager is null first
     *
     * @param phoneManager phone manager to close
     */
    public static final void close(PhoneManager phoneManager) {
        try {
            if(phoneManager != null) {

                phoneManager.close();

            }
        }
        catch (RuntimeException e) {
            log.log(Level.SEVERE, e.getMessage(), e);
            throw e;
        }

    }


}
