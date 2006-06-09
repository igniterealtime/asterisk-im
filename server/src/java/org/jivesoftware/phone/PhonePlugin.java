/**
 * Copyright (C) 1999-2004 Jive Software. All rights reserved.
 * Copyright (C) 2006 headissue GmbH; Jens Wilke. All rights reserved.
 *
 * This software is published under the terms of the GNU Public License (GPL),
 * a copy of which is included in this distribution.
 *
 */
package org.jivesoftware.phone;

import org.jivesoftware.database.DbConnectionManager;
import org.jivesoftware.phone.database.DatabaseUtil;
import org.jivesoftware.phone.util.PhoneConstants;
import org.jivesoftware.util.JiveConstants;
import org.jivesoftware.util.JiveGlobals;
import org.jivesoftware.util.Log;
import org.jivesoftware.wildfire.ClientSession;
import org.jivesoftware.wildfire.SessionManager;
import org.jivesoftware.wildfire.XMPPServer;
import org.jivesoftware.wildfire.container.Plugin;
import org.jivesoftware.wildfire.container.PluginManager;
import org.jivesoftware.wildfire.event.SessionEventDispatcher;
import org.jivesoftware.wildfire.interceptor.InterceptorManager;
import org.xmpp.component.Component;
import org.xmpp.component.ComponentException;
import org.xmpp.component.ComponentManager;
import org.xmpp.component.ComponentManagerFactory;
import org.xmpp.packet.IQ;
import org.xmpp.packet.JID;
import org.xmpp.packet.Packet;
import org.xmpp.packet.Presence;

import java.io.File;
import java.sql.Connection;
import java.util.Collection;

public abstract class PhonePlugin implements Plugin, Component, PhoneConstants {

    // FIXME: a plugin should get the server reference throuh initialization ;jw
    protected XMPPServer server = XMPPServer.getInstance();
    protected PhoneManager phoneManager;
    protected PacketHandler packetHandler;
    protected PresenceLayerer interceptor;
    private boolean isComponentReady;
    private JID componentJID;
    private ComponentManager componentManager;

    /**
     * Send a packet to all user sessions
     */
    public void sendPacket2User(String user, Packet message) {
        message.setFrom(getComponentJID());
        SessionManager sessionManager = server.getSessionManager();
        Collection<ClientSession> sessions = sessionManager.getSessions(user);
        if (sessions.size() == 0) {
            return;
        }
        for (ClientSession session : sessions) {
            message.setTo(session.getAddress());
            sendPacket(message);
        }
    }

    public abstract void initPhoneManager();

    public void initializePlugin(PluginManager manager, File pluginDirectory) {
        init();
    }

    public void init() {
        Log.info("Initializing phone plugin");

        Connection con = null;
        try {
            con = DbConnectionManager.getConnection();
            DatabaseUtil.upgradeDatabase(con);
        }
        catch (Exception e) {
            Log.error(e);
        }
        finally {
            DbConnectionManager.closeConnection(con);
        }

        try {
            initPhoneManager();
        }
        catch (Throwable e) {
            // Make sure we catch all exceptions show we can Log anything that might be
            // going on
            Log.error(e.getMessage(), e);
            Log.error("Asterisk-IM not Initializing because of errors");
            return;
        }

        packetHandler = new PacketHandler(phoneManager, this);
        interceptor = new PresenceLayerer();

        // Register a packet interceptor for handling on phone presence changes
        InterceptorManager.getInstance().addInterceptor(interceptor);

        // Register OnPhonePacketInterceptor as a session event listener
        SessionEventDispatcher.addListener(interceptor);

        componentManager = ComponentManagerFactory.getComponentManager();
        // only register the component if we are enabled
        if (JiveGlobals.getBooleanProperty(PhoneProperties.ENABLED, false)) {
            try {
                Log.info("Registering phone plugin as a component");
                componentManager.addComponent(getName(), this);
            }
            catch (Throwable e) {
                Log.error(e.getMessage(), e);
                // Do nothing. Should never happen.
                if (componentManager != null) {
                    componentManager.getLog().error(e);
                }
            }
        }
    }

    public void destroyPlugin() {
        destroy();
    }

    public void destroy() {
        Log.info("unloading asterisk-im plugin resources");

        try {
            Log.info("Unregistering phone plugin as a component");
            // Unregister this component. When unregistering the isComponentReady variable
            // will be set to false so new phone calls won't be processed.
            componentManager.removeComponent(getName());
        }
        catch (Throwable e) {
            Log.error(e.getMessage(), e);
            // Do nothing. Should never happen.
            componentManager.getLog().error(e);
        }

        interceptor.restoreCompletely();

        // If there isn't a manager instance established don't try to destroy it.
        if (phoneManager != null) {
            phoneManager.destroy();
        }

        // Remove the packet interceptor
        InterceptorManager.getInstance().removeInterceptor(interceptor);

        // Remove OnPhonePacketInterceptor as a session event listener
        SessionEventDispatcher.removeListener(interceptor);
    }

    /**
     * sets isComponentReady to true so we start accepting requests
     */
    public void start() {
        isComponentReady = true;
    }

    /**
     * Sets isComponentReady to false we will quit accepting requests
     */
    public void shutdown() {
        isComponentReady = false;
    }


    public boolean isComponentReady() {
        return isComponentReady;
    }

    /**
     * Restart the plugin, used by the web configuration after the
     * configuration changed
     */
    public void restart() {
        try {
            destroy();
            Thread.sleep(1 * JiveConstants.SECOND);
            init();
            Thread.sleep(1 * JiveConstants.SECOND);
        }
        catch (InterruptedException e) {
            Log.error(e);
        }
    }

    public abstract PhoneManager getPhoneManager();

    /**
     * Processes all IQ packets passed in, other types of packets will be ignored
     *
     * @param packet packet to process
     */
    public void processPacket(Packet packet) {

        // race, packatHandler not yet there
        if (packetHandler == null) {
            return;
        }

        if (!isComponentReady) {
            Log.warn("Phone component not ready, ignoring request");
            return;
        }

        if (!(packet instanceof IQ)) {
            return;
        }

        IQ iq = (IQ) packet;
        // This is where actions are processed, dialing, etc.
        packetHandler.processPacket(iq);

    }

    /**
     * Initializes the component.
     *
     * @param jid              the jid of the component
     * @param componentManager instance of the componentManager
     * @throws ComponentException thrown if there are issues initializing this component
     */
    public void initialize(JID jid, ComponentManager componentManager) throws ComponentException {
        this.componentJID = jid;
    }


    /**
     * Used to send a packet with this component
     *
     * @param packet the package to send
     */
    public void sendPacket(Packet packet) {
        try {
            componentManager.sendPacket(this, packet);
        }
        catch (Exception e) {
            Log.error(e);
        }
    }

    /**
     * Returns JID for this component
     *
     * @return the jid for this component
     */
    public JID getComponentJID() {
        return componentJID;
    }

    public void restorePresence(String user) {
        interceptor.restorePresence(user);
    }

    public void setPresence(String user, Presence p) {
        interceptor.setPresence(user, p);
    }

    public abstract PhoneOption[] getOptions();

}
