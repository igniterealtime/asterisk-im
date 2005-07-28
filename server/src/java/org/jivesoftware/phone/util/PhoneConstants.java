/**
 * $RCSfile: PhoneConstants.java,v $
 * $Revision: 1.9 $
 * $Date: 2005/06/21 18:54:22 $
 *
 * Copyright (C) 1999-2004 Jive Software. All rights reserved.
 *
 * This software is the proprietary information of Jive Software. Use is subject to license terms.
 */
package org.jivesoftware.phone.util;

/**
 * @author Andrew Wright
 */
public interface PhoneConstants {

    /**
     * namespace for the phone schema
     */
    public static final String NAMESPACE = "http://jivesoftware.com/xmlns/phone";


    /**
     * If no context is specified use this context
     */
    public static final String DEFAULT_CONTEXT = "from-sip";


    /**
     * Jive property keys this plugin expects
     */
    public static class Properties {

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
         * How many manager server connections we should make
         */
        public static final String POOLSIZE = "asterisk.manager.poolsize";

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


        private Properties() {
        }
    }

}
