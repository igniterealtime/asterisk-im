/**
 * $RCSfile: PhoneActionIQProvider.java,v $
 * $Revision: 1.4 $
 * $Date: 2005/07/02 00:22:51 $
 *
 * Copyright (C) 1999-2004 Jive Software. All rights reserved.
 *
 * This software is the proprietary information of Jive Software. Use is subject to license terms.
 */
package org.jivesoftware.phone.client.action;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.provider.IQProvider;
import org.xmlpull.v1.XmlPullParser;

/**
 * @author Andrew Wright
 */
public class PhoneActionIQProvider implements IQProvider {


    public IQ parseIQ(XmlPullParser parser) throws Exception {

        String type = parser.getAttributeValue(null, "type");


        if(PhoneActionPacket.ActionType.DIAL.name().equals(type)) {
            return new DialAction();
        }
        else if (PhoneActionPacket.ActionType.FORWARD.name().equals(type)) {
            return new ForwardAction();
        }
        else if (PhoneActionPacket.ActionType.INVITE.name().equals(type)) {
            return new InviteAction();
        }

        return null;
    }


}
