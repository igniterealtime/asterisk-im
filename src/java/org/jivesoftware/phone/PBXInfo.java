package org.jivesoftware.phone;

/**
 * Used to specify PBX information on pbx specific implementation classes.
 *
 * @author Andrew Wright
 */
public @interface PBXInfo {

    /**
     * The make of the pbx (ie asterisk)
     */
    String make();

    /**
     * org.jivesoftware.phone.Version of the pbx
     */
    String version();
}
