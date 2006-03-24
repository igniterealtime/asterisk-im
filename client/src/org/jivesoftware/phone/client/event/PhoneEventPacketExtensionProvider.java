/**
 * $RCSfile: PhoneEventPacketExtensionProvider.java,v $
 * $Revision: 1.6 $
 * $Date: 2005/06/25 02:09:34 $
 *
 * Copyright (C) 1999-2004 Jive Software. All rights reserved.
 *
 * This software is the proprietary information of Jive Software. Use is subject to license terms.
 */
package org.jivesoftware.phone.client.event;

import org.jivesoftware.phone.client.DialedEvent;
import org.jivesoftware.phone.client.HangUpEvent;
import org.jivesoftware.phone.client.OnPhoneEvent;
import org.jivesoftware.phone.client.RingEvent;
import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.provider.PacketExtensionProvider;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * A PacketExtension Provider that can be used to create subclasses of
 * {@link PhoneEventPacketExtension} based off packet extension information
 *
 * @author Andrew Wright
 */
public class PhoneEventPacketExtensionProvider implements PacketExtensionProvider {

    private static final Logger log =
            Logger.getLogger(PhoneEventPacketExtensionProvider.class.getName());


    public PacketExtension parseExtension(XmlPullParser parser) throws Exception {

        String type = parser.getAttributeValue(null, "type");
        String device = parser.getAttributeValue(null, "device");
        String callID = parser.getAttributeValue(null, "callID");

        // Ensure we did find a type attribute
        if (type == null) {
            log.severe("Could not find type attribute");
            throw new IllegalStateException("Could not find type attribute");
        }

        PacketExtension pe = null;

        if (PhoneEventPacketExtension.EventStatus.ON_PHONE.name().equals(type)) {
            pe = createOnPhoneEvent(parser, callID, device);
        }
        else if (PhoneEventPacketExtension.EventStatus.HANG_UP.name().equals(type)) {
            pe = createHangUpEvent(callID, device);
        }
        else if (PhoneEventPacketExtension.EventStatus.RING.name().equals(type)) {
            pe = createRingEvent(parser, callID, device);
        }
        else if (PhoneEventPacketExtension.EventStatus.DIALED.name().equals(type)) {
            pe = createDialedEvent(callID, device);
        }

        //ensure we are at the proper stopping point
        while (!isDone(parser)) {
            parser.next();
        }

        return pe;
    }

    /**
     * Returns true when we have reached the "phone-event" end tag
     */
    private boolean isDone(XmlPullParser parser) throws XmlPullParserException {

        return org.jivesoftware.phone.client.event.PhoneEventPacketExtension.ELEMENT_NAME.equals(
                parser.getName()) &&
                parser.getEventType() == XmlPullParser.END_TAG;

    }


    /**
     * Creates a new {@link OnPhoneEvent} from information from the parser
     */
    private OnPhoneEvent createOnPhoneEvent(XmlPullParser parser,
                                            String callID, String device
    )
            throws XmlPullParserException, IOException {

        String extension = null;

        while (!isDone(parser)) {

            if ("extension".equals(parser.getName()) &&
                    parser.getEventType() == XmlPullParser.START_TAG) {

                extension = parser.nextText();
            }
            else if ("callerID".equals(parser.getName()) &&
                    parser.getEventType() == XmlPullParser.START_TAG) {

                extension = parser.nextText();
            }
            else {
                parser.next(); //keep parsing until we find something useful
            }

        }

        return new OnPhoneEvent(callID, device, extension);

    }

    /**
     * Creates a new {@link OnPhoneEvent} from information from the parser
     */
    private RingEvent createRingEvent(XmlPullParser parser, String callID, String device)
            throws XmlPullParserException, IOException {

        String callerID = null;
        String callerIDName = null;

        while (!isDone(parser)) {

            if ("callerID".equals(parser.getName()) &&
                    parser.getEventType() == XmlPullParser.START_TAG) {

                callerID = parser.nextText();
            }
            else if ("callerIDName".equals(parser.getName()) &&
                    parser.getEventType() == XmlPullParser.START_TAG) {

                callerIDName = parser.nextText();

            }
            else {
                parser.next(); //keep parsing until we find something useful
            }

        }

        return new RingEvent(callID, device, callerID, callerIDName);

    }


    /**
     * Creates a new hangup event from the
     */
    private HangUpEvent createHangUpEvent(String callID, String device) {
        return new HangUpEvent(callID, device);
    }

    private DialedEvent createDialedEvent(String callID, String device) {
        return new DialedEvent(callID, device);
    }

}
