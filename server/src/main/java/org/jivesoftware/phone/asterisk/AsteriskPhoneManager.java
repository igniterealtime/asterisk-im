/**
 * $RCSfile: AsteriskPhoneManager.java,v $
 * $Revision: 1.13 $
 * $Date: 2005/07/02 00:22:51 $
 *
 * Copyright (C) 1999-2004 Jive Software. All rights reserved.
 *
 * This software is the proprietary information of Jive Software. Use is subject to license terms.
 */
package org.jivesoftware.phone.asterisk;

import org.jivesoftware.phone.*;
import org.jivesoftware.phone.queue.PhoneQueue;
import org.jivesoftware.phone.database.PhoneDAO;
import org.jivesoftware.util.JiveGlobals;
import org.jivesoftware.util.Log;
import org.jivesoftware.util.JiveConstants;
import org.xmpp.packet.JID;
import org.xmpp.packet.Packet;
import org.asteriskjava.manager.TimeoutException;
import org.asteriskjava.manager.AuthenticationFailedException;
import org.asteriskjava.live.ManagerCommunicationException;
import org.asteriskjava.live.AsteriskChannel;
import org.asteriskjava.live.ChannelState;

import java.io.IOException;
import java.util.*;


/**
 * Asterisk dependent implementation of {@link PhoneManager}
 *
 * @author Andrew Wright
 * @since 1.0
 */
@PBXInfo(make = "Asterisk", version = "1.2")
public class AsteriskPhoneManager extends BasePhoneManager
{

    private final Map<Long, CustomAsteriskServer> asteriskServers
            = Collections.synchronizedMap(new HashMap<Long, CustomAsteriskServer>());
    AsteriskPlugin plugin;
    private Timer timer;

    public AsteriskPhoneManager(PhoneDAO dao)
    {
        super(dao);
        this.timer = new Timer("Channel Update Timer");
    }

    public void init(AsteriskPlugin plugin)
    {
        Log.info("Initializing Asterisk Manager connection");

        Collection<PhoneServer> servers = getPhoneServers();

        if (servers == null || servers.size() <= 0)
        {
            servers = loadLegacyServerConfiguration();
        }

        for (PhoneServer server : servers)
        {
            try
            {
                CustomAsteriskServer asteriskServer = connectToServer(server);

                if (asteriskServer != null)
                {
                    asteriskServers.put(server.getID(), asteriskServer);
                }
            }
            catch (Throwable t)
            {
                Log.error("Error connecting to asterisk server " + server.getName(), t);
            }
        }

        this.plugin = plugin;
        timer.scheduleAtFixedRate(new ChannelStatusTask(), ChannelStatusTask.PERIOD,
                ChannelStatusTask.PERIOD);
    }

    private Collection<PhoneServer> loadLegacyServerConfiguration()
    {
        // Populate the legacy manager configuration
        String serverAddress = JiveGlobals.getProperty(PhoneProperties.SERVER);
        String username = JiveGlobals.getProperty(PhoneProperties.USERNAME);
        String password = JiveGlobals.getProperty(PhoneProperties.PASSWORD);
        int port = JiveGlobals.getIntProperty(PhoneProperties.PORT, 5038);

        if (serverAddress == null || username == null || password == null || port <= 0)
        {
            return Collections.emptyList();
        }

        PhoneServer server = createPhoneServer("Default Server", serverAddress, port, username,
                password);
        for (PhoneDevice device : getAllPhoneDevices())
        {
            device.setServerID(server.getID());
        }

        // Loads the legacy server manager configuration into the database.
        return Arrays.asList(server);
    }


