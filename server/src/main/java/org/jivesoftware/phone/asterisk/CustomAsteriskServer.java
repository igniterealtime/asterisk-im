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
import org.asteriskjava.manager.event.*;
import org.asteriskjava.manager.action.*;
import org.asteriskjava.manager.response.*;
import org.jivesoftware.phone.*;
import org.jivesoftware.phone.queue.PhoneQueue;
import org.jivesoftware.phone.util.PhoneConstants;
import static org.jivesoftware.util.JiveGlobals.getProperty;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.JID;

import java.io.IOException;
import java.util.*;

/**
 *
 */
public class CustomAsteriskServer extends DefaultAsteriskServer {

    private static final Logger Log = LoggerFactory.getLogger(CustomAsteriskServer.class);

    /**
     * The phrase Asterisk uses to report a dialplan context that does not exist, as in
     * "There is no existence of 'from-internal' context". Verified against Asterisk 16, 18, 20, 22 and 23.
     */
    private static final String NO_SUCH_CONTEXT = "no existence of";

    /**
     * How Asterisk reports a device it does not have. chan_sip and chan_iax2 answer "Peer <name>
     * not found."; res_pjsip answers "Unable to find object <name>.". Verified against Asterisk
     * 16 to 23.
     */
    private static final String[] NO_SUCH_DEVICE = {"not found.", "Unable to find object"};

    /**
     * Asterisk's reply when the module that would answer is not loaded, which says nothing about
     * whether the device exists.
     */
    private static final String NO_SUCH_COMMAND = "No such command";

    private static final String SIP_TECHNOLOGY = "sip";
    private static final String IAX2_TECHNOLOGY = "iax2";
    private static final String PJSIP_TECHNOLOGY = "pjsip";

    private String hostname;
    private int port;
    private String username;
    private String password;

    public CustomAsteriskServer(String hostname, int port, String username,
                                 String password)
    {
        this.hostname = hostname;
        this.port = port;
        this.username = username;
        this.password = password;
    }

    public void addEventHandler(ManagerEventListener asteriskEventHandler) {
        getManagerConnection().addEventListener(asteriskEventHandler);
    }

    public void logon() throws TimeoutException, IOException, AuthenticationFailedException,
            ManagerCommunicationException
    {
        final ManagerConnection connection;
        connection = new DefaultManagerConnection(hostname, port, username, password);
        super.setManagerConnection(connection);
        super.initialize();
    }

