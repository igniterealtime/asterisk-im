/**
 * Copyright (C) 1999-2004 Jive Software. All rights reserved.
 * Copyright (C) 2006 headissue GmbH; Jens Wilke. All rights reserved.
 *
 * This software is published under the terms of the GNU Public License (GPL),
 * a copy of which is included in this distribution.
 *
 */
package org.jivesoftware.phone.xmpp;

import org.jivesoftware.phone.asterisk.CallSession;
import org.jivesoftware.phone.asterisk.CallSessionFactory;
import org.jivesoftware.phone.asterisk.CallSessionListener;
import org.jivesoftware.phone.queue.QueueManager;
import org.jivesoftware.phone.xmpp.element.PhoneStatus;
import org.jivesoftware.util.Log;
import org.jivesoftware.openfire.PresenceRouter;
import org.jivesoftware.openfire.SessionManager;
import org.jivesoftware.openfire.event.SessionEventListener;
import org.jivesoftware.openfire.interceptor.PacketInterceptor;
import org.jivesoftware.openfire.interceptor.PacketRejectedException;
import org.jivesoftware.openfire.session.ClientSession;
import org.jivesoftware.openfire.session.Session;
import org.xmpp.packet.JID;
import org.xmpp.packet.Packet;
import org.xmpp.packet.Presence;

import java.util.*;

/**
 * The idea is to have a general implementation of the concept of presence layering.
 * Rewrite of OnPhoneInterceptor and UserPresenceUtil which capsulates all presence
 * negotiations.
 *
 * @author Jens Wilke
 */
