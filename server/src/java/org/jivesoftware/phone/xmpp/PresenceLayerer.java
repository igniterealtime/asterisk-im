/**
 * Copyright (C) 1999-2004 Jive Software. All rights reserved.
 * Copyright (C) 2006 headissue GmbH; Jens Wilke. All rights reserved.
 *
 * This software is published under the terms of the GNU Public License (GPL),
 * a copy of which is included in this distribution.
 *
 */
package org.jivesoftware.phone.xmpp;

import org.jivesoftware.util.Log;
import org.jivesoftware.wildfire.ClientSession;
import org.jivesoftware.wildfire.PresenceRouter;
import org.jivesoftware.wildfire.Session;
import org.jivesoftware.wildfire.SessionManager;
import org.jivesoftware.wildfire.XMPPServer;
import org.jivesoftware.wildfire.event.SessionEventListener;
import org.jivesoftware.wildfire.interceptor.PacketInterceptor;
import org.jivesoftware.wildfire.interceptor.PacketRejectedException;
import org.xmpp.packet.JID;
import org.xmpp.packet.Packet;
import org.xmpp.packet.Presence;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

/**
 * The idea is to have a general implementation of the concept of presence layering.
 * Rewrite of OnPhoneInterceptor and UserPresenceUtil which capsulates all presence
 * negotiations.
 *
 * @author Jens Wilke
 */
public class PresenceLayerer implements PacketInterceptor, SessionEventListener {

    XMPPServer server = XMPPServer.getInstance();
    HashMap<Session, SessionProxy> session2proxy = new HashMap<Session, SessionProxy>();
    HashMap<String, UserState> name2state = new HashMap<String, UserState>();

    /**
     * True if we don't need to do an interception. True for
     * annonymous and control packages and packages coming not from our server.
     */
    public boolean isNoInterceptionNeeded(Presence presence) {
        final JID from = presence.getFrom();
        // Only process packets sent by users on our server
        if (!server.getServerInfo().getName().equals(from.getDomain())) {
            return true;
        }
        // Ignore unavailable presences. That case is covered by #sessionDestroyed(Session)
        if (Presence.Type.unavailable.equals(presence.getType())) {
            return true;
        }
        // If the presence type is error we should just ignore it
        if (Presence.Type.error.equals(presence.getType())) {
            return true;
        }
        // We should also ignore probe presence types
        if (Presence.Type.probe.equals(presence.getType())) {
            return true;
        }
        // If this presence is directed to an individual then ignore it
        if (presence.getTo() != null) {
            return true;
        }
        final String username = from.getNode();

        // If the user is anonymous, or is a service
        return username != null;
    }

    /*
      * @see org.jivesoftware.wildfire.interceptor.PacketInterceptor#interceptPacket(org.xmpp.packet.Packet, org.jivesoftware.wildfire.Session, boolean, boolean)
      * @overwrite
      */
    public void interceptPacket(Packet packet,
                                Session session,
                                boolean incoming,
                                boolean processed) throws PacketRejectedException {
        // BTW: i dont really like this interecption interface, different methods
        // would be fine and a parameter / retrun type interface for the rejection ;jw

        // Only process incoming Presence packets before they have been processed by the server
        // For the case that a client was abruptly disconnected no unavailable presence will be
        // received. Therefore, we implement the SessionEventListener interface to react when
        // the session is destroyed
        // Log.debug("presence interceptor: "+packet);
        if (processed || !incoming || !(packet instanceof Presence)) {
            return;
        }
        Presence p = (Presence)packet;
        if (isNoInterceptionNeeded(p)) {
            // Log.debug("passed presence: "+p);
            return;
        }
        SessionProxy sp = session2proxy.get(session);
        // interception on this session?
        if (sp != null) {
            // Log.debug("intercepted: "+p);
            sp.latestPresence = p;
            throw new PacketRejectedException("Status will change after user is off the phone!");
        }
        // Log.debug("passed presence, no session: "+p);

    }

    synchronized void newInterceptSession(UserState us, ClientSession session) {
        SessionProxy sp = session2proxy.get(session);
        // there is already some interception going on!
        if (sp != null) {
            return;
        }
        sp = new SessionProxy();
        sp.user = us;
        sp.session = session;
        sp.latestPresence = session.getPresence();
        us.sessions.add(sp);
        session2proxy.put(session, sp);
    }

    /**
     * Set a different presence for the user. From now on a change in the presence
     * from the user client is intercepted until restorePresence is called for that
     * user.
     */
    public void setPresence(String user, Presence p) {
        Log.debug("Set special presence for " + user + ": " + p.toString());
        SessionManager sessionManager = server.getSessionManager();
        UserState us;
        synchronized (this) {
            us = name2state.get(user);
            if (us == null) {
                us = new UserState();
                us.username = user;
                us.phonePresence = p;
                name2state.put(user, us);
            }
        }
        PresenceRouter pr = server.getPresenceRouter();
        Collection<ClientSession> sessions = sessionManager.getSessions(user);
        for (ClientSession cs : sessions) {
            newInterceptSession(us, cs);
            // send updated presence packet
            JID fullJID = cs.getAddress();
            p.setFrom(fullJID);
            pr.route(p);
        }
    }

    public synchronized void restorePresence(String user) {
        Log.debug("Restoring special presence for " + user);
        UserState us = name2state.get(user);
        if (us == null) {
            return;
        }
        PresenceRouter pr = server.getPresenceRouter();
        List<SessionProxy> userStateSessionsCopy = new ArrayList<SessionProxy>(us.sessions);
        for (SessionProxy sp : userStateSessionsCopy) {
            sp.remove();
            Presence p = sp.latestPresence;
            pr.route(p);
        }
        // double, but does not hurt...
        name2state.remove(user);
    }

    /**
     * Restore all user presences
     */
    public synchronized void restoreCompletely() {
        for (UserState us : name2state.values()) {
            restorePresence(us.username);
        }
    }

    public void sessionCreated(Session session) {
        if (!(session instanceof ClientSession)) {
            return;
        }
        ClientSession cs = (ClientSession)session;
        UserState us = name2state.get(session.getAddress());
        if (us != null) {
            // a user that presences we intercept has created a new session!
            newInterceptSession(us, cs);
        }
    }

    public synchronized void sessionDestroyed(Session session) {
        SessionProxy sp = session2proxy.get(session);
        if (sp != null) {
            sp.remove();
        }
    }

    public void anonymousSessionCreated(Session session) {
        // we are only interested in user sessions
    }

    public void anonymousSessionDestroyed(Session session) {
        // we are only interested in user sessions
    }

    class UserState {

        List<SessionProxy> sessions = new ArrayList<SessionProxy>();
        String username;
        Presence phonePresence;

        void removeSession(SessionProxy sp) {
            sessions.remove(sp);
            if (sessions.size() == 0) {
                name2state.remove(username);
            }
        }

    }

    class SessionProxy {

        Session session;

        /**
         * latest presence we received in this session
         */
        Presence latestPresence;

        UserState user;

        void remove() {
            user.removeSession(this);
            session2proxy.remove(session);
        }

    }

}