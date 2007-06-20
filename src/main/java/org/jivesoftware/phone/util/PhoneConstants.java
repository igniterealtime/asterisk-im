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
     * JiveID sequence type for Phone Devices
     */
    public static final int DEVICE_SEQUENCE = 100;

    /**
     * JiveID sequence type for Phone Servers
     */
    public static final int SERVER_SEQUENCE = 101;

    /**
     * The default port that asterisk servers run on.
     */
    int DEFAULT_ASTERISK_PORT = 5038;

    /**
     * The default timeout for the first leg of the call
     */
    public static final String DEFAULT_FIRSTLEG_TIMEOUT = "30000";
}
