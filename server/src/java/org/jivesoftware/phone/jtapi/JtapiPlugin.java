/*
 * Copyright (C) 2006 headissue GmbH; Jens Wilke. All rights reserved.
 *
 * This software is published under the terms of the GNU Public License (GPL),
 * a copy of which is included in this distribution.
 */

package org.jivesoftware.phone.jtapi;

import org.jivesoftware.phone.*;
import org.jivesoftware.phone.util.PhoneConstants;
import org.jivesoftware.phone.database.DbPhoneDAO;
import org.jivesoftware.util.JiveGlobals;
import org.jivesoftware.util.Log;

/**
 *
 */
@PluginVersion("1.0.0")
@PBXInfo(make = "JTAPI", version = "0.2")
public class JtapiPlugin extends PhonePlugin {
	
	JtapiPhoneManager jtpaiPhoneManager;

    /**
     * The name of the jabber server component
     */
    public static final String NAME = "phone";

    /**
     * The description of this plugin
     */
    public static final String DESCRIPTION = "JTAPI integration component";
    
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
     * @param enabled
     */
    public void initPhoneManager(boolean enabled) {
        // Only initialize things if the plugin is enabled
        if (JiveGlobals.getBooleanProperty(PhoneProperties.ENABLED, false)) {
            try {
                jtpaiPhoneManager = new JtapiPhoneManager(new DbPhoneDAO());
                jtpaiPhoneManager.init(this);
            }
            catch (Throwable e) {
                Log.error(e);
            }

        }
    }

    protected void disablePhoneManager() {
        if(jtpaiPhoneManager == null) {
            return;
        }
        jtpaiPhoneManager.destroy();
        jtpaiPhoneManager = null;
    }

    public PhoneOption[] getJtapiOptions() {
        return null;
    }
    
    public PhoneOption[] getOptions() {
    	return new PhoneOption[]{
 				new PhoneOption("Peer",
    	    			JtapiProperties.JTAPI_PEER,
    	    			"Peer"),
    	 		new RequiredOption("Provider",
    	    			JtapiProperties.JTAPI_PROVIDER,
    	    			"Provider"),
    	    	new RequiredOption("Parameters",
						JtapiProperties.JTAPI_PARAMS,
						"Params"),
				new PhoneOption("Drop-down device selection",
						PhoneProperties.DEVICE_DROP_DOWN,
						"DropDown",
						PhoneOption.FLAG)};
    }

    public PhoneServerConfiguration getServerConfiguration() {
        return new PhoneServerConfiguration() {
            public boolean supportsMultipleServers() {
                return false;
            }

            public int getDefaultPort() {
                return PhoneConstants.DEFAULT_ASTERISK_PORT;
            }
        };
    }

    public PhoneManager getPhoneManager() {
        return jtpaiPhoneManager;
    }
    
}

