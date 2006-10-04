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


import org.jivesoftware.util.Log;
import org.jivesoftware.util.ConcurrentHashSet;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Used for acquiring call session objects.
 *
 * @author Andrew Wright
 */
public class CallSessionFactory {

    private Set<CallSessionListener> callSessionListeners
            = new ConcurrentHashSet<CallSessionListener>();

    /**
     * key => asterisk unique id, value => CallSession for that id
     */
    private Map<String, CallSession> sessionMap = new ConcurrentHashMap<String, CallSession>();

    /**
     * key => username, value => all call sessions for that user
     */
    private Map<String, Collection<CallSession>> userSessionMap
            = new ConcurrentHashMap<String, Collection<CallSession>>();

    private static final CallSessionFactory INSTANCE = new CallSessionFactory();

    private CallSessionFactory() {
    }

    /**
     * Acquire a call session by its id
     *
     * @param id       the call session id
     * @param username user who the session belongs too.
     * @return the call session object with a specific id, else null
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
     * destroys the specific call session
     *
     * @param id id of the session to destory
     */
    public synchronized CallSession destroyPhoneSession(String id) {
        CallSession session = sessionMap.remove(id);
        if(session == null) {
            if (Log.isDebugEnabled() && !id.startsWith("SIP/")) {
                Log.debug("Cannot destroy non-existent CallSession with id: " + id);
            }
            return null;
        }

        Collection<CallSession> sessions = userSessionMap.get(session.getUsername());
        // should never be null
        sessions.remove(session);

        // Remove the map if there are nolonger any session for this user
        if (sessions.size() == 0) {
            userSessionMap.remove(session.getUsername());
        }
        fireCallSessionDestroyed(session);
        return session;
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
            if (channel.equals(session.getChannel())) {
                return destroyPhoneSession(session.getId());
            }
        }
        return null;
    }

    /**
     * Returns all sessions for a specific user
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
     * Used to acquire an instance of the call session factory
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
