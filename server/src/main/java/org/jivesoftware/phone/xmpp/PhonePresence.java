package org.jivesoftware.phone.xmpp;

import org.xmpp.packet.Presence;
import org.dom4j.Element;

/**
 *
 */
public class PhonePresence extends Presence
{
    public PhonePresence()
    {
    }

    public PhonePresence(Presence presence)
    {
        super(presence.getElement());
    }
}
