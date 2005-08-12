/**
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
public class ForwardAction extends PhoneActionPacket {

    private String extension;

    public ForwardAction() {
    }

    /**
     * Constructs a new instance of this action.
     *
     * @param callID The current call session id (call we are in)
     * @param extension The extension we want to forward. Can be a full phone number or local extension
     */
    public ForwardAction(String callID, String extension) {
        super(callID);
        this.extension = extension;
    }

    /**
     * Returns {@link ActionType.FORWARD}
     *
     * @return {@link ActionType.FORWARD}
     */
    public ActionType getActionType() {
        return ActionType.FORWARD;
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
