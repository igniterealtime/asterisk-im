/**
 * $RCSfile: CallSessionFactory.java,v $
 * $Revision: 1.2 $
 * $Date: 2005/07/01 23:56:27 $
 *
 * Copyright (C) 1999-2004 Jive Software. All rights reserved.
 *
 * This software is the proprietary information of Jive Software. Use is subject to license terms.
 */
package org.jivesoftware.phone.asterisk;


import org.jivesoftware.util.ConcurrentHashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Used for acquiring call session objects.
 *
 * @author Andrew Wright
 */
public class CallSessionFactory {
    private static final Logger Log = LoggerFactory.getLogger(CallSessionFactory.class);

    private static final CallSessionFactory INSTANCE = new CallSessionFactory();

    private final Set<CallSessionListener> callSessionListeners;

    /**
     * key => asterisk unique id, value => CallSession for that id.
     */
    private final Map<String, CallSession> sessionMap;

    /**
     * key => username, value => all call sessions for that user.
     */
    private final Map<String, Collection<CallSession>> userSessionMap;

    private CallSessionFactory() {
        callSessionListeners = new ConcurrentHashSet<CallSessionListener>();
        sessionMap = new ConcurrentHashMap<String, CallSession>();
        userSessionMap = new ConcurrentHashMap<String, Collection<CallSession>>();
    }

    /**
     * Acquire a call session by its id.
     *
     * @param serverID id of the phone server.
     * @param id       the call session id
     * @param username user who the session belongs to.
     * @return the created call session
     * @throws IllegalArgumentException if there is already a session with the given id.
     */
    public synchronized CallSession createCallSession(long serverID, String id, String username)
    {
        CallSession session = sessionMap.get(id);

        if (session == null) {
            session = new CallSession(serverID, id, username);
            sessionMap.put(id, session);
        }
        else {
            throw new IllegalArgumentException("Call session already exists");
        }

        Collection<CallSession> sessions = userSessionMap.get(username);
        if (sessions == null) {
            sessions = Collections.synchronizedList(new ArrayList<CallSession>());
            userSessionMap.put(username, sessions);
        }

        if (!sessions.contains(session)) {
            sessions.add(session);
        }

        fireCallSessionCreated(session);
        return session;
    }

    public synchronized void modifyCallSession(CallSession session,
                                                      CallSession.Status currentStatus) {
        CallSession.Status oldStatus;
        oldStatus = session.getStatus();
        session.setStatus(currentStatus);
        fireCallSessionModified(session, oldStatus);
    }

    private void fireCallSessionModified(CallSession session, CallSession.Status oldStatus) {
        for (CallSessionListener listener : callSessionListeners) {
            listener.callSessionModified(session, oldStatus);
        }
    }

    private void fireCallSessionCreated(CallSession session) {
        for(CallSessionListener listener : callSessionListeners) {
            listener.callSessionCreated(session);
        }
    }

    public CallSession getCallSession(String id) {
        return sessionMap.get(id);
    }

    /**
     * destroys the specific call session.
     *
     * @param id id of the session to destory
     * @return the session that has been destoryed or <code>null</code>
     *         if there is no session with the given id.
     */
    public synchronized CallSession destroyPhoneSession(String id) {
        final CallSession callSession;

        callSession = sessionMap.remove(id);
        if(callSession == null) {
            Log.debug("Cannot destroy non-existent CallSession with id: " + id);
            return null;
        } else {
            Log.debug("Destoying CallSession " + callSession);
        }

        Collection<CallSession> sessions = userSessionMap.get(callSession.getUsername());
        // should never be null
        sessions.remove(callSession);

        // Remove the map if there are nolonger any callSession for this user
        if (sessions.size() == 0) {
            userSessionMap.remove(callSession.getUsername());
        }
        fireCallSessionDestroyed(callSession);
        return callSession;
    }

    private void fireCallSessionDestroyed(CallSession session) {
        for(CallSessionListener listener : callSessionListeners) {
            listener.callSessionDestroyed(session);
        }
    }

    /**
     * Destroys the CallSession of the specified user with the given channel.
     *
     * @param channel  the channel of the CallSession.
     * @param username the name of the user that has the CallSession.
     * @return the removed CallSession or <tt>null</tt> if nothing was removed.
     */
    public synchronized CallSession destroyPhoneSession(String channel, String username) {
        Collection<CallSession> sessions = userSessionMap.get(username);
        if (sessions == null) {
            return null;
        }
        for (CallSession session : sessions) {
            if (channel.equals(session.getChannelName())) {
                return destroyPhoneSession(session.getChannelId());
            }
        }
        return null;
    }

    /**
     * Returns all sessions for a specific user.
     *
     * @param username there user who's call sessions to grab
     * @return collection of call sessions that belong to a specific user
     */
    public Collection<CallSession> getUserCallSessions(String username) {
        Collection<CallSession> sessions = userSessionMap.get(username);
        if (sessions == null) {
            sessions = Collections.emptyList();
        }

        return Collections.unmodifiableCollection(sessions);
    }

    public int getUserCallSessionsCount(String username) {
        return getUserCallSessions(username).size();
    }

    /**
     * Used to acquire an instance of the call session factory.
     *
     * @return instance of the call session factory
     */
    public static CallSessionFactory getInstance() {
        return INSTANCE;
    }

    public void addCallSessionListener(CallSessionListener listener) {
        if(listener == null) {
            return;
        }
        callSessionListeners.add(listener);
    }

    public void removeCallSessionListener(CallSessionListener listener) {
        if(listener == null) {
            return;
        }
        callSessionListeners.remove(listener);
    }
}
