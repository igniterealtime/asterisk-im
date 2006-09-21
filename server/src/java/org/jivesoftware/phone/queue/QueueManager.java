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
import org.jivesoftware.wildfire.user.UserNotFoundException;
import org.xmpp.packet.Presence;

import java.util.*;

/**
 *
 */
public class QueueManager {
    private PhoneManager phoneManager;
    private SessionManager sessionManager;
    private List<String> queueUsers;
    private List<String> unpausedUsers;
    private List<String> pausedUsers;
    private boolean isEnabled = false;

    public QueueManager(SessionManager sessionManager, PhoneManager phoneManager) {
        this.phoneManager = phoneManager;
        this.sessionManager = sessionManager;
    }

    public synchronized void updateQueueStatus(ClientSession session, Presence presence) {
        if(!isEnabled || !phoneManager.isQueueSupported()) {
            return;
        }
        String username;
        try {
            username = session.getUsername();
        }
        catch (UserNotFoundException e) {
            // session has not yet authenticated.
            return;
        }
        if (username != null && !queueUsers.contains(username)) {
            return;
        }
        if (checkPresence(presence) || checkQueueStatus(sessionManager.getSessions(username))) {
            enqueueUser(username);
        }
        else {
            dequeueUser(username);
        }
    }

    /**
     * Unpauses a user in the queue.
     *
     * @param username the username of the user to unpause in the queue.
     */
    public synchronized void enqueueUser(String username) {
        if(!isEnabled || !phoneManager.isQueueSupported() || !pausedUsers.contains(username)) {
            return;
        }

        Collection<PhoneDevice> devices = phoneManager.getPhoneDevicesByUsername(username);
        for(PhoneDevice device : devices) {
            String deviceName = device.getDevice();
            long serverID = device.getServerID();
            try {
                phoneManager.unpauseMemberInQueue(serverID, deviceName);
                pausedUsers.remove(username);
                unpausedUsers.add(username);
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
        if(!phoneManager.isQueueSupported() || !unpausedUsers.contains(username)) {
            return;
        }

        Collection<PhoneDevice> devices = phoneManager.getPhoneDevicesByUsername(username);
        for (PhoneDevice device : devices) {
            String deviceName = device.getDevice();
            long serverID = device.getServerID();
            try {
                phoneManager.pauseMemberInQueue(serverID, deviceName);
                unpausedUsers.remove(username);
                pausedUsers.add(username);
            }
            catch (PhoneException e) {
                Log.error("Error pausing device " + deviceName + " on server " + serverID, e);
            }
        }
    }

    public synchronized void startup() {
        if(isEnabled || !phoneManager.isQueueSupported()) {
            return;
        }
        initQueues();
        this.isEnabled = true;
    }

    private void initQueues() {
        queueUsers = populateQueues();
        checkUsers(queueUsers);
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
                if(user != null && !userQueues.contains(user.getUsername())) {
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
        unpausedUsers = new ArrayList<String>();
        pausedUsers = new ArrayList<String>();
        for(String user : users) {
            if(checkQueueStatus(sessionManager.getSessions(user))) {
                unpausedUsers.add(user);
            }
        }
        pausedUsers.addAll(users);
        pausedUsers.removeAll(unpausedUsers);
        syncQueues(unpausedUsers, pausedUsers);
    }

    private static boolean checkQueueStatus(Collection<ClientSession> sessions) {
        boolean isAvailable = false;
        for (ClientSession session : sessions) {
            if (checkPresence(session.getPresence())) {
                isAvailable = true;
                break;
            }
        }
        return isAvailable;
    }

    private static boolean checkPresence(Presence presence) {
        Presence.Show show = presence.getShow();
        return show == null || Presence.Show.chat.equals(show);
    }

    private void syncQueues(List<String> unpausedUsers, List<String> pausedUsers) {
        for(String user : unpausedUsers) {
            enqueueUser(user);
        }

        for(String user : pausedUsers) {
            dequeueUser(user);
        }
    }

    public synchronized void shutdown() {
        isEnabled = false;
    }
}
