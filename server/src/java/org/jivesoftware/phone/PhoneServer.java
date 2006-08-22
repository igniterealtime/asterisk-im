/**
 * $RCSfile:  $
 * $Revision:  $
 * $Date:  $
 *
 * Copyright (C) 2006 Jive Software. All rights reserved.
 * This software is the proprietary information of Jive Software. Use is subject to license terms.
 */
package org.jivesoftware.phone;

import org.jivesoftware.database.JiveID;
import org.jivesoftware.phone.util.PhoneConstants;

/**
 * Represents the information for a phone server which Asterisk-IM can use to connect to a
 * PBX.
 */
@JiveID(PhoneConstants.SERVER_SEQUENCE)
public class PhoneServer {
    private String name;
    private String hostname;
    private String username;
    private String password;
    private int port;
    private long id;

    /**
     * Sets the unique ID for the PBX server.
     *
     * @param id the unique ID for the server.
     */
    public void setID(long id) {
        this.id = id;
    }

    /**
     * Returns the unique ID for the PBX server.
     *
     * @return the unique ID for the PBX server.
     */
    public long getID() {
        return id;
    }

    /**
     * A unique name entered by the user in order to identify a PBX server in order to
     * facilitate the creation of appropriate device mappings.
     *
     * @param name the name entered by the user.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns a unique name entered by the user in order to identify a PBX server in order to
     * facilitate the creation of appropriate device mappings.
     *
     * @return a unique name entered by the user in order to identify a PBX server in order to
     *         facilitate the creation of appropriate device mappings.
     */
    public String getName() {
        return name;
    }

    /**
     * The hostname or IP of the PBX server which Asterisk-IM will use to create a socket
     * connection.
     *
     * @param hostname the hostname or IP of the PBX server which Asterisk-IM will use to create a
     * socket connection.
     */
    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    /**
     * Returns the hostname or IP of the PBX server which Asterisk-IM will use to create a socket
     * connection.
     *
     * @return the hostname or IP of the PBX server which Asterisk-IM will use to create a socket
     *         connection.
     */
    public String getHostname() {
        return hostname;
    }

    /**
     * Sets the port to be used to connect to the PBX server.
     *
     * @param port the port to be used to connect to the PBX server.
     */
    public void setPort(int port) {
        this.port = port;
    }

    /**
     * Returns the port to be used to connect to the PBX server.
     *
     * @return the port to be used to connect to the PBX server.
     */
    public int getPort() {
        return port;
    }

    /**
     * The username credential with which Asterisk-IM will connect to the PBX server.
     *
     * @param username credential with which Asterisk-IM will connect to the PBX server.
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Returns the username credential with which Asterisk-IM will connect to the PBX server.
     *
     * @return the username credential with which Asterisk-IM will connect to the PBX server.
     */
    public String getUsername() {
        return username;
    }

    /**
     * The password related to the username with which Asterisk-IM will use to connect to the PBX
     * server.
     *
     * @param password the password related to the username with which Asterisk-IM will use to
     * connect to the PBX server.
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Returns the password related to the username with which Asterisk-IM will use to connect to
     * the PBX server.
     *
     * @return the password related to the username with which Asterisk-IM will use to connect to
     *         the PBX server.
     */
    public String getPassword() {
        return password;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("PhoneServer");
        sb.append("{id=").append(id);
        sb.append(", name='").append(name).append('\'');
        sb.append(", hostname='").append(hostname).append('\'');
        sb.append(", port=").append(port);
        sb.append(", username='").append(username).append('\'');
        sb.append(", password='").append(password).append('\'');
        sb.append("}");
        return sb.toString();
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
