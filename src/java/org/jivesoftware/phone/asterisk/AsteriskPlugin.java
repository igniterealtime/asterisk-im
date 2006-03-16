/**
 * $RCSfile: AsteriskPlugin.java,v $
 * $Revision: 1.8 $
 * $Date: 2005/07/01 18:19:40 $
 *
 * Copyright (C) 1999-2004 Jive Software. All rights reserved.
 *
 * This software is the proprietary information of Jive Software. Use is subject to license terms.
 */
package org.jivesoftware.phone.asterisk;

import net.sf.asterisk.manager.DefaultManagerConnection;
import net.sf.asterisk.manager.ManagerConnection;
import org.jivesoftware.phone.OnPhonePacketInterceptor;
import org.jivesoftware.phone.PacketHandler;
import org.jivesoftware.phone.PhoneManagerFactory;
import org.jivesoftware.phone.database.DbPhoneDAO;
import org.jivesoftware.phone.util.PhoneConstants;
import org.jivesoftware.phone.util.ThreadPool;
import org.jivesoftware.util.JiveGlobals;
import org.jivesoftware.util.Log;
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

import java.io.File;

/**
 * Plugin for integrating Asterisk with wildfire. This plugin will create a new connection pull
 * to the asterisk manager server and assign a handler to handle events received from the server.
 * <p/>
 * This plugin exepects the following jive properties to be set up.
 * <ul>
 * <li>asterisk.manager.server - The asterisk server
 * <li>asterisk.manager.port - Port to connect to on the server (optional, default to 5038)
 * <li>asterisk.manager.username - Username to connect to the manager api with
 * <li>asterisk.manager.password - User's password
 * <li>asterisk.manager.poolsize - how many connection to make to the server (optional, default is 10)
 * </ul>
 * <p/>
 * If you are setting many of these properties at
 * one you might want to call setAutoReInitManager(false) otherwise the manager connection pool
 * will reinitialize each time the properties are changed. Make sure you set it back to true
 * and call initAsteriskManager() when completed!
 *
 * @author Andrew Wright
 */
public class AsteriskPlugin implements Plugin, Component, PhoneConstants {

    /**
     * The name of this plugin
     */
    public static final String NAME = "phone";

    /**
     * The description of this plugin
     */
    public static final String DESCRIPTION = "Asterisk integration component";


    // This class needs to keep one connection for handling events
    private ManagerConnection managerConnection = null;

    // The jid for this component
    private JID componentJID = null;

    // Current instance of the component manager
    private ComponentManager componentManager = null;

    /**
     * Flag that indicates if the plugin is being shutdown. When shutting down presences
     * of users that start a new conversation will not be modified to on-the-phone.
     */
    private boolean isComponentReady = false;

    private PacketHandler packetHandler;

    private final OnPhonePacketInterceptor onPhoneInterceptor = new OnPhonePacketInterceptor(this);


    public void initializePlugin(PluginManager manager, File pluginDirectory) {
        init();
    }

    public void init() {
        Log.info("Initializing Asterisk-IM Plugin");

        try {
            Log.info("Initializing Asterisk-IM thread Pool");
            ThreadPool.init(); //initialize the thread pols
            initAsteriskManager();

        }
        catch (Throwable e) {
            // Make sure we catch all exceptions show we can Log anything that might be
            // going on
            Log.error(e.getMessage(), e);
            Log.error("Asterisk-IM not Initializing because of errors");
            return;
        }

        // only register the component if we are enabled
        if (JiveGlobals.getBooleanProperty(Properties.ENABLED, false)) {
            try {
                Log.info("Registering phone plugin as a component");
                ComponentManagerFactory.getComponentManager().addComponent(NAME, this);
            }
            catch (ComponentException e) {
                Log.error(e.getMessage(), e);
                // Do nothing. Should never happen.
                ComponentManagerFactory.getComponentManager().getLog().error(e);
            }
        }

        // Register a packet interceptor for handling on on phone presence changes
        InterceptorManager.getInstance().addInterceptor(onPhoneInterceptor);

        // Register OnPhonePacketInterceptor as a session event listener
        SessionEventDispatcher.addListener(onPhoneInterceptor);
    }

