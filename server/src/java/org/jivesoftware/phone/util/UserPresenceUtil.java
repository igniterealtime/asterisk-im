/**
 * Copyright (C) 1999-2005 Jive Software. All rights reserved.
 *
 * This software is the proprietary information of Jive Software. Use is subject to license terms.
 */
package org.jivesoftware.phone.util;

import org.xmpp.packet.Presence;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author Andrew Wright
 */
public class UserPresenceUtil {

    /**
     * Used to store old presence objects before a person goes on the phone
     */
    private static Map<String, Collection<Presence>> previousPresenceMap =
            new ConcurrentHashMap<String, Collection<Presence>>();


    public static Collection<Presence> getPresences(String username) {
        return previousPresenceMap.get(username);
    }


    public static void setPresences(String username, Collection<Presence> presences) {
        previousPresenceMap.put(username, new CopyOnWriteArrayList<Presence>(presences));
    }

    public static Collection<Presence> removePresences(String username) {
        return previousPresenceMap.remove(username);
    }

    public static Collection<String> getUsernames() {
        return previousPresenceMap.keySet();
    }

}
