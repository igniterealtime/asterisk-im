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

/**
 * @author Andrew Wright
 */
public class CallSession {

    private String id;
    private String channel;
    private String linkedChannel;

    CallSession(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
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


}
