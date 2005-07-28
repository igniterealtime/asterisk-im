/**
 * Copyright (C) 1999-2004 Jive Software. All rights reserved.
 *
 * This software is the proprietary information of Jive Software. Use is subject to license terms.
 */
package org.jivesoftware.phone.client.action;

/**
 * Used to invite a third party into the call
 *
 * @author Andrew Wright
 */
public class InviteAction extends PhoneActionPacket {

    private String extension;

    public InviteAction() {
    }

    public InviteAction(String callID, String extension) {
        super(callID);
        this.extension = extension;
    }

    /**
     * Returns {@link ActionType.INVITE}
     *
     * @return {@link ActionType.INVITE}
     */
    public ActionType getActionType() {
        return ActionType.INVITE;
    }

    /**
     * returns an extension element or a jid element depending on which was specified
     *
     * @return an extension element or a jid element, depending on which was specified
     */
    protected String getActionChildElementXML() {

        return new StringBuffer()
            .append("<extension>")
            .append(extension)
            .append("</extension>")
            .toString();
    }

}
