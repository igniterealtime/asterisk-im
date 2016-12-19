/**
 * $RCSfile: PhoneUser.java,v $
 * $Revision: 1.1 $
 * $Date: 2005/06/24 19:32:50 $
 *
 * Copyright (C) 1999-2004 Jive Software. All rights reserved.
 *
 * This software is the proprietary information of Jive Software. Use is subject to license terms.
 */
package org.jivesoftware.phone;

import org.jivesoftware.database.JiveID;

/**
 * Used to represent a user/channel relationship.
 *
 * @author Andrew Wright
 */
@JiveID(100)
public class PhoneUser implements java.io.Serializable {

    private long id;
    private String username;

    private static final long serialVersionUID = -3105905430411708323L;

    public PhoneUser() {
    }

    public PhoneUser(String username) {
        this.username = username;
    }

    public long getID() {
        return id;
    }

    public void setID(long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final PhoneUser phoneUser = (PhoneUser) o;

        if (id != phoneUser.id) {
            return false;
        }
        return !(username != null ? !username.equals(phoneUser.username) : phoneUser.username != null);

    }

    @Override
    public int hashCode() {
        int result;
        result = (int) (id ^ (id >>> 32));
        result = 29 * result + (username != null ? username.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("PhoneUser");
        sb.append("{id=").append(id);
        sb.append(", username='").append(username).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
