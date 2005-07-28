/**
 * $RCSfile: DefaultManagerConnectionPool.java,v $
 * $Revision: 1.2 $
 * $Date: 2005/06/30 19:11:43 $
 *
 * Copyright (C) 1999-2004 Jive Software. All rights reserved.
 *
 * This software is the proprietary information of Jive Software. Use is subject to license terms.
 */
package org.jivesoftware.phone.asterisk;

import net.sf.asterisk.manager.ManagerConnection;
import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.PoolableObjectFactory;
import org.apache.commons.pool.impl.GenericObjectPool;

import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Provide connection pooling for connections for asterisk manager connections
 *
 * @author Andrew Wright
 */
public class DefaultManagerConnectionPool implements ManagerConnectionPool {

    private static final Logger log = Logger.getLogger(DefaultManagerConnectionPool.class.getName());

    private ObjectPool objectPool; //underlying object pool


    public static class ManagerObjectPoolFactory implements PoolableObjectFactory {

        private ManagerConfig config;
        private DefaultManagerConnectionPool pool;

        public ManagerObjectPoolFactory(ManagerConfig config, DefaultManagerConnectionPool pool) {
            this.config = config;
            this.pool = pool;
        }

        public Object makeObject() throws Exception {
            PooledManagerConnection conn = new PooledManagerConnection(config,pool);
            conn.login();
            return conn;
        }

        public void destroyObject(Object o) throws Exception {
            PooledManagerConnection conn = (PooledManagerConnection) o;
            conn.close();
        }

        public boolean validateObject(Object o) {
            PooledManagerConnection conn = (PooledManagerConnection) o;
            return conn.isConnected();
        }

        public void activateObject(Object o) throws Exception {
            //do nothing
        }

        public void passivateObject(Object o) throws Exception {
            //do nothing
        }
    }


    public DefaultManagerConnectionPool(ManagerConfig config) {

        PoolableObjectFactory factory = new ManagerObjectPoolFactory(config, this);
        this.objectPool = new GenericObjectPool(factory, config.getMaxPoolSize());

    }


    public synchronized ManagerConnection getConnection() throws ManagerException {
        try {
            return (ManagerConnection) objectPool.borrowObject();

        }
        catch (Exception e) {
            log.log(Level.SEVERE, e.getMessage(), e);
            throw new ManagerException(e);
        }
    }

    public void close() throws ManagerException {
        try {
            objectPool.clear();
            objectPool.close();
            objectPool = null;
        } catch (Exception e) {
            throw new ManagerException(e);
        }
    }

    void release(ManagerConnection conn) {
        try {
            objectPool.returnObject(conn);
        }
        catch (Exception e) {
            log.log(Level.SEVERE, e.getMessage(), e);
            throw new RuntimeException(e); //todo probably make a subclass
        }
    }

}
