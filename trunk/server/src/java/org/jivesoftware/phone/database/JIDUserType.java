/**
 * $RCSfile: JIDUserType.java,v $
 * $Revision: 1.4 $
 * $Date: 2005/06/20 22:14:27 $
 *
 * Copyright (C) 1999-2004 Jive Software. All rights reserved.
 *
 * This software is the proprietary information of Jive Software. Use is subject to license terms.
 */
package org.jivesoftware.phone.database;

import org.hibernate.HibernateException;
import org.hibernate.usertype.UserType;
import org.xmpp.packet.JID;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

/**
 * @author Andrew Wright
 */
public class JIDUserType implements UserType {

    public int[] sqlTypes() {
        return new int[] { Types.VARCHAR };
    }

    public Class returnedClass() {
        return JID.class;
    }

    public boolean equals(Object o, Object o1) throws HibernateException {
        return o.equals(o1);
    }

    public int hashCode(Object o) throws HibernateException {
        return o.hashCode();
    }

    public Object nullSafeGet(ResultSet resultSet, String[] strings, Object o)
            throws HibernateException, SQLException {
        return new JID(resultSet.getString(strings[0]));
    }

    public void nullSafeSet(PreparedStatement preparedStatement, Object o, int i)
            throws HibernateException, SQLException {

        JID jid = (JID) o;

        if(jid == null) {
            preparedStatement.setString(i, null);
        } else {
            preparedStatement.setString(i, jid.toString());
        }
    }

    public Object deepCopy(Object o) throws HibernateException {
        JID jid = (JID) o;

        return new JID(jid.toString());
    }

    public boolean isMutable() {
        return false;
    }

    public Serializable disassemble(Object o) throws HibernateException {
        return o.toString();
    }

    public Object assemble(Serializable serializable, Object o) throws HibernateException {
        String jidString = (String) serializable;
        return new JID(jidString);
    }

    public Object replace(Object o, Object o1, Object o2) throws HibernateException {
        return o;
    }


}
