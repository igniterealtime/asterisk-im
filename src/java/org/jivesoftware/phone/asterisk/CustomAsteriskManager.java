/**
 * $RCSfile:  $
 * $Revision:  $
 * $Date:  $
 *
 * Copyright (C) 2006 Jive Software. All rights reserved.
 * This software is the proprietary information of Jive Software. Use is subject to license terms.
 */
package org.jivesoftware.phone.asterisk;

import org.asteriskjava.live.CallerId;
import org.asteriskjava.live.DefaultAsteriskServer;
import org.asteriskjava.live.ManagerCommunicationException;
import org.asteriskjava.manager.*;
import org.asteriskjava.manager.event.QueueMemberEvent;
import org.asteriskjava.manager.event.ResponseEvent;
import org.asteriskjava.manager.action.*;
import org.asteriskjava.manager.response.CommandResponse;
import org.asteriskjava.manager.response.MailboxCountResponse;
import org.asteriskjava.manager.response.ManagerError;
import org.asteriskjava.manager.response.ManagerResponse;
import org.jivesoftware.phone.*;
import org.jivesoftware.phone.queue.PhoneQueue;
import org.jivesoftware.phone.util.PhoneConstants;
import static org.jivesoftware.util.JiveGlobals.getProperty;
import org.jivesoftware.util.Log;
import org.xmpp.packet.JID;

import java.io.IOException;
import java.util.*;

/**
 *
 */
public class CustomAsteriskManager extends DefaultAsteriskServer {

    private ManagerConnection connection;
    private String hostname;
    private int port;
    private String username;
    private String password;

    public CustomAsteriskManager(String hostname, int port, String username,
                                 String password)
    {
        this.hostname = hostname;
        this.port = port;
        this.username = username;
        this.password = password;
    }

    @Override
    public void setManagerConnection(ManagerConnection connection) {
        super.setManagerConnection(connection);
        this.connection = connection;
    }

    public ManagerConnection getManagerConnection() {
        return connection;
    }

    public void addEventHandler(ManagerEventListener asteriskEventHandler) {
        connection.addEventListener(asteriskEventHandler);
    }

    public void logon() throws TimeoutException, IOException, AuthenticationFailedException,
            ManagerCommunicationException
    {
        connection = new DefaultManagerConnection(hostname, port, username, password);
        super.setManagerConnection(connection);
        super.initialize();
    }

    public void logoff() throws TimeoutException, IOException {
        connection.logoff();
    }

    public MailboxStatus getMailboxStatus(String mailbox) throws PhoneException {
        MailboxCountAction action = new MailboxCountAction();
        action.setMailbox(mailbox);

        try {
            ManagerResponse managerResponse = handleAction(action);

            if (managerResponse instanceof MailboxCountResponse) {
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
        catch (Exception e) {
            throw new PhoneException(e);
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
            CallerId callerID = new CallerId(originatingDevice.getCallerId() != null ?
                    originatingDevice.getCallerId() :
                    getProperty(PhoneProperties.DEFAULT_CALLER_ID, ""),
                    originatingDevice.getExtension());

            // fix leading spaces and + signs
            targetExtension = targetExtension.replaceAll("\\s+", "");
            targetExtension = targetExtension.replaceAll("^\\+", "00");

            String context = getProperty(PhoneProperties.CONTEXT, PhoneConstants.DEFAULT_CONTEXT);
            if ("".equals(context)) {
                context = PhoneConstants.DEFAULT_CONTEXT;
            }

            String variables = getProperty(PhoneProperties.DIAL_VARIABLES, "").trim();

            //noinspection unchecked
            Map<String, String> varMap = Collections.EMPTY_MAP;
            if (variables != null && !"".equals(variables)) {
                String[] varArray = variables.split(",");

                varMap = new HashMap<String, String>();
                for (String aVarArray : varArray) {
                    String[] s = aVarArray.split("=");
                    String key = s[0].trim();
                    String value = s[1].trim();
                    varMap.put(key, value);
                }
            }
            originateToExtension(originatingDevice.getDevice(),
                    context, targetExtension, 1, 5000, callerID, varMap);
        }
        catch (Exception e) {
            throw new PhoneException("Unabled to dial extention " + targetExtension, e);
        }
    }


    public void forward(CallSession phoneSession, String username, String extension, JID jid)
            throws PhoneException
    {
        phoneSession.setForwardedExtension(extension);
        phoneSession.setForwardedJID(jid);

        RedirectAction action = new RedirectAction();

        // The channel should be the person that called us
        action.setChannel(phoneSession.getLinkedChannel());
        action.setExten(extension);
        action.setPriority(1);

        String context = getProperty(PhoneProperties.CONTEXT, PhoneConstants.DEFAULT_CONTEXT);
        if ("".equals(context)) {
            context = PhoneConstants.DEFAULT_CONTEXT;
        }

        action.setContext(context);
        handleAction(action);
    }

    public void pauseMemberInQueue(String deviceName) throws PhoneException {
        QueuePauseAction pauseAction = new QueuePauseAction(deviceName, true);
        handleAction(pauseAction);
    }

    public void unpauseMemberInQueue(String deviceName) throws PhoneException {
        QueuePauseAction pauseAction = new QueuePauseAction(deviceName, false);
        handleAction(pauseAction);
    }

    private ManagerResponse handleAction(ManagerAction managerAction) throws PhoneException {
        ManagerResponse managerResponse;
        try {
            managerResponse = connection.sendAction(managerAction);
        }
        catch (Exception e) {
            throw new PhoneException("Error executing manager action", e);
        }

        if (managerResponse instanceof ManagerError) {
            throw new PhoneException(managerResponse.getMessage());
        }

        return managerResponse;
    }

    public Collection<PhoneQueue> getQueueMembers() throws PhoneException {
        QueueStatusAction queueAction = new QueueStatusAction();
        ResponseEvents events;
        try {
            events = connection.sendEventGeneratingAction(queueAction, 2000);
        }
        catch (IOException e) {
            throw new PhoneException(e);
        }
        catch (EventTimeoutException e) {
            throw new PhoneException(e);
        }

        if(events.getResponse() instanceof ManagerError) {
            throw new PhoneException(events.getResponse().getMessage());
        }

        //noinspection unchecked
        Collection<ResponseEvent> response = events.getEvents();
        Map<String, PhoneQueue> queueMap = new HashMap<String, PhoneQueue>();
        for(ResponseEvent event : response) {
            if(!(event instanceof QueueMemberEvent)) {
                continue;
            }
            QueueMemberEvent member = (QueueMemberEvent)event;
            String queueName = member.getQueue();
            PhoneQueue queue = queueMap.get(queueName);
            if(queue == null) {
                queue = new PhoneQueue(queueName);
                queueMap.put(queueName, queue);
            }

            queue.addDevice(member.getLocation());
        }

        return queueMap.values();
    }
}
