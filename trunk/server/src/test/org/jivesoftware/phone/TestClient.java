/**
 * $RCSfile: TestClient.java,v $
 * $Revision: 1.8 $
 * $Date: 2005/06/22 00:02:45 $
 *
 * Copyright (C) 1999-2004 Jive Software. All rights reserved.
 *
 * This software is the proprietary information of Jive Software. Use is subject to license terms.
 */
package org.jivesoftware.phone;

import org.jivesoftware.phone.util.PhoneConstants;
import junit.framework.TestCase;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Message;
import org.xmpp.packet.Packet;

/**
 * @author Andrew Wright
 */
public class TestClient extends TestCase {

    static {
        //XMPPConnection.DEBUG_ENABLED = true;
    }

    private XMPPConnection conn = null;

    protected void setUp() throws Exception {
        super.setUp();

        conn = new XMPPConnection("cronus.pdx-int.jivesoftware.com");
        conn.login("andrew", "test", "junit");
    }

    protected void tearDown() throws Exception {
        super.tearDown();    //To change body of overridden methods use File | Settings | File Templates.
        conn.close();
    }


    public void testDial() throws Exception {

        // look at smack and messenger debugger to figure out why this isn't working

        IQ iq = new IQ() {
            public String getChildElementXML() {
                return "<phone-action xmlns=\""+ PhoneConstants.NAMESPACE + "\"  type=\"DIAL\">" +
                            "<extension>6141</extension>" +
                        "</phone-action>";
            }
        };

        iq.setTo("phone.cronus.pdx-int.jivesoftware.com");

        conn.sendPacket(iq);

        Thread.sleep(500);
    }



    public void testSend() throws Exception {

        /*
        Message msg = new Message();
        msg.setTo("andrew@cronus.pdx-int.jivesoftware.com");
        msg.setBody("hi how are you");
        msg.setType(Message.Type.NORMAL);
        msg.setSubject("hi");
        */

        conn.createChat("andrew@pdx-int.jivesoftware.com")
            .sendMessage("Howdy!");

        //conn.sendPacket(msg);
    }




}
