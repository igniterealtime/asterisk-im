/**
 * $RCSfile: JiveIDGenerator.java,v $
 * $Revision: 1.7 $
 * $Date: 2005/06/23 23:37:30 $
 *
 * Copyright (C) 1999-2004 Jive Software. All rights reserved.
 *
 * This software is the proprietary information of Jive Software. Use is subject to license terms.
 */
package org.jivesoftware.phone.database;

import org.hibernate.HibernateException;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.id.IdentifierGenerator;
import org.jivesoftware.database.SequenceManager;

import java.io.Serializable;

/**
 * @author Andrew Wright
 */
public class JiveIDGenerator implements IdentifierGenerator {

    public Serializable generate(SessionImplementor sessionImplementor, Object o)
            throws HibernateException {

        return SequenceManager.nextID(o);
       
    }

}
