/**
 * $RCSfile: AsteriskPhoneManager.java,v $
 * $Revision: 1.13 $
 * $Date: 2005/07/02 00:22:51 $
 *
 * Copyright (C) 1999-2004 Jive Software. All rights reserved.
 *
 * This software is the proprietary information of Jive Software. Use is subject to license terms.
 */
package org.jivesoftware.phone.asterisk;

import net.sf.asterisk.manager.*;
import net.sf.asterisk.manager.action.CommandAction;
import net.sf.asterisk.manager.action.MailboxCountAction;
import net.sf.asterisk.manager.action.RedirectAction;
import net.sf.asterisk.manager.response.CommandResponse;
import net.sf.asterisk.manager.response.MailboxCountResponse;
import net.sf.asterisk.manager.response.ManagerError;
import net.sf.asterisk.manager.response.ManagerResponse;
import org.jivesoftware.phone.*;
import org.jivesoftware.phone.database.PhoneDAO;
import org.jivesoftware.phone.element.PhoneEvent;
import org.jivesoftware.phone.util.PhoneConstants;
import org.jivesoftware.util.JiveGlobals;
import static org.jivesoftware.util.JiveGlobals.getProperty;
import org.jivesoftware.util.Log;
import org.xmpp.component.ComponentManagerFactory;
import org.xmpp.packet.JID;
import org.xmpp.packet.Message;
import org.xmpp.packet.Packet;

import java.io.IOException;
import java.util.*;


/**
 * Asterisk dependent implementation of {@link PhoneManager}
 *
 * @author Andrew Wright
 * @since 1.0
 */
@PBXInfo(make = "Asterisk", version = "1.2")
public class AsteriskPhoneManager extends BasePhoneManager {

    private ManagerConnection con;
    private DefaultAsteriskManager asteriskManager;
//    private ChannelStatusRunnable channelStatusRunnable;
    AsteriskPlugin plugin;

    public AsteriskPhoneManager(PhoneDAO dao) {
        super(dao);
    }

    public void init(AsteriskPlugin plugin) throws TimeoutException, IOException, AuthenticationFailedException {


        Log.info("Initializing Asterisk Manager connection");

        // Populate the manager configuration
        String server = JiveGlobals.getProperty(PhoneProperties.SERVER);
        String username = JiveGlobals.getProperty(PhoneProperties.USERNAME);
        String password = JiveGlobals.getProperty(PhoneProperties.PASSWORD);
        int port = JiveGlobals.getIntProperty(PhoneProperties.PORT, 5038);

        // Check to see if the configuration is valid then
        // Initialize the manager connection pool and create an eventhandler
        if (server != null && username != null && password != null) {

            try {

                if (con != null) {
                    con.logoff();
                }

                con = new DefaultManagerConnection(server, port, username, password);
                con.addEventHandler(new AsteriskEventHandler(this, plugin));
                asteriskManager = new DefaultAsteriskManager(con);

            }
            catch (Throwable e) {
                Log.error("unable to obtain a manager connection --> " + e.getMessage(), e);
            }

        }
        else {
            Log.warn("AsteriskPlugin configuration is invalid, please see admin tool!!");
        }


        this.plugin = plugin;
        asteriskManager.initialize();
        // Start handling events
        // channelStatusRunnable = new ChannelStatusRunnable();
        // new Thread(channelStatusRunnable).start();
    }

    public void destroy() {

//        if (channelStatusRunnable != null) {
//            channelStatusRunnable.shouldRun = false;
//        }


        Log.debug("Shutting down Manager connection");
        try {
            con.logoff();
        }
        catch (Throwable e) {
            // Make sure we catch all exceptions show we can Log anything that might be
            // going on
            Log.error(e.getMessage(), e);
            ComponentManagerFactory.getComponentManager().getLog().error(e);
        }
    }

// FIXME what is this? ;jw
//    public void stopMonitor(String channel) throws PhoneException {
//
//        StopMonitorAction action = new StopMonitorAction();
//        action.setChannel(channel);
//
//        try {
//
//            ManagerResponse managerResponse = con.sendAction(action);
//
//            if (managerResponse instanceof ManagerError) {
//                Log.warn(managerResponse.getMessage());
//                throw new PhoneException(managerResponse.getMessage());
//            }
//
//        }
//        catch (PhoneException pe) {
//            throw pe;
//        }
//        catch (Exception e) {
//            Log.error(e.getMessage(), e);
//            throw new PhoneException(e.getMessage());
//        }
//
//    }

