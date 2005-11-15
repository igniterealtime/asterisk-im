/**
 * $RCSfile: CallSessionFactory.java,v $
 * $Revision: 1.2 $
 * $Date: 2005/07/01 23:56:27 $
 *
 * Copyright (C) 1999-2004 Jive Software. All rights reserved.
 *
 * This software is the proprietary information of Jive Software. Use is subject to license terms.
 */
package org.jivesoftware.phone;


import org.jivesoftware.util.Log;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Used for acquiring call session objects.
 *
 * @author Andrew Wright
 */
public class CallSessionFactory {

    private Map<String, CallSession> sessionMap = new ConcurrentHashMap<String, CallSession>();
    private Map<String, Collection<CallSession>> userSessionMap = new ConcurrentHashMap<String, Collection<CallSession>>();

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
    public synchronized CallSession getCallSession(String id, String username) {

        CallSession session = sessionMap.get(id);

        if (session == null) {
            session = new CallSession(id, username);
            sessionMap.put(id, session);
        }

        Collection<CallSession> sessions = userSessionMap.get(username);
        if (sessions == null) {
            sessions = Collections.synchronizedList(new ArrayList<CallSession>());
            userSessionMap.put(username, sessions);
        }

        if(!sessions.contains(session)) {
            sessions.add(session);
        }

        return session;
    }

    /**
     * destroys the specific call session
     *
     * @param id id of the session to destory
     */
    public synchronized CallSession destroyPhoneSession(String id) {
        CallSession session = sessionMap.remove(id);

        if (session != null) {
            try {
                Collection<CallSession> sessions = userSessionMap.get(session.getUsername());
                // should never be null
                sessions.remove(session);

                // Remove the map if there are nolonger any session for this user
                if (sessions.size() == 0) {
                    userSessionMap.remove(session.getUsername());
                }
            }
            catch (RuntimeException e) {
                Log.error("CallSessionFactory: Unexpected RuntimeException occurred ", e);
            }
        }
        else {
            if (Log.isDebugEnabled()) {
                if (!id.startsWith("SIP/")) {
                    Log.debug("Cannot destroy non-existent CallSession with id: " + id);
                }
            }
        }
        return session;
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

        return sessions;
    }

    /**
     * Used to acquire an instance of the call session factory
     *
     * @return instance of the call session factory
     */
    public static CallSessionFactory getCallSessionFactory() {
        return INSTANCE;
    }

}
