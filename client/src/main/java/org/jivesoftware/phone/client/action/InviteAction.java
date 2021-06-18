/**
 * Copyright (C) 1999-2004 Jive Software. All rights reserved.
 *
 * This software is the proprietary information of Jive Software. Use is subject to license terms.
 */
package org.jivesoftware.phone.client.action;

/**
 * Used to invite a third party into the call.
 * <p/>
 * Currently this does not behave in a way I would prefer. It will cause both channels to be forwarded to a device,
 * making it look like the device is receiving two calls.
 * <p/>
 * This action works most effectively if you wish to transfer the call to a password free conference room.
 *
 * @author Andrew Wright
 */
public class InviteAction extends PhoneActionPacket {

    private String extension;

    public InviteAction() {
    }

    /**
     * Constructs a new instance of this action.
     *
     * @param callID    The current call session id (call we are in)
     * @param extension The extension we want to invite. Can be a full phone number or local extension
     */
    public InviteAction(String callID, String extension) {
        super(callID);
        this.extension = extension;
    }

    /**
     * Returns {@link ActionType#INVITE}
     *
     * @return {@link ActionType#INVITE}
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
