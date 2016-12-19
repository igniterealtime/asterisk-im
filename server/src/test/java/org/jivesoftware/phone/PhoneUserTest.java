/**
 * $RCSfile: PhoneUserTest.java,v $
 * $Revision: 1.2 $
 * $Date: 2005/06/24 19:32:50 $
 *
 * Copyright (C) 1999-2004 Jive Software. All rights reserved.
 *
 * This software is the proprietary information of Jive Software. Use is subject to license terms.
 */
package org.jivesoftware.phone;

import junit.framework.TestCase;
import org.jivesoftware.database.JiveID;

/**
 * @author Andrew Wright
 */
public class PhoneUserTest extends TestCase {


    public void testGetJiveID() {

        PhoneUser jid = new PhoneUser();

        JiveID id = jid.getClass().getAnnotation(JiveID.class);

        assertNotNull(id);

        assertEquals(id.value(), 100);


    }

}
