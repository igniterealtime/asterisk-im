/**
 * Copyright (C) 1999-2005 Jive Software. All rights reserved.
 *
 * This software is the proprietary information of Jive Software. Use is subject to license terms.
 */
package org.jivesoftware.phone.util;

import org.xmpp.packet.Presence;
import org.xmpp.packet.JID;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Util for keeping track of presences for users
 *
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


    /**
     * Sets a collection of presences for specific username
     *
     * @param username username for the presences
     * @param presences collection of presences for the user
     */
    public static void setPresences(String username, Collection<Presence> presences) {
        previousPresenceMap.put(username, new CopyOnWriteArrayList<Presence>(presences));
    }

    /**
     * Removes all stored presences of the specified user. This means that if the user was
     * connected from many clients then any stored presence will be removed.
     *
     * @param username the username of the user (i.e. JID's node)
     * @return the previously stored presences or <tt>null</tt> if no presence was being stored.
     */
    public static Collection<Presence> removePresences(String username) {
        return previousPresenceMap.remove(username);
    }

    /**
     * Removes stored presences of the user that was connected from a specific client.
     * The received JID is a full JID so we can identify the presences to remove.
     *
     * @param userFullJID JID of the user whose presences are not going to be stored anymore.
     */
    public static void removePresences(JID userFullJID) {
        Collection<Presence> presences = previousPresenceMap.get(userFullJID.getNode());
        if (presences != null) {
            for (Presence presence : presences) {
                if (userFullJID.equals(presence.getFrom())) {
                    presences.remove(presence);
                }
            }
        }
    }

    /**
     * Returns the names of all the users whos presences we are holding
     *
     * @return the names of all the users whos presences we are holding
     */
    public static Collection<String> getUsernames() {
        return previousPresenceMap.keySet();
    }

}
