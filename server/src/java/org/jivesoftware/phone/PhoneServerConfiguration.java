/**
 * $RCSfile:  $
 * $Revision:  $
 * $Date:  $
 *
 * Copyright (C) 2006 Jive Software. All rights reserved.
 * This software is the proprietary information of Jive Software. Use is subject to license terms.
 */
package org.jivesoftware.phone;

/**
 *
 */
public interface PhoneServerConfiguration {
    boolean supportsMultipleServers();

    int getDefaultPort();
}
