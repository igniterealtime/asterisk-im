/**
 * $RCSfile: JiveConnectionProvider.java,v $
 * $Revision: 1.3 $
 * $Date: 2005/06/20 22:14:27 $
 *
 * Copyright (C) 1999-2004 Jive Software. All rights reserved.
 *
 * This software is the proprietary information of Jive Software. Use is subject to license terms.
 */
package org.jivesoftware.phone.database;

import org.hibernate.connection.ConnectionProvider;
import org.hibernate.HibernateException;
import org.jivesoftware.database.DbConnectionManager;

import java.util.Properties;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Connection provider to use with hibernate that binds to DbConnectionManager
 *
 * @author Andrew Wright
 */
public class JiveConnectionProvider implements ConnectionProvider {

    public void configure(Properties properties) throws HibernateException {
        // Do nothing
    }

    public Connection getConnection() throws SQLException {
        return DbConnectionManager.getConnection();
    }

    public void closeConnection(Connection connection) throws SQLException {
        DbConnectionManager.closeConnection(connection);
    }

    public void close() throws HibernateException {
        // Do nothing
    }

    public boolean supportsAggressiveRelease() {
        return false;
    }

}
