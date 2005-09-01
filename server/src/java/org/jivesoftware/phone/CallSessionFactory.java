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


import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Used for acquiring call session objects.
 *
 * @author Andrew Wright
 */
public class CallSessionFactory {

    private Map<String,CallSession> sessionMap = new ConcurrentHashMap<String,CallSession>();

    private static final CallSessionFactory INSTANCE = new CallSessionFactory();

    private CallSessionFactory() {
    }

    /**
     * Acquire a call session by its id
     *
     * @param id the call session id
     * @return the call session object with a specific id, else null
     */
    public CallSession getPhoneSession(String id) {

        CallSession session = sessionMap.get(id);

        if(session == null) {
            session = new CallSession(id);
            sessionMap.put(id, session);
        }

        return session;
    }

    /**
     * destroys the specific call session
     *
     * @param id id of the session to destory
     */
    public CallSession destroyPhoneSession(String id) {
        return sessionMap.remove(id);
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
