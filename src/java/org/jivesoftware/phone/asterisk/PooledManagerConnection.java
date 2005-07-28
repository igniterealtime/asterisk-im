/**
 * $RCSfile: PooledManagerConnection.java,v $
 * $Revision: 1.1 $
 * $Date: 2005/06/20 23:07:47 $
 *
 * Copyright (C) 1999-2004 Jive Software. All rights reserved.
 *
 * This software is the proprietary information of Jive Software. Use is subject to license terms.
 */
package org.jivesoftware.phone.asterisk;

import net.sf.asterisk.manager.DefaultManagerConnection;
import net.sf.asterisk.manager.TimeoutException;

import java.io.IOException;

/**
 * @author Andrew Wright
 */
public class PooledManagerConnection extends DefaultManagerConnection {


    DefaultManagerConnectionPool pool;


    public PooledManagerConnection(ManagerConfig config, DefaultManagerConnectionPool pool) {
        super(config.getServer(), config.getPort(), config.getUsername(), config.getPassword());
        this.pool = pool;
    }


    /**
     * Does not logoff, just releases back to the pool
     *
     * @throws IOException
     * @throws TimeoutException
     */
    @Override
    public void logoff() throws IOException, TimeoutException {
        pool.release(this);
    }

    public void close() throws IOException, TimeoutException {
        super.logoff();
    }


}