    public void logoff() throws TimeoutException, IOException {
        getManagerConnection().logoff();
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
    public List<String> getDevices() throws PhoneException {
        ArrayList<String> list = new ArrayList<>();

        // chan_sip is deprecated as of Asterisk 17 and was removed in Asterisk 21; chan_iax2 is
        // optional. Each technology is therefore probed before it is queried, so that a server
        // that lacks it simply contributes no devices instead of failing the whole lookup.
        if (isChannelAvailable(SIP_TECHNOLOGY)) {
            list.addAll(getDevices(SIP_TECHNOLOGY));
        }
        if (isChannelAvailable(IAX2_TECHNOLOGY)) {
            list.addAll(getDevices(IAX2_TECHNOLOGY));
        }
        if (isChannelAvailable(PJSIP_TECHNOLOGY)) {
            list.addAll(getPJSIPDevices());
        }

        return list;
    }

    public boolean isChannelAvailable(String technology) throws PhoneException {
        Log.debug("Verify if Asterisk server supports channel '{}'.", technology);
        try {
            CommandAction action = new CommandAction();
            action.setCommand("core show channeltype " + technology);

            ManagerResponse managerResponse = getManagerConnection().sendAction(action);
            if (managerResponse instanceof ManagerError) {
                Log.debug("Manager response indicates that channel '{}' is not supported.", technology);
                return false;
            }

            Log.debug("Manager response indicates that channel '{}' is supported.", technology);
            return true;
        } catch (Exception e) {
            throw new PhoneException(e);
        }
    }

    /**
     * Reports whether this Asterisk server has a given device.
     *
     * <p>The device is looked up by name rather than by listing every device the server has, so
     * that the check costs the same on a deployment with five phones and one with five thousand.
     *
     * @param device a device in the '&lt;technology&gt;/&lt;name&gt;' form the plugin stores, such
     *               as "PJSIP/2001".
     * @return true when the server has it, false when the server reports that it does not, and
     *         null when the question could not be answered, including for a technology this
     *         plugin does not know how to ask about.
     */
    public Boolean isDeviceAvailable(String device) {
        if (device == null) {
            return null;
        }

        int separator = device.indexOf('/');
        if (separator < 1 || separator == device.length() - 1) {
            return null;
        }

        String technology = device.substring(0, separator);
        String name = device.substring(separator + 1);

        final String command;
        if (SIP_TECHNOLOGY.equalsIgnoreCase(technology)) {
            command = "sip show peer " + name;
        }
        else if (IAX2_TECHNOLOGY.equalsIgnoreCase(technology)) {
            command = "iax2 show peer " + name;
        }
        else if (PJSIP_TECHNOLOGY.equalsIgnoreCase(technology)) {
            command = "pjsip show endpoint " + name;
        }
        else {
            // DAHDI, Local and the rest: the plugin has no way to ask, so it does not judge.
            Log.debug("No way to look up a '{}' device; not checking '{}'.", technology, device);
            return null;
        }

        Log.debug("Verify if Asterisk server has device '{}'.", device);
        try {
            CommandAction action = new CommandAction();
            action.setCommand(command);

            // As with a missing dialplan context, a lookup that finds nothing fails the CLI
            // command, so the answer arrives as an Error whose output carries the reason.
            ManagerResponse managerResponse = getManagerConnection().sendAction(action);
            String output = managerResponse.getOutput();
            Log.trace("Device lookup for '{}' answered {}: {}", device,
                    managerResponse.getResponse(), output);

            if (output == null || output.isEmpty()) {
                return null;
            }

            if (output.contains(NO_SUCH_COMMAND)) {
                // The channel driver is not loaded, so this server cannot answer either way.
                return null;
            }

            for (String marker : NO_SUCH_DEVICE) {
                if (output.contains(marker)) {
                    return false;
                }
            }

            return managerResponse instanceof ManagerError ? null : true;
        } catch (Exception e) {
            Log.debug("Unable to look up device '{}'.", device, e);
            return null;
        }
    }

    /**
     * Reports whether a dialplan context exists on this Asterisk server.
     *
     * <p>An originate into a context that does not exist fails, and the failure is easy to
     * mistake for the plugin not working, so the administrator is better told when the value
     * they have configured does not name a real context.
     *
     * @param context the dialplan context to look for, such as "from-internal".
     * @return true when the context exists, false when Asterisk reports that it does not, and
     *         null when the question could not be answered.
     */
    public Boolean isContextAvailable(String context) {
        Log.debug("Verify if Asterisk server has dialplan context '{}'.", context);
        try {
            CommandAction action = new CommandAction();
            action.setCommand("dialplan show " + context);

            // A context that does not exist makes the CLI command fail, so Asterisk answers with
            // an Error rather than a Success. The command output is carried either way, and it is
            // the output that distinguishes "no such context" from any other reason the command
            // did not run, such as the AMI account lacking the 'command' permission.
            ManagerResponse managerResponse = getManagerConnection().sendAction(action);
            String output = managerResponse.getOutput();
            Log.trace("Dialplan lookup for '{}' answered {}: {}", context,
                    managerResponse.getResponse(), output);

            if (output != null && output.contains(NO_SUCH_CONTEXT)) {
                return false;
            }

            if (managerResponse instanceof ManagerError) {
                Log.debug("Unable to inspect the dialplan: {}", managerResponse.getMessage());
                return null;
            }

            // A successful listing that says nothing about a missing context is the positive
            // answer. Empty output means the question went unanswered rather than answered "no".
            return output == null || output.isEmpty() ? null : true;
        } catch (Exception e) {
            Log.debug("Unable to inspect the dialplan for context '{}'.", context, e);
            return null;
        }
    }

    /**
     * Returns a list of all devices configured on this Asterisk server for the given
     * technology.
     *
     * @param technology A channel name, such as "sip" or "iax2".
     * @return a list of devices, that is Asterisk channel names that can be mapped to phone users.
     * @throws PhoneException if the list can't be retrieved.
     */
    public List<String> getDevices(String technology) throws PhoneException {
        Log.debug("Get '{}' devices.", technology);
        try {
            CommandAction action = new CommandAction();
            action.setCommand(technology.toLowerCase() + " show peers");

            ManagerResponse managerResponse = getManagerConnection().sendAction(action);
            if (managerResponse instanceof ManagerError) {
                Log.warn("Manager response that was an error while trying to get '{}' devices: {}", technology, managerResponse.getMessage());
                throw new PhoneException(managerResponse.getMessage());
            }

            CommandResponse response = (CommandResponse) managerResponse;
            List<String> results = response.getResult();
            Log.debug("Response has {} result line(s).", results != null ? results.size() : "no");
            ArrayList<String> list = new ArrayList<String>();

            if (results.size() < 2) {
                return list;
            }

            /*
             * The result will look like this:
             *
             * for older versions of Asterisk:
             *
             * Name/username              Host            Dyn Nat ACL Port     Status
             * 1313/1313                  10.13.0.61       D       A  5061     OK (192 ms)
             * 1312/1312                  10.13.0.61       D       A  5061     OK (183 ms)
             * 1303/1303                  (Unspecified)    D   N      0        UNKNOWN
             * 1302/1302                  (Unspecified)    D       A  0        UNKNOWN
             * <some footer summary>
             *
             * For newer versions of Asterisk:
             * Name/username             Host                                    Dyn Forcerport Comedia    ACL Port     Status      Description
             * 701                       (Unspecified)                            D  No         No          A  0        UNKNOWN     Guus
             * 1 sip peers [Monitored: 0 online, 1 offline Unmonitored: 0 online, 0 offline]
             */

            // Trim header and footer
            results = results.subList(1, results.size() - 1);
            for (String result : results) {
                Log.debug(result);
                result = result.trim();
                if (!result.contains(" ")) {
                    continue;
                }
                result = result.substring(0, result.indexOf(" "));
                if (result.contains("/")) {
                    list.add(technology.toUpperCase() + "/" + result.substring(0, result.indexOf("/")));
                } else {
                    list.add(technology.toUpperCase() + "/" + result);
                }
            }

            return list;
        }
        catch (Exception e) {
            throw new PhoneException(e);
        }
    }

    /**
     * Returns a list of all devices configured on this Asterisk server for the pjsip channel.
     *
     * @return a list of devices, that is Asterisk channel names that can be mapped to phone users.
     * @throws PhoneException if the list can't be retrieved.
     */
    public List<String> getPJSIPDevices() throws PhoneException {
        Log.debug("Get PJSIP Devices");
        final List<String> result = new ArrayList<>();
        try {
            final PJSipShowEndpointsAction action = new PJSipShowEndpointsAction();
            final ResponseEvents responseEvents = getManagerConnection().sendEventGeneratingAction(action);
            final Collection<ResponseEvent> events = responseEvents.getEvents();
            Log.trace("Received {} event(s).", events.size());
            events.forEach(responseEvent -> {
                if (responseEvent instanceof EndpointList) {
                    EndpointList event = (EndpointList) responseEvent;
                    Log.trace("Received: {}, {}, {}, {}", event.getEvent(), event.getObjectName(), event.getObjectType(), event.getAor());
                    if ("endpoint".equalsIgnoreCase(event.getObjectType())) {
                        // Asterisk names PJSIP channels 'PJSIP/<endpoint>-<uniqueid>'. The device
                        // identifier must be the 'PJSIP/<endpoint>' part, as that is what
                        // AsteriskUtil#getDevice derives from a channel name, and what Originate
                        // expects. Note that the endpoint name (ObjectName) is what appears in the
                        // channel name; the AOR merely happens to share that name in most configs.
                        result.add(PJSIP_TECHNOLOGY.toUpperCase() + "/" + event.getObjectName());
                    }
                } else if (responseEvent instanceof EndpointListComplete) {
                    Log.trace("Completed {} for list {}", ((EndpointListComplete) responseEvent).getListItems(), ((EndpointListComplete) responseEvent).getEventList());
                } else {
                    Log.debug("Unknown response type: {}", responseEvent);
                }
            });
        } catch (IOException | EventTimeoutException e) {
            throw new PhoneException("Exception occurred while trying to get PJSIP devices.", e);
        }

        return result;
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

			String firstleg_timeout = getProperty(PhoneProperties.FIRSTLEG_TIMEOUT, PhoneConstants.DEFAULT_FIRSTLEG_TIMEOUT);
            if ("".equals(firstleg_timeout)) {
                firstleg_timeout = PhoneConstants.DEFAULT_FIRSTLEG_TIMEOUT;
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
                    context, targetExtension, 1, Integer.parseInt(firstleg_timeout), callerID, varMap);
        }
        catch (Exception e) {
            throw new PhoneException("Unable to dial extention " + targetExtension + ": " + e.getMessage(), e);
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
            managerResponse = getManagerConnection().sendAction(managerAction);
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
            events = getManagerConnection().sendEventGeneratingAction(queueAction, 2000);
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
