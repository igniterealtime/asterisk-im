/**
 * $RCSfile:  $
 * $Revision:  $
 * $Date:  $
 *
 * Copyright (C) 2006 Jive Software. All rights reserved.
 * This software is the proprietary information of Jive Software. Use is subject to license terms.
 */
package org.jivesoftware.phone.queue;

import org.jivesoftware.phone.PhoneManager;

/**
 *
 */
public class QueueManager {
    private PhoneManager phoneManager;

    public QueueManager(PhoneManager phoneManager) {
        this.phoneManager = phoneManager;
    }

    public void enqueueUser(String username) {

    }

    public void pauseUserQueue(String username) {
        
    }

    public void dequeueUser(String username) {

    }
}
