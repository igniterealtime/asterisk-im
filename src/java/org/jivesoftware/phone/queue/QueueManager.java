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
import org.jivesoftware.phone.PhoneUser;
import org.jivesoftware.util.Log;
import org.jivesoftware.wildfire.SessionManager;
import org.jivesoftware.wildfire.ClientSession;
import org.xmpp.packet.Presence;

import java.util.*;

/**
 *
 */
public class QueueManager {
    private PhoneManager phoneManager;
    private SessionManager sessionManager;

    public QueueManager(SessionManager sessionManager, PhoneManager phoneManager) {
        this.phoneManager = phoneManager;
        this.sessionManager = sessionManager;
    }

    /**
     * Unpauses a user in the queue.
     *
     * @param username the username of the user to unpause in the queue.
     */
    public synchronized void enqueueUser(String username) {
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
    public synchronized void dequeueUser(String username) {
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

    private synchronized void initQueues() {
        List<String> users = populateQueues();
        checkUsers(users);
    }

    /**
     * Loads all queue members from the asterisk servers.
     */
    private List<String> populateQueues() {
        List<PhoneQueue> phoneQueues = new ArrayList<PhoneQueue>(phoneManager
                .getAllPhoneQueues());

        List<String> userQueues = new ArrayList<String>();
        for(PhoneQueue phoneQueue : phoneQueues) {
            long serverID = phoneQueue.getServerID();
            for (String device : phoneQueue.getDevices()) {
                PhoneUser user = phoneManager.getPhoneUserByDevice(serverID, device);
                if(!userQueues.contains(user.getUsername())) {
                    userQueues.add(user.getUsername());
                }
            }
        }

        return userQueues;
    }

    /**
     * Take all users who are a part of a queue and sync there queue status, paused or unpaused,
     * to their current IM status.
     *
     * @param users the list of usernames for users who are part of a queue.
     */
    private void checkUsers(List<String> users) {
        List<String> unpausedUsers = new ArrayList<String>();
        List<String> pausedUsers = new ArrayList<String>();
        for(String user : users) {
            boolean isAvailable = false;
            Collection<ClientSession> sessions = sessionManager.getSessions(user);
            for(ClientSession session : sessions) {
                Presence.Show show = session.getPresence().getShow();
                if(show == null || Presence.Show.chat.equals(show)) {
                    isAvailable = true;
                    break;
                }
            }
            if(isAvailable) {
                unpausedUsers.add(user);
            }
            else {
                pausedUsers.add(user);
            }
        }
        syncQueues(unpausedUsers, pausedUsers);
    }

    private void syncQueues(List<String> unpausedUsers, List<String> pausedUsers) {
        for(String user : unpausedUsers) {
            enqueueUser(user);
        }

        for(String user : pausedUsers) {
            dequeueUser(user);
        }
    }

    public void shutdown() {

    }
}
