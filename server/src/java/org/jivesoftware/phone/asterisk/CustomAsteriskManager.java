/**
 * $RCSfile:  $
 * $Revision:  $
 * $Date:  $
 *
 * Copyright (C) 2006 Jive Software. All rights reserved.
 * This software is the proprietary information of Jive Software. Use is subject to license terms.
 */
package org.jivesoftware.phone.asterisk;

import net.sf.asterisk.manager.*;
import net.sf.asterisk.manager.response.ManagerResponse;
import net.sf.asterisk.manager.response.ManagerError;
import net.sf.asterisk.manager.response.CommandResponse;
import net.sf.asterisk.manager.response.MailboxCountResponse;
import net.sf.asterisk.manager.action.CommandAction;
import net.sf.asterisk.manager.action.MailboxCountAction;
import net.sf.asterisk.manager.action.RedirectAction;

import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

import org.jivesoftware.phone.*;
import org.jivesoftware.phone.util.PhoneConstants;
import org.jivesoftware.util.Log;
import static org.jivesoftware.util.JiveGlobals.getProperty;
import org.xmpp.packet.JID;

/**
 *
 */
public class CustomAsteriskManager extends DefaultAsteriskManager {

    private ManagerConnection connection;

    public CustomAsteriskManager(ManagerConnection connection) {
        super(connection);
        this.connection = connection;
    }

    @Override
    public void setManagerConnection(ManagerConnection connection) {
        super.setManagerConnection(connection);
        this.connection = connection;
    }

    public ManagerConnection getManagerConnection() {
        return connection;
    }

    public void logoff() throws TimeoutException, IOException {
        connection.logoff();
    }

    public MailboxStatus getMailboxStatus(String mailbox) throws PhoneException {

        MailboxCountAction action = new MailboxCountAction();
        action.setMailbox(mailbox);

        try {

            ManagerResponse managerResponse = connection.sendAction(action);

            if (managerResponse instanceof ManagerError) {
                Log.warn(managerResponse.getMessage());
                throw new PhoneException(managerResponse.getMessage());
            }
            else if (managerResponse instanceof MailboxCountResponse) {
                MailboxCountResponse mailboxStatus = (MailboxCountResponse) managerResponse;
                int oldMessages = mailboxStatus.getOldMessages();
                int newMessages = mailboxStatus.getNewMessages();
                return new MailboxStatus(mailbox, oldMessages, newMessages);
            }
            else {
                Log.error("Did not receive a MailboxCountResponseEvent!");
                throw new PhoneException("Did not receive a MailboxCountResponseEvent!");
            }

        }
        catch (PhoneException pe) {
            throw pe;
        }
        catch (Exception e) {
            Log.error(e.getMessage(), e);
            throw new PhoneException(e.getMessage());
        }
    }

    @SuppressWarnings({"unchecked"})
    public List<String> getSipDevices() throws PhoneException {
        try {
            CommandAction action = new CommandAction();
            action.setCommand("sip show peers");

            ManagerResponse managerResponse = connection.sendAction(action);
            if (managerResponse instanceof ManagerError) {
                Log.warn(managerResponse.getMessage());
                throw new PhoneException(managerResponse.getMessage());
            }

            CommandResponse response = (CommandResponse) managerResponse;
            List<String> results = response.getResult();

            ArrayList<String> list = new ArrayList<String>();
            boolean isFirst = true; // The first entry is Name, we want to skip that one
            for (String result : results) {
                if (!isFirst) {
                    result = result.trim();
                    result = result.substring(0, result.indexOf(" "));
                    list.add("SIP/" + result.split("/")[0]);
                }
                isFirst = false;
            }
            if (list.size() > 0) {
                list.remove(list.size() - 1);   // Remove the last entry, it just tells how
                // many are online
            }

            return list;
        }
        catch (Exception e) {
            throw new PhoneException(e);
        }
    }

    public void dial(PhoneDevice originatingDevice, String targetExtension) throws PhoneException {
        try {
            Originate action = new Originate();
            action.setChannel(originatingDevice.getDevice());
            action.setCallerId(originatingDevice.getCallerId() != null ?
                    originatingDevice.getCallerId() :
                    getProperty(PhoneProperties.DEFAULT_CALLER_ID, ""));

            // fix leading spaces and + signs
            targetExtension = targetExtension.replaceAll("\\s+", "");
            targetExtension = targetExtension.replaceAll("^\\+", "00");

            action.setExten(targetExtension);
            String context = getProperty(
                    PhoneProperties.CONTEXT,
                    PhoneConstants.DEFAULT_CONTEXT);
            if ("".equals(context)) {
                context = PhoneConstants.DEFAULT_CONTEXT;
            }

            action.setContext(context);
            action.setPriority(1);

            String variables = getProperty(PhoneProperties.DIAL_VARIABLES, "").trim();

            if (variables != null && !"".equals(variables)) {

                String[] varArray = variables.split(",");

                Map<String, String> varMap = new HashMap<String, String>();
                for (String aVarArray : varArray) {
                    String[] s = aVarArray.split("=");
                    String key = s[0].trim();
                    String value = s[1].trim();
                    varMap.put(key, value);
                }

                action.setVariables(varMap);
            }

            originateCall(action);

        }
        catch (Exception e) {
            throw new PhoneException("Unabled to dial extention " + targetExtension, e);
        }
    }

    
    public void forward(String callSessionID, String username, String extension, JID jid)
            throws PhoneException
    {
        CallSession phoneSession = CallSessionFactory.getCallSessionFactory()
                .getCallSession(callSessionID, username);

        phoneSession.setForwardedExtension(extension);
        phoneSession.setForwardedJID(jid);

        RedirectAction action = new RedirectAction();

        // The channel should be the person that called us
        action.setChannel(phoneSession.getLinkedChannel());
        action.setExten(extension);
        action.setPriority(1);


        String context =
                getProperty(PhoneProperties.CONTEXT,
                        PhoneConstants.DEFAULT_CONTEXT);
        if ("".equals(context)) {
            context = PhoneConstants.DEFAULT_CONTEXT;
        }

        action.setContext(context);

        try {
            ManagerResponse managerResponse = connection.sendAction(action);


            if (managerResponse instanceof ManagerError) {
                Log.warn(managerResponse.getMessage());
                throw new PhoneException(managerResponse.getMessage());
            }

        }
        catch (PhoneException pe) {
            throw pe;
        }
        catch (Exception e) {
            Log.error(e.getMessage(), e);
            throw new PhoneException(e.getMessage());
        }
    }
}