public class PresenceLayerer implements PacketInterceptor, SessionEventListener,
        CallSessionListener
{

    private Map<Session, SessionProxy> session2proxy = new HashMap<Session, SessionProxy>();
    private Map<String, UserState> name2state = new HashMap<String, UserState>();
    private boolean isShutdown = false;
    private QueueManager queueManager;
    private SessionManager sessionManager;
    private PresenceRouter presenceRouter;
    private String serverName;
    private CallSessionFactory callSessionFactory;

    public PresenceLayerer(String serverName, SessionManager sessionManager,
                           QueueManager queueManager, PresenceRouter presenceRouter,
                           CallSessionFactory callSessionFactory) {
        this.queueManager = queueManager;
        this.sessionManager = sessionManager;
        this.presenceRouter = presenceRouter;
        this.serverName = serverName;
        this.callSessionFactory = callSessionFactory;
    }

    /**
     * True if we don't need to do an interception. True for
     * annonymous and control packages and packages coming not from our server.
     */
    public boolean isNoInterceptionNeeded(Presence presence) {
        final JID from = presence.getFrom();
        // Only process packets sent by users on our server
        if (!serverName.equals(from.getDomain())) {
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
        return username == null || "".equals(username);
    }

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

        final Presence presence = (Presence)packet;

        if (presence instanceof PhonePresence) {
            Log.debug("Not interception our own presence updates");
            return;
        }

        try {
            if (session instanceof ClientSession) {
                queueManager.updateQueueStatus((ClientSession) session, presence);
            }
        }
        catch (Exception ex) {
            Log.error("error checking users queue presence", ex);
        }
        if (isNoInterceptionNeeded(presence)) {
            Log.debug("passed presence: "+presence);
            return;
        }
        // we need to sync so that the user's presence won't get out of sync
        updateSessionProxy(session, presence);

        Log.debug("passed presence, no session: "+presence);
    }

    private synchronized void updateSessionProxy(Session session, Presence presence)
            throws PacketRejectedException {
        SessionProxy sessionProxy = session2proxy.get(session);
        // interception on this session?
        // TODO fix updating presence when on the phone
        if (sessionProxy != null) {
            Log.debug("intercepted: "+presence);
            sessionProxy.latestPresence = presence;
            throw new PacketRejectedException("Status will change after user is off the phone!");
        }
    }

    private synchronized void createInterceptSession(UserState userState,
                                                     ClientSession clientSession, Presence presence)
    {
        SessionProxy sessionProxy = session2proxy.get(clientSession);
        // there is no interception going on!
        if (sessionProxy == null) {
            sessionProxy = new SessionProxy(clientSession, userState);
            sessionProxy.latestPresence = clientSession.getPresence();
            userState.sessions.add(sessionProxy);
            session2proxy.put(clientSession, sessionProxy);
        }
        // a phone presence is passed in if one is currently available to this user.
        if(presence != null) {
            routePresence(clientSession, presence);
        }
    }

    private void routePresence(ClientSession clientSession, Presence presence) {
        // send updated presence packet
        JID fullJID = clientSession.getAddress();
        presence.setFrom(fullJID);
        presenceRouter.route(presence);
    }

    /**
     * Set a different presence for the user. From now on a change in the presence
     * from the user client is intercepted until restorePresence is called for that
     * user.
     */
    public void setPresence(String username, Presence presence) {
        Log.debug("Set special presence for " + username + ": " + presence.toString());
        UserState userState;
        synchronized (this) {
            if(isShutdown) {
                return;
            }
            userState = name2state.get(username);
            if (userState == null) {
                userState = new UserState();
                name2state.put(username, userState);
            }
            userState.phonePresence = presence;
        }

        Collection<ClientSession> sessions = sessionManager.getSessions(username);
        for (ClientSession clientSession : sessions) {
            createInterceptSession(userState, clientSession, presence);
        }
    }

    public synchronized void restorePresence(String username) {
        Log.debug("Restoring special presence for " + username);
        UserState userState = name2state.remove(username);
        if (userState == null) {
            return;
        }
        List<SessionProxy> userStateSessionsCopy = new ArrayList<SessionProxy>(userState.sessions);
        for (SessionProxy sessionProxy : userStateSessionsCopy) {
            userState.removeSession(sessionProxy);
            session2proxy.remove(sessionProxy.session);
            presenceRouter.route(sessionProxy.latestPresence);
        }
    }

    /**
     * Restores all user presences.
     */
    public synchronized void shutdown() {
        if(isShutdown) {
            return;
        }
        isShutdown = true;
        // Make a copy of the usernames
        List<String> usernames = new ArrayList<String>(name2state.keySet());
        for (String username : usernames) {
            restorePresence(username);
        }
    }

    public synchronized void sessionCreated(Session session) {
        if (!(session instanceof ClientSession) || isShutdown) {
            return;
        }
        // TODO check if this is correct (srt)
        UserState us = name2state.get(session.getAddress().getNode());
        if (us != null) {
            // a user whose presences we intercept has created a new session!
            createInterceptSession(us, (ClientSession)session, us.phonePresence);
        }
    }

    public synchronized void sessionDestroyed(Session session) {
        SessionProxy sp = session2proxy.remove(session);
        if (sp != null) {
            sp.user.removeSession(sp);
        }
    }

    public void anonymousSessionCreated(Session session) {
        // we are only interested in user sessions
    }

    public void anonymousSessionDestroyed(Session session) {
        // we are only interested in user sessions
    }

    public synchronized void callSessionCreated(CallSession session) {
        if(session.getStatus() == CallSession.Status.onphone) {
            setPresence(session.getUsername(), createOnPhonePresence("On the phone"));
        }
    }

    public synchronized void callSessionDestroyed(CallSession session) {
        final String username;

        username = session.getUsername();

        Log.debug("CallSession destroyed for user '" + username + "'");
        if (callSessionFactory.getUserCallSessionsCount(username) <= 0) {
            Log.debug("Restoring presence for user '" + username + "'");
            restorePresence(username);
        }
        else
        {
            Log.debug("Not restoring presence for user '" + username + "', " + callSessionFactory.getUserCallSessionsCount(session.getUsername()) + " calls left");
        }
    }

    public synchronized void callSessionModified(CallSession session,
                                                 CallSession.Status oldStatus)
    {
        if(session.getStatus() == CallSession.Status.onphone) {
            setPresence(session.getUsername(), createOnPhonePresence("On the phone"));
        }
    }
    

    private Presence createOnPhonePresence(String status) {
        Presence presence = new PhonePresence();
        presence.setShow(Presence.Show.away);
        presence.setStatus(status);

        PhoneStatus phoneStatus = new PhoneStatus(PhoneStatus.Status.ON_PHONE);
        presence.getElement().add(phoneStatus);
        return presence;
    }

    private class UserState {
        private List<SessionProxy> sessions = new ArrayList<SessionProxy>();
        private Presence phonePresence;

        void removeSession(SessionProxy sp) {
            sessions.remove(sp);
        }

    }

    private class SessionProxy {

        final Session session;

        /**
         * latest presence we received in this session
         */
        Presence latestPresence;

        final UserState user;

        SessionProxy(Session session, UserState user) {
            this.session = session;
            this.user = user;
        }

        @Override
        public boolean equals(Object obj) {
            if(obj instanceof SessionProxy) {
                return ((SessionProxy)obj).session == session;
            }
            return false;
        }
    }
}