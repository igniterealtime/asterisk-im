/**
 * $RCSfile: CallSession.java,v $
 * $Revision: 1.2 $
 * $Date: 2005/07/01 23:56:27 $
 *
 * Copyright (C) 1999-2004 Jive Software. All rights reserved.
 *
 * This software is the proprietary information of Jive Software. Use is subject to license terms.
 */
package org.jivesoftware.phone.asterisk;

import org.xmpp.packet.JID;

/**
 * @author Andrew Wright
 */
public class CallSession {

    private final long serverID;
    private final String channelId;
    private final String username;
    
    private String channelName;
    private String linkedChannel;
    private String callerID;
    private String callerIDName;
    private String forwardedExtension;
    private JID forwardedJID;
    private JID dialedJID;
    private boolean isMonitored;
    private Status status;

    public CallSession(long serverID, String channelId, String username) {
        this.serverID = serverID;
        this.channelId = channelId;
        this.username = username;
    }

    public String getChannelId() {
        return channelId;
    }

    public String getUsername() {
        return username;
    }

    public long getServerID() {
        return serverID;
    }

    public String getChannelName() {
        return channelName;
    }

    public void setChannelName(String channelName) {
        this.channelName = channelName;
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

    public String getCallerIDName() {
        return callerIDName;
    }

    public void setCallerIDName(String callerIDName) {
        this.callerIDName = callerIDName;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final CallSession that = (CallSession) o;

        if (isMonitored != that.isMonitored) {
            return false;
        }
        if (callerID != null ? !callerID.equals(that.callerID) : that.callerID != null) {
            return false;
        }
        if (channelName != null ? !channelName.equals(that.channelName) : that.channelName != null) {
            return false;
        }
        if (dialedJID != null ? !dialedJID.equals(that.dialedJID) : that.dialedJID != null) {
            return false;
        }
        if (forwardedExtension != null ? !forwardedExtension.equals(that.forwardedExtension)
                : that.forwardedExtension != null)
        {
            return false;
        }
        if (forwardedJID != null ? !forwardedJID.equals(that.forwardedJID)
                : that.forwardedJID != null)
        {
            return false;
        }
        if (channelId != null ? !channelId.equals(that.channelId) : that.channelId != null) {
            return false;
        }
        if (linkedChannel != null ? !linkedChannel.equals(that.linkedChannel)
                : that.linkedChannel != null)
        {
            return false;
        }
        return !(username != null ? !username.equals(that.username) : that.username != null);

    }

    @Override
    public int hashCode() {
        int result;
        result = (channelId != null ? channelId.hashCode() : 0);
        result = 29 * result + (channelName != null ? channelName.hashCode() : 0);
        result = 29 * result + (linkedChannel != null ? linkedChannel.hashCode() : 0);
        result = 29 * result + (callerID != null ? callerID.hashCode() : 0);
        result = 29 * result + (forwardedExtension != null ? forwardedExtension.hashCode() : 0);
        result = 29 * result + (forwardedJID != null ? forwardedJID.hashCode() : 0);
        result = 29 * result + (dialedJID != null ? dialedJID.hashCode() : 0);
        result = 29 * result + (username != null ? username.hashCode() : 0);
        result = 29 * result + (isMonitored ? 1 : 0);
        return result;
    }


    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("CallSession");
        sb.append("{channelId='").append(channelId).append('\'');
        sb.append(", channelName='").append(channelName).append('\'');
        sb.append(", linkedChannel='").append(linkedChannel).append('\'');
        sb.append(", callerID='").append(callerID).append('\'');
        sb.append(", forwardedExtension='").append(forwardedExtension).append('\'');
        sb.append(", forwardedJID=").append(forwardedJID);
        sb.append(", dialedJID=").append(dialedJID);
        sb.append(", username='").append(username).append('\'');
        sb.append(", isMonitored=").append(isMonitored);
        sb.append('}');
        return sb.toString();
    }

    public Status getStatus() {
        return status;
    }

    protected void setStatus(Status status) {
        this.status = status;
    }

    public enum Status {
        dialed, ringing, onphone
    }
}
