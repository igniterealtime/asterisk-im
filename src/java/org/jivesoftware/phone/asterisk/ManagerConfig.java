/**
 * $RCSfile: ManagerConfig.java,v $
 * $Revision: 1.1 $
 * $Date: 2005/06/20 23:07:47 $
 *
 * Copyright (C) 1999-2004 Jive Software. All rights reserved.
 *
 * This software is the proprietary information of Jive Software. Use is subject to license terms.
 */
package org.jivesoftware.phone.asterisk;

/**
 * @author Andrew Wright
 */
public class ManagerConfig {

    public static final int DEFAULT_MAX_POOL_SIZE = 10;
    public static final int DEFAULT_PORT = 5038;

    private String username;
    private String password;
    private String server;
    private int port = DEFAULT_PORT;
    private int maxPoolSize = DEFAULT_MAX_POOL_SIZE;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getServer() {
        return server;
    }

    public void setServer(String server) {
        this.server = server;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getMaxPoolSize() {
        return maxPoolSize;
    }

    public void setMaxPoolSize(int maxPoolSize) {
        this.maxPoolSize = maxPoolSize;
    }

}
