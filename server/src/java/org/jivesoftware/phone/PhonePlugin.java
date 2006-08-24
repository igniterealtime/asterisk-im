/**
 * Copyright (C) 1999-2004 Jive Software. All rights reserved.
 * Copyright (C) 2006 headissue GmbH; Jens Wilke. All rights reserved.
 *
 * This software is published under the terms of the GNU Public License (GPL),
 * a copy of which is included in this distribution.
 *
 */
package org.jivesoftware.phone;

import org.jivesoftware.phone.util.PhoneConstants;
import org.jivesoftware.phone.util.PhoneExecutionService;
import org.jivesoftware.phone.xmpp.PresenceLayerer;
import org.jivesoftware.util.JiveGlobals;
import org.jivesoftware.util.Log;
import org.jivesoftware.util.PropertyEventListener;
import org.jivesoftware.util.PropertyEventDispatcher;
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
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

public abstract class PhonePlugin implements Plugin, Component, PhoneConstants {

    // FIXME: a plugin should get the server reference throuh initialization ;jw
    protected XMPPServer server = XMPPServer.getInstance();
    protected PacketHandler packetHandler;
    protected PresenceLayerer interceptor;
    private boolean isComponentReady;
    private JID componentJID;
    private ComponentManager componentManager;
    private Future<Boolean> enableProcess;
    private PropertyListener propertyListener;

    public PhonePlugin() {
        this.propertyListener = new PropertyListener();
    }

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

    public abstract void initPhoneManager(boolean enabled);

    public void initializePlugin(PluginManager manager, File pluginDirectory) {
        PropertyEventDispatcher.addListener(propertyListener);
        init(JiveGlobals.getBooleanProperty(PhoneProperties.ENABLED, false));
    }

    public void init(boolean isEnabled) {
        Log.info("Initializing phone plugin");
        try {
            initPhoneManager(isEnabled);
        }
        catch (Throwable e) {
            // Make sure we catch all exceptions show we can Log anything that might be
            // going on
            Log.error(e.getMessage(), e);
            Log.error("Asterisk-IM not Initializing because of errors");
            return;
        }

        packetHandler = new PacketHandler(getPhoneManager(), this);
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
        PropertyEventDispatcher.removeListener(propertyListener);
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
        PhoneManager manager = getPhoneManager();
        if (manager != null) {
            manager.destroy();
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

    public void setEnabled(boolean enabled) {
        JiveGlobals.setProperty(PhoneProperties.ENABLED, String.valueOf(enabled));
    }

    public synchronized boolean isEnabled() throws Exception {
        final Future<Boolean> enableTask = enableProcess;
        if(enableTask != null) {
            try {
                return enableTask.get();
            } catch (Throwable t) {
                Log.error("Error starting or stoping Asterisk-IM", t);
            }
        }
        return getPhoneManager() != null;
    }

    /**
     * Enables or disables the plugin.
     *
     * @param shouldEnable true to enable and false to disable
     */
    private void doEnable(final boolean shouldEnable) {
        enableProcess = PhoneExecutionService.getService().submit(new Callable<Boolean>() {
            public Boolean call() throws Exception {
                if(shouldEnable) {
                    init(true);
                }
                else {
                    destroy();
                }
                return shouldEnable;
            }
        });
    }

    /**
     * Returns the configuration options for connecting to a phone server or multiple phone
     * servers.
     *
     * @return the configuration options for connecting to a phone server or multiple phone
     * servers.
     */
    public abstract PhoneServerConfiguration getServerConfiguration();

    private class PropertyListener implements PropertyEventListener  {

        public void propertySet(String property, Map params) {
            if(PhoneProperties.ENABLED.equals(property)) {
                Object value = params.get("value");
                handleEnable((value != null && Boolean.valueOf(value.toString())));
            }
        }

        public void propertyDeleted(String property, Map params) {
            if (PhoneProperties.ENABLED.equals(property)) {
                handleEnable(false);
            }
        }

        public void xmlPropertySet(String property, Map params) {
        }

        public void xmlPropertyDeleted(String property, Map params) {
        }

        private void handleEnable(boolean shouldEnable) {
            try {
                boolean isCurrentlyEnabled = isEnabled();
                if (isCurrentlyEnabled != shouldEnable) {
                    doEnable(shouldEnable);
                }
            }
            catch (Exception ex) {
                /* Do Nothing as this exception is logged in isEnabled() */
            }
        }
    }

}