    public MailboxStatus mailboxStatus(String mailbox) throws PhoneException {

        MailboxCountAction action = new MailboxCountAction();
        action.setMailbox(mailbox);

        try {

            ManagerResponse managerResponse = con.sendAction(action);

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

    public List<String> getDevices() throws PhoneException {
        List<String> devices = getSipDevices();

        Collections.sort(devices);

        // todo Add IAX support
        return devices;
    }

    public Map getStatus() throws PhoneException {
        return asteriskManager.getChannels();
    }

    @SuppressWarnings({"unchecked"})
    protected List<String> getSipDevices() throws PhoneException {

        try {

            CommandAction action = new CommandAction();
            action.setCommand("sip show peers");

            ManagerResponse managerResponse = con.sendAction(action);
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
                list.remove(list.size() - 1); // Remove the last entry, it just tells how many are online
            }

            return list;

        }
        catch (Exception e) {
            Log.error(e.getMessage(), e);
            throw new PhoneException(e);
        }

    }

    public void dial(String username, String extension, JID jid) throws PhoneException {

        //acquire the jidUser object
        PhoneUser user = getPhoneUserByUsername(username);

        try {

            PhoneDevice primaryDevice = getPrimaryDevice(user.getID());

            Originate action = new Originate();
            action.setChannel(primaryDevice.getDevice());
            action.setCallerId(primaryDevice.getCallerId() != null ? primaryDevice.getCallerId() :
                    getProperty(PhoneProperties.DEFAULT_CALLER_ID, ""));

            // fix leading spaces and + signs
            extension = extension.replaceAll("\\s+", "");
            extension = extension.replaceAll("^\\+", "00");

            action.setExten(extension);
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

            asteriskManager.originateCall(action);

        }
        catch (Exception e) {
            Log.error(e.getMessage(), e);
            throw new PhoneException("Unabled to dial extention " + extension, e);
        }


    }

    public boolean isConnected() {
        return con.isConnected();
    }

    public boolean isReady() {
        return plugin.isComponentReady();
    }

    public void sendHangupMessage(String callSessionID, String device, String username) {
        Message message = new Message();
        message.setID(callSessionID);

        PhoneEvent phoneEvent = new PhoneEvent(callSessionID, PhoneEvent.Type.HANG_UP, device);
        message.getElement().add(phoneEvent);
        plugin.sendPacket2User(username, message);
    }

    public void sendPacket(Packet packet) {
        plugin.sendPacket(packet);
    }

    public void forward(String callSessionID, String username, String extension, JID jid) throws PhoneException {


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
            ManagerResponse managerResponse = con.sendAction(action);


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

// FIXME: check whether we really need this ;jw
//
//    /**
//     * Peforms a cleanup on any sessions that have been left open that shouldn't be
//     */
//    private class ChannelStatusRunnable implements Runnable {
//
//        boolean shouldRun = true;
//        long lastRun = -1;
//        static final long PERIOD = JiveConstants.MINUTE * 2;
//
//        public void run() {
//
//            while (shouldRun) {
//
//                if (lastRun + PERIOD > System.currentTimeMillis()) {
//
//                    Map channels = asteriskManager.getChannels();
//
//                    for (Object o : channels.entrySet()) {
//                        Map.Entry entry = (Map.Entry) o;
//
//                        String uniqueID = (String) entry.getKey();
//                        Channel channel = (Channel) entry.getValue();
//
//
//                        CallSession callSession = CallSessionFactory.getCallSessionFactory().getCallSession(uniqueID);
//
//                        // The channel is not up 
//                        if (!ChannelStateEnum.UP.equals(channel.getState()) && callSession != null) {
//
//                            Log.debug("AsteriskPhoneManger.ChannelStatusRunnable: User " + callSession.getUsername() +
//                                    " has no more call sessions, but his presence is " +
//                                    "still ON_PHONE. Changing to AVAILABLE");
//                            CallSessionFactory.getCallSessionFactory().destroyPhoneSession(uniqueID);
//                            if (!restoreUserPresence(callSession.getUsername())) {
//                                // TODO Remove this code when the "always on-the-phone problem is fixed"
//                                // Check if the user is available and his presence is still
//                                // on-the-phone (and no there are no more calls)
//                                SessionManager sessionManager = XMPPServer.getInstance().getSessionManager();
//                                Collection<ClientSession> sessions = sessionManager.getSessions(callSession.getUsername());
//                                for (ClientSession session : sessions) {
//                                    Presence presence = session.getPresence();
//                                    Element phoneStatusElement = presence.getElement().element("phone-status");
//                                    // If the phone-status attribute exists check to see if the status is avaialbable
//                                    if (phoneStatusElement != null &&
//                                            PhoneStatus.Status.ON_PHONE.name().equals(phoneStatusElement.attributeValue("status")))
//                                    {
//                                        Log.debug("Asterisk-IM HangupTask: User " + callSession.getUsername() +
//                                                " has no more call sessions, but his presence is " +
//                                                "still ON_PHONE. Changing to AVAILABLE");
//                                        // Change presence to available since there are no more active calls
//                                        phoneStatusElement.addAttribute("status", PhoneStatus.Status.AVAILABLE.name());
//                                        getInstance().getPresenceRouter().route(presence);
//                                    }
//                                }
//
//                            }
//                        }
//
//                    }
//                }
//
//                try {
//                    Thread.sleep(5 * JiveConstants.SECOND);
//                }
//                catch (InterruptedException e) {
//                    // Not really a big deal if this is interrupted
//                }
//
//            }
//
//        }
//
// }

}
