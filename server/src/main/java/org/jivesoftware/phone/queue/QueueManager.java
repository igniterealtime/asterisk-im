/**
 * $RCSfile:  $
 * $Revision:  $
 * $Date:  $
 *
 * Copyright (C) 2006 Jive Software. All rights reserved.
 * This software is the proprietary information of Jive Software. Use is subject to license terms.
 */
package org.jivesoftware.phone.queue;

import org.jivesoftware.phone.PhoneDevice;
import org.jivesoftware.phone.PhoneException;
import org.jivesoftware.phone.PhoneManager;
import org.jivesoftware.phone.PhoneUser;
import org.jivesoftware.util.Log;
import org.jivesoftware.openfire.SessionManager;
import org.jivesoftware.openfire.session.ClientSession;
import org.jivesoftware.openfire.user.UserNotFoundException;
import org.xmpp.packet.Presence;

import java.util.*;

/**
 *
 */
public class QueueManager
{
    private PhoneManager phoneManager;
    private SessionManager sessionManager;
    private Map<String, Object> queueUsers;
    private Map<String, Object> unpausedUsers;
    private Map<String, Object> pausedUsers;
    private boolean isEnabled = false;

    public QueueManager(SessionManager sessionManager, PhoneManager phoneManager)
    {
        this.phoneManager = phoneManager;
        this.sessionManager = sessionManager;
    }

    /**
     *
     * @param presence new presence if presence is to be changed, <code>null</code> otherwise.
     * @param session
     */
    public synchronized void updateQueueStatus(Presence presence, ClientSession session)
    {
        final String username;

        if (!isEnabled || !phoneManager.isQueueSupported())
        {
            return;
        }

        try
        {
            username = session.getUsername();
        }
        catch (UserNotFoundException e)
        {
            // session has not yet authenticated.
            return;
        }

        if (username != null && !queueUsers.containsKey(username))
        {
            return;
        }

        Collection<ClientSession> sessions = sessionManager.getSessions(username);

        if (presence != null)
        {
            sessions.remove(session);
        }

        if ((presence != null && checkPresence(presence)) || checkQueueStatus(sessions))
        {
            enqueueUser(username);
        }
        else
        {
            dequeueUser(username);
        }
    }

    /**
     * Unpauses a user in the queue if queuing is enabled and the user is currently paused.
     *
     * @param username the username of the user to unpause in the queue.
     */
    private synchronized void enqueueUser(String username)
    {
        if (!isEnabled || !phoneManager.isQueueSupported() || !pausedUsers.containsKey(username))
        {
            return;
        }
        unpauseUser(username);
    }

    private void unpauseUser(String username)
    {
        Collection<PhoneDevice> devices = phoneManager.getPhoneDevicesByUsername(username);
        for (PhoneDevice device : devices)
        {
            String deviceName = device.getDevice();
            long serverID = device.getServerID();
            try
            {
                phoneManager.unpauseMemberInQueue(serverID, deviceName);
                pausedUsers.remove(username);
                unpausedUsers.put(username, "");
            }
            catch (PhoneException e)
            {
                Log.error("Error unpausing device " + deviceName + " on server " + serverID, e);
            }
        }
    }

    /**
     * Pauses a user in his queues.
     *
     * @param username the username of the user to pause in the queue.
     */
    private synchronized void dequeueUser(String username)
    {
        if (!phoneManager.isQueueSupported() || !unpausedUsers.containsKey(username))
        {
            return;
        }
        pauseUser(username);
    }

    private void pauseUser(String username)
    {
        Collection<PhoneDevice> devices = phoneManager.getPhoneDevicesByUsername(username);
        for (PhoneDevice device : devices)
        {
            String deviceName = device.getDevice();
            long serverID = device.getServerID();
            try
            {
                phoneManager.pauseMemberInQueue(serverID, deviceName);
                unpausedUsers.remove(username);
                pausedUsers.put(username, "");
            }
            catch (PhoneException e)
            {
                Log.error("Error pausing device " + deviceName + " on server " + serverID, e);
            }
        }
    }

    public synchronized void startup()
    {
        if (isEnabled || !phoneManager.isQueueSupported())
        {
            return;
        }
        initQueues();
        this.isEnabled = true;
    }

    private void initQueues()
    {
        queueUsers = populateQueues();
        checkUsers(queueUsers);
    }

    /**
     * Loads all queue members from the asterisk servers.
     */
    private Map<String, Object> populateQueues()
    {
        List<PhoneQueue> phoneQueues = new ArrayList<PhoneQueue>(phoneManager
                .getAllPhoneQueues());

        Map<String, Object> userQueues = new HashMap<String, Object>();
        for (PhoneQueue phoneQueue : phoneQueues)
        {
            long serverID = phoneQueue.getServerID();
            for (String device : phoneQueue.getDevices())
            {
                PhoneUser user = phoneManager.getPhoneUserByDevice(serverID, device);
                if (user != null)
                {
                    userQueues.put(user.getUsername(), "");
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
    private void checkUsers(Map<String, Object> users)
    {
        unpausedUsers = new HashMap<String, Object>();
        pausedUsers = new HashMap<String, Object>();
        for (String user : users.keySet())
        {
            if (checkQueueStatus(sessionManager.getSessions(user)))
            {
                unpausedUsers.put(user, "");
            }
        }
        pausedUsers.putAll(users);
        for (String unpausedUser : unpausedUsers.keySet())
        {
            pausedUsers.remove(unpausedUser);
        }
        syncQueues(new ArrayList<String>(unpausedUsers.keySet()),
                new ArrayList<String>(pausedUsers.keySet()));
    }

    /**
     * Checks whether is a user is to be considered available for queuing or not.
     *
     * @param sessions the sessions of the users to check.
     * @return <code>true</code> if at laest one session is available.
     */
    private static boolean checkQueueStatus(Collection<ClientSession> sessions)
    {
        boolean isAvailable = false;
        for (ClientSession session : sessions)
        {
            if (checkPresence(session.getPresence()))
            {
                isAvailable = true;
                break;
            }
        }
        return isAvailable;
    }

    private static boolean checkPresence(Presence presence)
    {
        Presence.Show show = presence.getShow();
        Presence.Type type = presence.getType();
        if (type == Presence.Type.unavailable)
        {
            return false;
        }
        return show == null || Presence.Show.chat.equals(show);
    }

    private void syncQueues(List<String> unpausedUsers, List<String> pausedUsers)
    {
        for (String user : unpausedUsers)
        {
            unpauseUser(user);
        }

        for (String user : pausedUsers)
        {
            pauseUser(user);
        }
    }

    public synchronized void shutdown()
    {
        isEnabled = false;
    }
}
