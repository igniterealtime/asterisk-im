/**
 * Copyright (C) 1999-2004 Jive Software. All rights reserved.
 *
 * This software is the proprietary information of Jive Software. Use is subject to license terms.
 */
package org.jivesoftware.phone.asterisk;

/**
 * @author Andrew Wright
 */
public final class AsteriskUtil {

    private AsteriskUtil() {

    }

    /**
     * Strips the hyphen out of fullChannel names. Asterisk will pass fullChannel names such as
     * SIP/6131-53f. The -53f is unique per call, not per user. So will strip this part out
     * to get the correct fullChannel.
     *
     * @param fullChannel full Channel
     * @return the fullChannel with out the final hyphen section
     */
    public static String getDevice(String fullChannel) {
        return fullChannel.split("-")[0];
    }


}