    private CustomAsteriskServer connectToServer(PhoneServer server) throws TimeoutException,
            IOException, AuthenticationFailedException, ManagerCommunicationException
    {
        CustomAsteriskServer asteriskServer = null;
        // Check to see if the configuration is valid then
        // Initialize the manager connection pool and create an eventhandler
        if (server != null && server.getHostname() != null && server.getUsername() != null
                && server.getPassword() != null)
        {
            asteriskServer = new CustomAsteriskServer(server.getHostname(),
                    server.getPort(), server.getUsername(), server.getPassword());
            asteriskServer.logon();
            asteriskServer.addAsteriskServerListener(new AsteriskIMServerListener(server.getID(), this, CallSessionFactory.getInstance()));
            // asteriskServer.addEventHandler(new AsteriskEventHandler(server.getID(), this, CallSessionFactory.getInstance()));
        }
        else
        {
            Log.warn("Asterisk IM configuration is invalid, please see admin tool!");
        }

        return asteriskServer;
    }

    public void destroy()
    {
        Log.debug("Shutting down Manager connections");
        timer.cancel();
        for (CustomAsteriskServer asteriskServer : asteriskServers.values())
        {
            try
            {
                asteriskServer.logoff();
            }
            catch (Throwable e)
            {
                // Make sure we catch all exceptions show we can Log anything that might be
                // going on
                Log.error(e.getMessage(), e);
            }
        }
        asteriskServers.clear();
    }

    public PhoneServerStatus getPhoneServerStatus(long serverID)
    {
        return asteriskServers.containsKey(serverID) ? PhoneServerStatus.connected
                : PhoneServerStatus.disconnected;
    }

    @Override
    public void removePhoneServer(long serverID)
    {
        CustomAsteriskServer asteriskServer = asteriskServers.remove(serverID);
        if (asteriskServer != null)
        {
            try
            {
                asteriskServer.logoff();
            }
            catch (Throwable e)
            {
                Log.error("Error disconnecting from asterisk manager", e);
            }
        }

        super.removePhoneServer(serverID);
    }

    public MailboxStatus mailboxStatus(long serverID, String mailbox) throws PhoneException
    {
        CustomAsteriskServer asteriskServer = asteriskServers.get(serverID);
        if (asteriskServer != null)
        {
            asteriskServer.getMailboxStatus(mailbox);
        }
        return null;
    }

    public Map<Long, Collection<String>> getConfiguredDevices() throws PhoneException
    {
        Map<Long, Collection<String>> deviceMap = new HashMap<Long, Collection<String>>();
        for (Map.Entry<Long, CustomAsteriskServer> asteriskManager : asteriskServers.entrySet())
        {
            List<String> devices = asteriskManager.getValue().getDevices();
            Collections.sort(devices);
            deviceMap.put(asteriskManager.getKey(), devices);
        }
        return Collections.unmodifiableMap(deviceMap);
    }

    public Collection<String> getConfiguredDevicesByServerID(long serverID) throws PhoneException
    {
        CustomAsteriskServer asteriskServer = asteriskServers.get(serverID);
        if (asteriskServer != null)
        {
            return asteriskServer.getDevices();
        }
        return null;
    }

    public Collection<AsteriskChannel> getStatus(long serverID) throws PhoneException
    {
        CustomAsteriskServer asteriskServer = asteriskServers.get(serverID);
        if (asteriskServer != null)
        {
            try
            {
                return asteriskServer.getChannels();
            }
            catch (ManagerCommunicationException e)
            {
                throw new PhoneException(e);
            }
        }
        return null;
    }

    public void dial(String username, String extension, JID jid) throws PhoneException
    {
        //acquire the jidUser object for the originating caller
        PhoneUser user = getPhoneUserByUsername(username);
        PhoneDevice primaryDevice = getPrimaryDevice(user.getID());

        // aquire the originating server
        CustomAsteriskServer asteriskServer = asteriskServers.get(primaryDevice.getServerID());
        if (asteriskServer != null)
        {
            asteriskServer.dial(primaryDevice, extension);
        }
        else
        {
            throw new PhoneException("Not connected to originate phone server.");
        }
    }

    public boolean isReady()
    {
        return plugin.isComponentReady();
    }

    public void sendPacket(Packet packet)
    {
        plugin.sendPacket(packet);
    }

