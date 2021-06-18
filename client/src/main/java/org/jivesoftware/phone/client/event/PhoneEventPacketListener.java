/**
 * $RCSfile: PhoneEventPacketListener.java,v $
 * $Revision: 1.3 $
 * $Date: 2005/06/25 00:05:40 $
 *
 * Copyright (C) 1999-2004 Jive Software. All rights reserved.
 *
 * This software is the proprietary information of Jive Software. Use is subject to license terms.
 */
package org.jivesoftware.phone.client.event;

import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.phone.client.OnPhoneEvent;
import org.jivesoftware.phone.client.RingEvent;
import org.jivesoftware.smack.packet.Stanza;

/**
 * Listens for Packets that have phone-event packet extensions and dispatches them
 * to a PhoneEventDispatcher
 *
 * @author Andrew Wright
 */
public class PhoneEventPacketListener implements StanzaListener{

    private PhoneEventDispatcher eventDispatcher;

    /**
     * Creates a new PhoneEventPacketListener that will use the given dispatcher to
     * dispatch phone events.
     *
     * @param eventDispatcher Dispatcher used to process phone events
     */
    public PhoneEventPacketListener(PhoneEventDispatcher eventDispatcher) {
        this.eventDispatcher = eventDispatcher;
    }


    public void processPacket(Stanza packet) {

        PhoneEventExtensionElement eventPacketExtension = packet.getExtension(
                        PhoneEventExtensionElement.ELEMENT_NAME, PhoneEventExtensionElement.NAMESPACE);

        if(eventPacketExtension != null) {

            // Initialize the call if this is an answer event
            if(eventPacketExtension instanceof OnPhoneEvent ) {
                ((OnPhoneEvent) eventPacketExtension).initCall(eventDispatcher);
            } else if (eventPacketExtension instanceof RingEvent) {
                ((RingEvent) eventPacketExtension).initCall(eventDispatcher);
            }

            eventDispatcher.dispatchEvent(eventPacketExtension);
        }

    }

}
