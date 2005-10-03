/**
 * $RCSfile: CallSession.java,v $
 * $Revision: 1.2 $
 * $Date: 2005/07/01 23:56:27 $
 *
 * Copyright (C) 1999-2004 Jive Software. All rights reserved.
 *
 * This software is the proprietary information of Jive Software. Use is subject to license terms.
 */
package org.jivesoftware.phone;

import org.xmpp.packet.JID;

/**
 * @author Andrew Wright
 */
public class CallSession {

    private String id;
    private String channel;
    private String linkedChannel;
    private String callerID;
    private String forwardedExtension;
    private JID forwardedJID;
    private JID dialedJID;
    private String username;
    private boolean isMonitored;

    CallSession(String id, String username) {
        this.id = id;
        this.username = username;
    }

    public String getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getLinkedChannel() {
        return linkedChannel;
    }

    public void setLinkedChannel(String linkedChannel) {
        this.linkedChannel = linkedChannel;
    }

    public boolean isMonitored() {
        return isMonitored;
    }

    public void setMonitored(boolean monitored) {
        isMonitored = monitored;
    }

    public String getCallerID() {
        return callerID;
    }

    public void setCallerID(String callerID) {
        this.callerID = callerID;
    }

    public JID getDialedJID() {
        return dialedJID;
    }

    public void setDialedJID(JID dialedJID) {
        this.dialedJID = dialedJID;
    }

    public String getForwardedExtension() {
        return forwardedExtension;
    }

    public void setForwardedExtension(String forwardedExtension) {
        this.forwardedExtension = forwardedExtension;
    }

    public JID getForwardedJID() {
        return forwardedJID;
    }

    public void setForwardedJID(JID forwardedJID) {
        this.forwardedJID = forwardedJID;
    }


}
