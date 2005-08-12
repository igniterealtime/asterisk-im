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

import net.sf.asterisk.manager.ManagerConnection;
import org.jivesoftware.messenger.container.Plugin;
import org.jivesoftware.messenger.container.PluginManager;
import org.jivesoftware.phone.PacketHandler;
import org.jivesoftware.phone.database.HibernateUtil;
import org.jivesoftware.phone.util.PhoneConstants;
import org.jivesoftware.phone.util.ThreadPool;
import org.jivesoftware.util.JiveGlobals;
import org.jivesoftware.util.Log;
import org.xmpp.component.Component;
import org.xmpp.component.ComponentException;
import org.xmpp.component.ComponentManager;
import org.xmpp.component.ComponentManagerFactory;
import org.xmpp.packet.IQ;
import org.xmpp.packet.JID;
import org.xmpp.packet.Packet;

import java.io.File;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Plugin for integrating Asterisk with messenger. This plugin will create a new connection pull
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

    private static Logger log = Logger.getLogger(AsteriskPlugin.class.getName());

    private boolean isComponentReady = false;

    private PacketHandler packetHandler;

    public void initializePlugin(PluginManager manager, File pluginDirectory) {
        init();
    }

    public void init() {
        log.info("Initializing Asterisk-IM Plugin");

        try {
            log.info("Initializing Hibernate for Asterisk-IM");
            HibernateUtil.init();

            // Attempts to setup the database if it hasn't been done already
            log.info("Checking to see if Asterisk-IM database schema is present");
            List<Exception> exceptions = HibernateUtil.initDB();
            if(exceptions.size() > 0 ) {
                log.warning("Asterisk-IM table contains errors exited with error, database may not behave properly");
                for (Exception e : exceptions) {
                    Log.warn("Asterisk-IM table creation --> "+e.getMessage(), e);
                }
            }

            log.info("Initializing Asterisk-IM thread Pool");
            ThreadPool.init(); //initialize the thread pols
            initAsteriskManager();

        } catch (RuntimeException e) {
            // Make sure we catch all exceptions show we can log anything that might be
            // going on
            log.log(Level.SEVERE, e.getMessage(), e);
            throw e;
        }

        // only register the component if we are enabled
        if(JiveGlobals.getBooleanProperty(Properties.ENABLED, false)) {
            try {
                log.info("Registering phone plugin as a component");
                ComponentManagerFactory.getComponentManager().addComponent(NAME, this);
            }
            catch (ComponentException e) {
                log.log(Level.SEVERE, e.getMessage(), e);
                // Do nothing. Should never happen.
                ComponentManagerFactory.getComponentManager().getLog().error(e);
            }
        }

    }

    public void destroyPlugin() {
         destroy();
    }

    public void destroy() {

        log.info("unloading asterisk-im plugin resources");

        try {
            log.info("Registering asterisk-im plugin as a component");
            ComponentManagerFactory.getComponentManager().removeComponent(NAME);
        }
        catch (Exception e) {
            log.log(Level.SEVERE, e.getMessage(), e);
            // Do nothing. Should never happen.
            ComponentManagerFactory.getComponentManager().getLog().error(e);
        }

        try {
            closeManagerConnection();
            log.info("Shutting down Asterisk-IM Thread Pool");
            ThreadPool.shutdown();
            log.info("Shutting down Hibernate for Asterisk-IM");
            HibernateUtil.close();

        }
        catch (Exception e) {
            // Make sure we catch all exceptions show we can log anything that might be
            // going on
            log.log(Level.SEVERE, e.getMessage(), e);
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
        if(JiveGlobals.getBooleanProperty(Properties.ENABLED, false)) {

            log.info("Initializing Asterisk Manager connection");

            // Populate the manager configuration
            ManagerConfig asteriskManagerConfig = new ManagerConfig();
            asteriskManagerConfig.setServer(JiveGlobals.getProperty(Properties.SERVER));
            asteriskManagerConfig.setUsername(JiveGlobals.getProperty(Properties.USERNAME));
            asteriskManagerConfig.setPassword(JiveGlobals.getProperty(Properties.PASSWORD));

            int port = JiveGlobals.getIntProperty(Properties.PORT, -1);
            if (port > 0) {
                asteriskManagerConfig.setPort(port);
            }

            int poolsize = JiveGlobals.getIntProperty(Properties.POOLSIZE, -1);
            if (poolsize > 0) {
                asteriskManagerConfig.setMaxPoolSize(poolsize);
            }

            // Check to see if the configuration is valid then
            // Initialize the manager connection pool and create an eventhandler
            if (isConfigValid(asteriskManagerConfig)) {

                try {

                    // This will release the connection back to the pool so can be reinitialized
                    if (managerConnection != null) {
                        managerConnection.logoff();
                    }

                    ManagerConnectionPoolFactory.init(asteriskManagerConfig);
                    managerConnection = ManagerConnectionPoolFactory.getManagerConnectionPool()
                            .getConnection();

                    // Start handling events
                    managerConnection.addEventHandler(new AsteriskEventHandler(this));
                }
                catch (Exception e) {
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

    private void closeManagerConnection() {

        log.info("Closing Asterisk Manager Connection");

        try {
            if(managerConnection != null) {
                managerConnection.logoff();
            }
            ManagerConnectionPoolFactory.close();
        }
        catch (Exception e) {
            log.log(Level.SEVERE, "problem closing connection pool", e);
        }


    }


    /**
     * Returns true if the given ManagerConfig is valid
     *
     * @param config manager config to check
     * @return true if the config is valid
     */
    private boolean isConfigValid(ManagerConfig config) {

        return config.getServer() != null && !"".equals(config.getServer())
                && config.getUsername() != null && !"".equals(config.getUsername())
                && config.getPassword() != null && !"".equals(config.getPassword());

    }


}
