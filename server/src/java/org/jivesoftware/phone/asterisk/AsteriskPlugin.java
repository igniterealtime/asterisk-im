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

import org.jivesoftware.phone.*;
import org.jivesoftware.phone.database.DbPhoneDAO;

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
 * </ul>
 * <p/>
 * If you are setting many of these properties at
 * one you might want to call setAutoReInitManager(false) otherwise the manager connection pool
 * will reinitialize each time the properties are changed. Make sure you set it back to true
 * and call initAsteriskManager() when completed!
 *
 * @author Andrew Wright
 * @since 1.0
 */
@PluginVersion("1.1.0")
@PBXInfo(make = "Asterisk", version = "1.2")
public class AsteriskPlugin extends PhonePlugin {

    /**
     * The name of this plugin
     */
    public static final String NAME = "phone";

    /**
     * The description of this plugin
     */
    public static final String DESCRIPTION = "Asterisk integration component";
    private AsteriskPhoneManager asteriskPhoneManager;

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
     * Initializes the manager connection with the asterisk server
     *
     * @param isPluginEnabled true if the plugin is currently enabled and false if it is not.
     */
    public void initPhoneManager(boolean isPluginEnabled) throws Exception {
        if (!isPluginEnabled) {
            asteriskPhoneManager = null;
            return;
        }

        // Only initialize things if the plugin is enabled
        asteriskPhoneManager = new AsteriskPhoneManager(new DbPhoneDAO());
        asteriskPhoneManager.init(this);
    }

    protected void disablePhoneManager() {
        if(asteriskPhoneManager == null) {
            return;
        }
        asteriskPhoneManager.destroy();
        asteriskPhoneManager = null;
    }

    @Override
    public PhoneManager getPhoneManager() {
        return asteriskPhoneManager;
    }

    @Override
    public PhoneOption[] getOptions() {
        return new PhoneOption[]{
                new PhoneOption("Drop-down device selection",
                        PhoneProperties.DEVICE_DROP_DOWN,
                        "DropDown",
                        PhoneOption.FLAG),
                new PhoneOption("Asterisk Context",
                        PhoneProperties.CONTEXT,
                        "Context"),
                new PhoneOption("Default Caller ID",
                        PhoneProperties.DEFAULT_CALLER_ID,
                        "DefaultCallerId"),
                new PhoneOption("Dial Command Variables",
                        PhoneProperties.DIAL_VARIABLES,
                        "DialVariables")};
    }

    public PhoneServerConfiguration getServerConfiguration() {
        return new PhoneServerConfiguration() {
            public boolean supportsMultipleServers() {
                return true;
            }
        };
    }
}
