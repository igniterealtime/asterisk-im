/**
 * $RCSfile: PhoneClientTest.java,v $
 * $Revision: 1.3 $
 * $Date: 2005/06/29 23:33:30 $
 *
 * Copyright (C) 1999-2004 Jive Software. All rights reserved.
 *
 * This software is the proprietary information of Jive Software. Use is subject to license terms.
 */
package org.jivesoftware.phone.client;

import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.phone.client.event.PhoneEventPacketExtension;
import org.jivesoftware.phone.client.event.PhoneEventPacketExtensionProvider;
import org.jivesoftware.phone.client.action.PhoneActionPacket;
import org.jivesoftware.phone.client.action.PhoneActionIQProvider;
import junit.framework.TestCase;

import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * @author Andrew Wright
 */
public class PhoneClientTest extends TestCase {

    private static final Logger log = Logger.getLogger(PhoneClientTest.class.getName());

    static {

        // XMPPConnection.DEBUG_ENABLED = true;

        try {

            ProviderManager.getInstance().addExtensionProvider("phone-event",
                    PhoneEventPacketExtension.NAMESPACE,
                    new PhoneEventPacketExtensionProvider());


            ProviderManager.getInstance().addIQProvider("phone-action",
                    PhoneActionPacket.NAMESPACE,
                    new PhoneActionIQProvider());
        } catch (Exception e) {
            log.log(Level.SEVERE, e.getMessage(), e);
            throw new ExceptionInInitializerError(e);
        }

    }

    XMPPConnection conn = null;
    PhoneClient phoneClient = null;

    protected void setUp() throws Exception {

        String host = System.getProperty("host");
        assertNotNull(host);
        conn = new XMPPConnection(host);

        phoneClient = new PhoneClient(conn);

        phoneClient.addEventListener(new PhoneEventListener() {
            public void handle(PhoneEvent event) {
                log.info("received event of type "+event);
            }
        });

    }


    public void testDial() throws Exception{

        String exten = System.getProperty("exten");
        assertNotNull(exten);

        phoneClient.dialByExtension(exten);

    }




}
