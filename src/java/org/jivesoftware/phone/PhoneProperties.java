/**
 * Copyright (C) 1999-2004 Jive Software. All rights reserved.
 * Copyright (C) 2006 headissue GmbH; Jens Wilke. All rights reserved.
 *
 * This software is published under the terms of the GNU Public License (GPL),
 * a copy of which is included in this distribution.
 */
 
 package org.jivesoftware.phone;

public class PhoneProperties {

    /**
     * The asterisk manager server to connect to
     */
    public static final String SERVER = "asterisk.manager.server";

    /**
     * The port of the asterisk manager server to connect to
     */
    public static final String PORT = "asterisk.manager.port";

    /**
     * The username to use when connection to the asterisk manager server
     */
    public static final String USERNAME = "asterisk.manager.username";

    /**
     * Password to use to connect to the asterisk  manager server
     */
    public static final String PASSWORD = "asterisk.manager.password";

    /**
     * Whether or not the plugin is enabled
     */
    public static final String ENABLED = "asterisk.manager.enabled";

    /**
     * The context that we are dialing through with asterisk (ie, from sip)
     */
    public static final String CONTEXT = "asterisk.manager.context";

    /**
     * The default caller id to use if none has been specified for a user
     */
    public static final String DEFAULT_CALLER_ID = "asterisk.manager.defaultCallerID";

    /**
     * Whether or not to use the device drop down
     */
    public static final String DEVICE_DROP_DOWN = "asterisk.manager.userDeviceDropDown";

    /**
     * Variables that are used when executing the dial command (orginate)
     */
    public static final String DIAL_VARIABLES = "asterisk.manager.dialVariables";
	
}
