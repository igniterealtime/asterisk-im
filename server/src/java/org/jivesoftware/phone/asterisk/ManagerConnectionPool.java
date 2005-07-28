/**
 * $RCSfile: ManagerConnectionPool.java,v $
 * $Revision: 1.1 $
 * $Date: 2005/06/20 23:07:47 $
 *
 * Copyright (C) 1999-2004 Jive Software. All rights reserved.
 *
 * This software is the proprietary information of Jive Software. Use is subject to license terms.
 */
package org.jivesoftware.phone.asterisk;

import net.sf.asterisk.manager.ManagerConnection;

/**
 * @author Andrew Wright
 */
public interface ManagerConnectionPool {

    public ManagerConnection getConnection() throws ManagerException;

    public void close() throws ManagerException;

}
