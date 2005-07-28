/**
 * $RCSfile: ManagerConnectionPoolFactory.java,v $
 * $Revision: 1.1 $
 * $Date: 2005/06/20 23:07:47 $
 *
 * Copyright (C) 1999-2004 Jive Software. All rights reserved.
 *
 * This software is the proprietary information of Jive Software. Use is subject to license terms.
 */
package org.jivesoftware.phone.asterisk;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Used for acquiring instances of {@link ManagerConnectionPool}
 *
 * @author Andrew Wright
 */
public final class ManagerConnectionPoolFactory {

    private static ManagerConnectionPool connectionPool = null;

    private static final Log log = LogFactory.getLog(ManagerConnectionPoolFactory.class);

    private ManagerConnectionPoolFactory() {
    }


    public static synchronized void init(ManagerConfig config) {
        log.info("initializing factory");

        //if there is an instance already close it
        if(connectionPool != null) {
            try {
                connectionPool.close();
            } catch (Exception e) {
                log.error(e.getMessage(),e);
            }
        }

        //current returns only this implementation
        connectionPool = new DefaultManagerConnectionPool(config);
    }

    public static ManagerConnectionPool getManagerConnectionPool() {

        if(connectionPool == null) {
            log.error("ManagerFactory has not been initialized");
            throw new IllegalStateException("ManagerFactory has not been initialized!");
        }

        return connectionPool;
    }

    /**
     * Convenience method for closing the current connection pool
     *
     * @throws ManagerException thrown if there is problem closing the pool
     */
    public static void close() throws ManagerException {
        if(connectionPool != null) {
            connectionPool.close();
            connectionPool = null;
        }
    }

}
