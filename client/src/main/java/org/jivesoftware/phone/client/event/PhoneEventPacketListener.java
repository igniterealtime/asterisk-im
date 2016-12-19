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

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.phone.client.OnPhoneEvent;
import org.jivesoftware.phone.client.RingEvent;

/**
 * Listens for Packets that have phone-event packet extensions and dispatches them
 * to a PhoneEventDispatcher
 *
 * @author Andrew Wright
 */
public class PhoneEventPacketListener implements PacketListener {

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


    public void processPacket(Packet packet) {

        PhoneEventPacketExtension eventPacketExtension =
                (PhoneEventPacketExtension) packet.getExtension(
                        org.jivesoftware.phone.client.event.PhoneEventPacketExtension.ELEMENT_NAME, org.jivesoftware.phone.client.event.PhoneEventPacketExtension.NAMESPACE);

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
