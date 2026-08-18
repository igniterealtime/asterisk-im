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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.JID;
import org.xmpp.packet.Packet;
import org.asteriskjava.manager.TimeoutException;
import org.asteriskjava.manager.AuthenticationFailedException;
import org.asteriskjava.live.ManagerCommunicationException;
import org.asteriskjava.live.AsteriskChannel;
import org.asteriskjava.live.ChannelState;

import java.io.IOException;
import java.time.Duration;
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
    private static final Logger Log = LoggerFactory.getLogger(AsteriskPhoneManager.class);

    private final Map<Long, CustomAsteriskServer> asteriskServers
            = Collections.synchronizedMap(new HashMap<Long, CustomAsteriskServer>());

    /**
     * Why the last connection attempt to a given server failed, keyed by server ID. A server that
     * is connected has no entry. The admin console reads this to explain a disconnected server,
     * which the connected/disconnected indicator on its own cannot do.
     */
    private final Map<Long, String> connectionErrors
            = Collections.synchronizedMap(new HashMap<Long, String>());
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
            openConnection(server);
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
        closeConnection(serverID);
        connectionErrors.remove(serverID);

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
        Log.debug("Get configured devices by server ID '{}'", serverID);
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
        if (user == null)
        {
            // PacketHandler turns a PhoneException into an IQ error the caller can be shown.
            // Anything else leaves the dial request unanswered, and the client waits for a reply
            // that never arrives.
            throw new PhoneException("No phone is mapped to user '" + username
                    + "'. Add one under Asterisk-IM, Phone Mappings.");
        }

        PhoneDevice primaryDevice = getPrimaryDevice(user.getID());
        if (primaryDevice == null)
        {
            throw new PhoneException("User '" + username
                    + "' has no primary phone. Mark one of their phones as primary under"
                    + " Asterisk-IM, Phone Mappings.");
        }

        // aquire the originating server
        CustomAsteriskServer asteriskServer = asteriskServers.get(primaryDevice.getServerID());
        if (asteriskServer != null)
        {
            asteriskServer.dial(primaryDevice, extension);
        }
        else
        {
            String reason = getConnectionError(primaryDevice.getServerID());
            throw new PhoneException("Not connected to the phone server that '"
                    + primaryDevice.getDevice() + "' belongs to."
                    + (reason == null ? "" : " " + reason));
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
        openConnection(server);
        return server;
    }

    /**
     * Applies an edited configuration and then reconnects with it.
     *
     * <p>Without this override the edited details reach the database while the connection built
     * from the previous ones stays in use, so the change appears to have been accepted but has no
     * effect until Openfire restarts, and details that do not work are never found out about.
     */
    @Override
    public PhoneServer updatePhoneServer(long serverID, String serverName, String serverAddress,
                                         int serverPort, String username, String password)
    {
        PhoneServer server = super.updatePhoneServer(serverID, serverName, serverAddress,
                serverPort, username, password);
        if (server != null)
        {
            openConnection(server);
        }
        return server;
    }

    /**
     * Connects to a phone server, replacing any connection already held for it, and records
     * whether that succeeded.
     *
     * <p>The outcome is not returned: a failure is left in {@link #connectionErrors}, where the
     * admin console picks it up and shows it against the server it belongs to.
     *
     * @param server the server to connect to. May be null, in which case nothing happens.
     */
    private void openConnection(PhoneServer server)
    {
        if (server == null)
        {
            return;
        }

        closeConnection(server.getID());

        try
        {
            CustomAsteriskServer asteriskServer = connectToServer(server);

            if (asteriskServer == null)
            {
                // connectToServer rejects an incomplete configuration by returning null.
                connectionErrors.put(server.getID(), "The configuration is incomplete.");
                return;
            }

            asteriskServers.put(server.getID(), asteriskServer);
            connectionErrors.remove(server.getID());
        }
        catch (AuthenticationFailedException e)
        {
            Log.warn("Rejected by the '{}' phone server: {}", server.getName(), e.getMessage());
            connectionErrors.put(server.getID(), "The server rejected the username or password.");
        }
        catch (TimeoutException e)
        {
            Log.warn("Timed out connecting to the '{}' phone server: {}", server.getName(), e.getMessage());
            connectionErrors.put(server.getID(), "Timed out connecting to " + server.getHostname()
                    + ":" + server.getPort() + ".");
        }
        catch (Throwable t)
        {
            Log.error("Error connecting to the '" + server.getName() + "' phone server", t);
            connectionErrors.put(server.getID(), t.getMessage() == null
                    ? t.getClass().getSimpleName() : t.getMessage());
        }
    }

    /**
     * Drops the connection held for a server, if there is one.
     *
     * @param serverID identifies the server.
     */
    private void closeConnection(long serverID)
    {
        CustomAsteriskServer previous = asteriskServers.remove(serverID);
        if (previous != null)
        {
            try
            {
                previous.logoff();
            }
            catch (Throwable t)
            {
                Log.debug("Error while dropping the previous connection to server {}", serverID, t);
            }
        }
    }

    /**
     * Reports whether a phone server has a given device.
     *
     * @param serverID identifies the server to ask.
     * @param device the device, in the '&lt;technology&gt;/&lt;name&gt;' form the plugin stores.
     * @return true when the server has it, false when it reports that it does not, and null when
     *         the question could not be answered.
     */
    public Boolean isDeviceAvailable(long serverID, String device)
    {
        CustomAsteriskServer asteriskServer = asteriskServers.get(serverID);
        return asteriskServer == null ? null : asteriskServer.isDeviceAvailable(device);
    }

    /**
     * Reports whether a dialplan context exists on any connected phone server.
     *
     * @param context the dialplan context to look for.
     * @return true when a connected server has it, false when every connected server reports that
     *         it does not, and null when no connected server could answer.
     */
    public Boolean isContextAvailable(String context)
    {
        Boolean result = null;
        for (CustomAsteriskServer asteriskServer : new ArrayList<>(asteriskServers.values()))
        {
            Boolean available = asteriskServer.isContextAvailable(context);
            if (Boolean.TRUE.equals(available))
            {
                return true;
            }
            if (Boolean.FALSE.equals(available))
            {
                result = false;
            }
        }
        return result;
    }

    /**
     * Returns why the last connection attempt to a server failed.
     *
     * @param serverID identifies the server.
     * @return the reason, or null when the server is connected or has never been tried.
     */
    public String getConnectionError(long serverID)
    {
        return connectionErrors.get(serverID);
    }

    private class ChannelStatusTask extends TimerTask
    {
        private static final long PERIOD = Duration.ofMinutes(2).toMillis();

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