    public void destroyPlugin() {
        destroy();
    }

    public void destroy() {
        Log.info("unloading asterisk-im plugin resources");

        try {
            Log.info("Unregistering asterisk-im plugin as a component");
            // Unregister this component. When unregistering the isComponentReady variable
            // will be set to false so new phone calls won't be processed.
            ComponentManagerFactory.getComponentManager().removeComponent(NAME);
        }
        catch (Throwable e) {
            Log.error(e.getMessage(), e);
            // Do nothing. Should never happen.
            ComponentManagerFactory.getComponentManager().getLog().error(e);
        }

        // Remove the packet interceptor
        InterceptorManager.getInstance().removeInterceptor(onPhoneInterceptor);

        // Remove OnPhonePacketInterceptor as a session event listener
        SessionEventDispatcher.removeListener(onPhoneInterceptor);

        try {
            managerConnection.logoff();
            Log.info("Shutting down Asterisk-IM Thread Pool");
            ThreadPool.shutdown();
            Log.info("Shutting down Hibernate for Asterisk-IM");

        }
        catch (Exception e) {
            // Make sure we catch all exceptions show we can Log anything that might be
            // going on
            Log.error(e.getMessage(), e);
            ComponentManagerFactory.getComponentManager().getLog().error(e);
        }


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

    /**
     * Returns the name of this component, "phone"
     *
     * @return The name, "phone"
     */
    public String getName() {
        return NAME;
    }

    /**
     * Returns the description of this component
     *
     * @return the description of this component
     */
    public String getDescription() {
        return DESCRIPTION;
    }

    public boolean isComponentReady() {
        return isComponentReady;
    }

    /**
     * Processes all IQ packets passed in, other types of packets will be ignored
     *
     * @param packet packet to process
     */
    public void processPacket(Packet packet) {

        if (!isComponentReady) {
            Log.warn("Phone component not ready, ignoring request");
            return;

        }

        if (!(packet instanceof IQ)) {
            Log.debug("Phone components expects packets to be IQ packets, ignoring!");
            return;
        }

        IQ iq = (IQ) packet;
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
        this.componentManager = componentManager;
        packetHandler = new PacketHandler(this);
    }

    /**
     * Initializes the manager connection with the asterisk server
     */
    public void initAsteriskManager() {

        // Only initialize things if the plugin is enabled
        if (JiveGlobals.getBooleanProperty(Properties.ENABLED, false)) {

            Log.info("Initializing Asterisk Manager connection");

            // Populate the manager configuration
            String server = JiveGlobals.getProperty(Properties.SERVER);
            String username = JiveGlobals.getProperty(Properties.USERNAME);
            String password = JiveGlobals.getProperty(Properties.PASSWORD);
            int port = JiveGlobals.getIntProperty(Properties.PORT, 5038);

            // Check to see if the configuration is valid then
            // Initialize the manager connection pool and create an eventhandler
            if (server != null && username != null && password != null) {

                try {

                    // This will release the connection back to the pool so can be reinitialized
                    if (managerConnection != null) {
                        managerConnection.logoff();
                    }

                    managerConnection = new DefaultManagerConnection(server, port, username, password);
                    AsteriskPhoneManager asteriskPhoneManager =
                            new AsteriskPhoneManager(new DbPhoneDAO(), managerConnection);
                    asteriskPhoneManager.init(this);
                    PhoneManagerFactory.init(asteriskPhoneManager);

                }
                catch (Throwable e) {
                    Log.error("unable to obtain a manager connection --> " + e.getMessage(), e);
                }

            }
            else {
                Log.warn("AsteriskPlugin configuration is invalid, please see admin tool!!");
            }
        }
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
}
