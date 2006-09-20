/**
 * $RCSfile: DbPhoneDAOTest.java,v $
 * $Revision: 1.8 $
 * $Date: 2005/06/24 19:32:50 $
 *
 * Copyright (C) 1999-2004 Jive Software. All rights reserved.
 *
 * This software is the proprietary information of Jive Software. Use is subject to license terms.
 */
package org.jivesoftware.phone.database;

import junit.framework.TestCase;
import org.jivesoftware.phone.PhoneDevice;
import org.jivesoftware.phone.PhoneUser;
import org.jivesoftware.util.JiveGlobals;

import java.util.Collection;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Andrew Wright
 */
public class DbPhoneDAOTest extends TestCase {

    static {
        JiveGlobals.setConfigName("wildfire.xml");
        JiveGlobals.getPropertyNames(); // just called to intialize jive globals
    }

    public void testCRUD() throws Exception {

        Logger log = Logger.getLogger("org.hibernate.SQL");
        log.setLevel(Level.ALL);

        ConsoleHandler h = new ConsoleHandler();
        h.setLevel(Level.ALL);
        log.addHandler(h);

        PhoneDAO phoneDAO = new DbPhoneDAO();


        PhoneUser phoneJID = new PhoneUser("andrew");
        PhoneDevice device = new PhoneDevice("SIP/1231");
        device.setPrimary(true);

        assertTrue(phoneJID.getID() > 0);

        PhoneUser phoneJID2 = phoneDAO.getPhoneUserByID(phoneJID.getID());
        assertNotNull(phoneJID2);
        assertEquals(phoneJID, phoneJID2);

        PhoneDevice primary = phoneDAO.getPrimaryDevice(phoneJID.getID());
        assertNotNull(primary);


        Collection<PhoneUser> phones = phoneDAO.getPhoneUsers();
        for (PhoneUser pjid : phones) {
            assertNotNull(pjid);
        }

        phoneJID2 = phoneDAO.getByUsername("andrew");
        assertNotNull(phoneJID);
        assertEquals(phoneJID, phoneJID2);


        phoneDAO.remove(phoneJID);

        phoneJID = phoneDAO.getPhoneUserByID(phoneJID.getID());

        assertNull(phoneJID);


    }


}