    public void forward(String callSessionID, String username, String extension, JID jid)
            throws PhoneException
    {
        CallSession phoneSession = CallSessionFactory.getInstance()
                .getCallSession(callSessionID);
        if (phoneSession == null)
        {
            throw new PhoneException("Call session not currently stored in Asterisk-IM");
        }
        CustomAsteriskServer asteriskServer = asteriskServers.get(phoneSession.getServerID());
        if (asteriskServer == null)
        {
            throw new PhoneException("Not connected to asterisk server to forward call");
        }
        asteriskServer.forward(phoneSession, username, extension, jid);
    }

    @Override
    public PhoneServer createPhoneServer(String name, String serverAddress, int port,
                                         String username, String password)
    {
        PhoneServer server = super.createPhoneServer(name, serverAddress, port, username, password);
        try
        {
            CustomAsteriskServer asteriskServer = connectToServer(server);

            if (asteriskServer != null)
            {
                asteriskServers.put(server.getID(), asteriskServer);
            }
        }
        catch (Throwable t)
        {
            Log.error("Error connecting to " + name + " phone server", t);
        }
        return server;
    }

    private class ChannelStatusTask extends TimerTask
    {
        private static final long PERIOD = JiveConstants.MINUTE * 2;

        public void run()
        {
            for (CustomAsteriskServer asteriskServer : asteriskServers.values())
            {
                //noinspection unchecked
                Collection<AsteriskChannel> channels;
                try
                {
                    channels = asteriskServer.getChannels();
                }
                catch (ManagerCommunicationException e)
                {
                    Log.error("Error communicating with asterisk server", e);
                    continue;
                }
                updateChannels(channels);
            }
        }

        private void updateChannels(Collection<AsteriskChannel> channels)
        {
            for (AsteriskChannel channel : channels)
            {
                String uniqueID = channel.getId();

                CallSession callSession = CallSessionFactory.getInstance()
                        .getCallSession(uniqueID);
                if (callSession == null || ChannelState.UP.equals(channel.getState()))
                {
                    continue;
                }
                // The channel is not up
                Log.debug("AsteriskPhoneManger.ChannelStatusRunnable: User " +
                        callSession.getUsername() + " has no more call sessions, but his " +
                        "presence is still ON_PHONE. Changing to AVAILABLE");
                CallSessionFactory.getInstance().destroyPhoneSession(uniqueID);
            }
        }
    }

    @Override
    public boolean isQueueSupported()
    {
        return true;
    }

    @Override
    public void pauseMemberInQueue(long serverID, String deviceName) throws PhoneException
    {
        CustomAsteriskServer asteriskServer = asteriskServers.get(serverID);
        if (asteriskServer == null)
        {
            throw new PhoneException("Not connected to asterisk server to pause queue member");
        }
        asteriskServer.pauseMemberInQueue(deviceName);
    }

    @Override
    public void unpauseMemberInQueue(long serverID, String deviceName) throws PhoneException
    {
        CustomAsteriskServer asteriskServer = asteriskServers.get(serverID);
        if (asteriskServer == null)
        {
            throw new PhoneException("Not connected to asterisk server to unpause queue member");
        }
        asteriskServer.unpauseMemberInQueue(deviceName);
    }

    @Override
    public Collection<PhoneQueue> getAllPhoneQueues()
    {
        final Collection<PhoneQueue> phoneQueues = new ArrayList<PhoneQueue>();
        for (Map.Entry<Long, CustomAsteriskServer> entry : asteriskServers.entrySet())
        {
            final Long serverId;
            final CustomAsteriskServer asteriskServer;
            final Collection<PhoneQueue> queues;

            serverId = entry.getKey();
            asteriskServer = entry.getValue();
            try
            {
                queues = asteriskServer.getQueueMembers();
            }
            catch (PhoneException e)
            {
                Log.error("Unable to get queue members from server " + asteriskServer, e);
                continue;
            }

            for (PhoneQueue queue : queues)
            {
                queue.setServerID(serverId);
            }
            phoneQueues.addAll(queues);
        }
        return phoneQueues;
    }
}
