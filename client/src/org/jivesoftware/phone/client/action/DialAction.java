/**
 * $RCSfile: DialAction.java,v $
 * $Revision: 1.3 $
 * $Date: 2005/06/29 23:33:30 $
 *
 * Copyright (C) 1999-2004 Jive Software. All rights reserved.
 *
 * This software is the proprietary information of Jive Software. Use is subject to license terms.
 */
package org.jivesoftware.phone.client.action;

/**
 * Used to dialByExtension a user. You must set either an extension or jid in this class for
 * dialing to complete successfully.
 *
 * @author Andrew Wright
 */
public class DialAction extends PhoneActionPacket {

    private String extension;
    private String jid;

    public DialAction() {
        
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    public void setJid(String jid) {
        this.jid = jid;
    }


    /**
     * Returns {@link ActionType.DIAL}
     *
     * @return {@link ActionType.DIAL}
     */
    public ActionType getActionType() {
        return ActionType.DIAL;
    }

    /**
     * returns an extension element or a jid element depending on which was specified
     *
     * @return an extension element or a jid element, depending on which was specified
     */
    protected String getActionChildElementXML() {
        if(jid != null) {
            return new StringBuffer()
                .append("<jid>")
                .append(jid)
                .append("</jid>")
                .toString();
        } else {
            return new StringBuffer()
                .append("<extension>")
                .append(extension)
                .append("</extension>")
                .toString();
        }
    }

}
