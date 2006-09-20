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
import org.jivesoftware.phone.PhoneDevice;
import org.jivesoftware.phone.PhoneException;
import org.jivesoftware.util.Log;

import java.util.Collection;

/**
 *
 */
public class QueueManager {
    private PhoneManager phoneManager;

    public QueueManager(PhoneManager phoneManager) {
        this.phoneManager = phoneManager;
    }

    /**
     * Unpauses a user in the queue.
     *
     * @param username the username of the user to unpause in the queue.
     */
    public void enqueueUser(String username) {
        if(!phoneManager.isQueueSupported()) {
            return;
        }

        Collection<PhoneDevice> devices = phoneManager.getPhoneDevicesByUsername(username);
        for(PhoneDevice device : devices) {
            String deviceName = device.getDevice();
            long serverID = device.getServerID();
            try {
                phoneManager.unpauseMemberInQueue(serverID, deviceName);
            }
            catch (PhoneException e) {
                Log.error("Error unpausing device " + deviceName + " on server " + serverID, e);
            }
        }
    }

    /**
     * Pauses a user in their queues.
     *
     * @param username the username of the user to pause in the queue.
     */
    public void dequeueUser(String username) {
        if(!phoneManager.isQueueSupported()) {
            return;
        }

        Collection<PhoneDevice> devices = phoneManager.getPhoneDevicesByUsername(username);
        for (PhoneDevice device : devices) {
            String deviceName = device.getDevice();
            long serverID = device.getServerID();
            try {
                phoneManager.pauseMemberInQueue(serverID, deviceName);
            }
            catch (PhoneException e) {
                Log.error("Error pausing device " + deviceName + " on server " + serverID, e);
            }
        }
    }

    public void startup() {
        if(!phoneManager.isQueueSupported()) {
            return;
        }
        initQueues();
    }

    private void initQueues() {
        populateQueues();
    }

    /**
     * Loads all queue members from the asterisk servers.
     */
    private void populateQueues() {
        Collection<PhoneQueue> phoneQueue = phoneManager.getAllPhoneQueues();
    }

    public void shutdown() {

    }
}
