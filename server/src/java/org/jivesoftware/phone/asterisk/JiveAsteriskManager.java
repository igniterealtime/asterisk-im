/**
 * $Revision$
 * $Date$
 *
 * Copyright (C) 1999-2005 Jive Software. All rights reserved.
 * This software is the proprietary information of Jive Software. Use is subject to license terms.
 */
package org.jivesoftware.phone.asterisk;

import net.sf.asterisk.manager.DefaultAsteriskManager;
import net.sf.asterisk.manager.ManagerConnection;
import net.sf.asterisk.manager.event.*;
import org.dom4j.Element;
import org.jivesoftware.phone.CallSession;
import org.jivesoftware.phone.CallSessionFactory;
import static org.jivesoftware.phone.CallSessionFactory.getCallSessionFactory;
import org.jivesoftware.phone.PhoneUser;
import static org.jivesoftware.phone.asterisk.AsteriskUtil.getDevice;
import org.jivesoftware.phone.element.PhoneEvent;
import org.jivesoftware.phone.element.PhoneStatus;
import org.jivesoftware.phone.util.UserPresenceUtil;
import org.jivesoftware.util.Log;
import org.jivesoftware.util.StringUtils;
import org.jivesoftware.wildfire.ClientSession;
import org.jivesoftware.wildfire.SessionManager;
import org.jivesoftware.wildfire.XMPPServer;
import static org.jivesoftware.wildfire.XMPPServer.getInstance;
import org.xmpp.packet.JID;
import org.xmpp.packet.Message;
import org.xmpp.packet.Presence;

import java.util.ArrayList;
import java.util.Collection;

/**
 *
 */
public class JiveAsteriskManager extends DefaultAsteriskManager {

    private AsteriskPhoneManager phoneManager;

    public JiveAsteriskManager(ManagerConnection connection, AsteriskPhoneManager asteriskPhoneManager) {
        super(connection);
        this.phoneManager = asteriskPhoneManager;
    }

    @Override
    protected void handleDisconnectEvent(DisconnectEvent disconnectEvent) {
        super.handleDisconnectEvent(disconnectEvent);
    }


    @Override
    protected void handleHangupEvent(HangupEvent event) {
        super.handleHangupEvent(event);
        handleHangup(event);
    }

    @Override
    protected void handleNewExtenEvent(NewExtenEvent event) {
        super.handleNewExtenEvent(event);

        if ("Dial".equals(event.getApplication())) {
            handleDial(event);
        }
    }

    @Override
    protected void handleNewStateEvent(NewStateEvent event) {
        super.handleNewStateEvent(event);

        if ("Up".equals(event.getState())) {

            if (Log.isDebugEnabled()) {
                Log.debug("Asterisk-IM: Processing NewState:UP event channel : "
                        + event.getChannel() + " id: " + event.getUniqueId());
            }
            handleOnPhone(event);
        }


    }

    protected void handleNewChannelEvent(NewChannelEvent event) {
        super.handleNewChannelEvent(event);

        if (event.getState().equals("Ringing")) {

            if (Log.isDebugEnabled()) {
                Log.debug("Asterisk-IM: Processing NewChannel:RINGING event channel : " + event.getChannel() + " id: " + event.getUniqueId());
            }


            handleRinging(event);
        }
    }

    protected void handleDial(NewExtenEvent event) {

        String device = getDevice(event.getChannel());

        try {
            PhoneUser phoneUser = phoneManager.getPhoneUserByDevice(device);

            //If there is no jid for this device don't do anything else
            if (phoneUser == null) {
                return;
            }


            CallSession callSession = getCallSessionFactory().getCallSession(event.getUniqueId(), phoneUser.getUsername());
            if (callSession.getCallerID() != null) {
                // We have already reported a Dial event to the user, no need to do it gain
                return;
            }

            callSession.setChannel(event.getChannel());

            Message message = new Message();
            message.setID(event.getUniqueId());
            message.setFrom(phoneManager.getComponentJID());

            String appData = event.getAppData();

            String callerID;

            if (appData != null) {
                if (appData.contains("Zap/")) {
                    String[] tokens = appData.split("/");
                    callerID = tokens[tokens.length - 1];
                }
                else if (appData.contains("IAX/") || appData.contains("SIP/")) {

                    // Hopefully they used useful names like SIP/exten
                    String name = getDevice(appData);

                    // string may be like this SIP/6131&SIP/232|20
                    int index = name.indexOf("|");
                    if (index > 0) {
                        name = name.substring(0, index);
                    }

                    // string may be like SIP/6131&SIP/232
                    index = name.indexOf("&");
                    if (index > 0) {
                        name = name.substring(0, index);
                    }

                    // string will be like SIP/6131
                    index = name.indexOf("/");
                    name = name.substring(index + 1);

                    callerID = name;
                }
                else {
                    // Whatever it is use it (hack)
                    callerID = appData;
                }
            }
            else {
                //finally just put something in there (hack)
                callerID = event.getUniqueId();
            }
            callSession.setCallerID(callerID);

            PhoneEvent phoneEvent =
                    new PhoneEvent(event.getUniqueId(), PhoneEvent.Type.DIALED, device);
            message.getElement().add(phoneEvent);

            phoneEvent.addElement("callerID").setText(callerID);

            // Send the message to each of jids for this user
            SessionManager sessionManager = XMPPServer.getInstance().getSessionManager();
            Collection<ClientSession> sessions = sessionManager.getSessions(phoneUser.getUsername());
            for (ClientSession session : sessions) {
                message.setTo(session.getAddress());
                phoneManager.sendPacket(message);
            }
        }
        catch (Throwable e) {
            Log.error(e.getMessage(), e);
        }
    }

    protected void handleOnPhone(NewStateEvent event) {

        // Do nothing when the phoneManager is being removed/destroyed
        if (!phoneManager.isReady()) {
            return;
        }

        //everything after the hyphen should be skipped
        String device = getDevice(event.getChannel());

        try {
            PhoneUser phoneUser = phoneManager.getPhoneUserByDevice(device);

            //If there is no jid for this device don't do anything else
            if (phoneUser == null) {
                Log.debug("Asterisk-IM OnPhoneTask: Could not find device/jid mapping for device " +
                        device + " returning");
                return;
            }

            Log.debug("Asterisk-IM OnPhoneTask called for user " + phoneUser);

            CallSession callSession = getCallSessionFactory().getCallSession(event.getUniqueId(), phoneUser.getUsername());

            XMPPServer server = getInstance();

            // Notify the client that they have answered the phone
            Message message = new Message();
            message.setFrom(phoneManager.getComponentJID());
            message.setID(event.getUniqueId());

            PhoneEvent phoneEvent =
                    new PhoneEvent(callSession.getId(), PhoneEvent.Type.ON_PHONE, device);
            // Get the callerID to add to the phone-event. If no callerID info is available
            // then just set an empty string and let clients do the proper rendering
            String callerID = callSession.getCallerID() == null ? "" : callSession.getCallerID();
            phoneEvent.addElement("callerID").setText(callerID);
            message.getElement().add(phoneEvent);

            //Acquire the xmpp sessions for the user
            SessionManager sessionManager = server.getSessionManager();
            Collection<ClientSession> sessions = sessionManager.getSessions(phoneUser.getUsername());

            // We don't care about people without a session
            if (sessions.size() == 0) {
                // Release call session since the user is not logged into the server
                getCallSessionFactory().destroyPhoneSession(event.getUniqueId());
                return;
            }

            Log.debug("Asterisk-IM OnPhoneTask: setting presence to away for " + phoneUser);

            // If we haven't determined their original presence, set it
            // If there is already and original presence, it means the user is
            // receiving an additional phone call. In this case we should not save the presence
            // because the current precense would also be "on phone"
            synchronized (phoneUser.getUsername().intern()) {

                if (UserPresenceUtil.getPresences(phoneUser.getUsername()) == null ||
                        UserPresenceUtil.getPresences(phoneUser.getUsername()).isEmpty()) {

                    // Iterate through all of the sessions sending out new presences for each
                    Presence presence = new Presence();
                    presence.setShow(Presence.Show.away);
                    presence.setStatus("On the phone");

                    PhoneStatus phoneStatus = new PhoneStatus(PhoneStatus.Status.ON_PHONE);
                    presence.getElement().add(phoneStatus);


                    Collection<Presence> prevPresences = new ArrayList<Presence>();

                    for (ClientSession session : sessions) {

                        message.setTo(session.getAddress());
                        phoneManager.sendPacket(message);


                        Presence prevPresence = session.getPresence();

                        // Only add the presence if it does not contain a phone element
                        if (prevPresence.getElement().element(PhoneStatus.Status.ON_PHONE.name()) == null) {
                            prevPresences.add(prevPresence);

                            JID fullJID = session.getAddress();
                            presence.setFrom(fullJID);

                            server.getPresenceRouter().route(presence);
                        }

                    }

                    UserPresenceUtil.setPresences(phoneUser.getUsername(), prevPresences);
                }
            }

        }
        catch (Throwable e) {
            Log.error(e.getMessage(), e);
        }
    }

    public void handleHangup(HangupEvent event) {
        //everything after the hyphen should be skipped
        String device = getDevice(event.getChannel());

        try {
            PhoneUser phoneUser = phoneManager.getPhoneUserByDevice(device);

            //If there is no jid for this device don't do anything else
            if (phoneUser == null) {
                return;
            }

            Log.debug("Asterisk-IM HangupTask called for user " + phoneUser);

            // Send hang up message to user
            phoneManager.sendHangupMessage(event.getUniqueId(), device, phoneUser.getUsername());

            CallSessionFactory callSessionFactory = getCallSessionFactory();

            // If the user does not have any more call sessions, set back
            // the presence to what it was before they received any calls
            synchronized (phoneUser.getUsername().intern()) {
                int callSessionCount = callSessionFactory.getUserCallSessions(phoneUser.getUsername()).size();
                if (callSessionCount <= 1) {

                    // Set the user's presence back to what it was before the phone call. The
                    // presence will be broadcasted to corresponding users
                    if (!phoneManager.restoreUserPresence(phoneUser.getUsername())) {
                        // TODO Remove this code when the "always on-the-phone problem is fixed"
                        // Check if the user is available and his presence is still
                        // on-the-phone (and no there are no more calls)
                        SessionManager sessionManager = XMPPServer.getInstance().getSessionManager();
                        Collection<ClientSession> sessions = sessionManager.getSessions(phoneUser.getUsername());
                        for (ClientSession session : sessions) {
                            Presence presence = session.getPresence();
                            Element phoneStatusElement = presence.getElement().element("phone-status");
                            // If the phone-status attribute exists check to see if the status is avaialbable
                            if (phoneStatusElement != null && PhoneStatus.Status.ON_PHONE.name().equals(phoneStatusElement.attributeValue("status")))
                            {
                                Log.debug("Asterisk-IM HangupTask: User " + phoneUser.getUsername() +
                                        " has no more call sessions, but his presence is " +
                                        "still ON_PHONE. Changing to AVAILABLE");
                                // Change presence to available since there are no more active calls
                                phoneStatusElement.addAttribute("status", PhoneStatus.Status.AVAILABLE.name());
                                getInstance().getPresenceRouter().route(presence);
                            }
                        }
                    }
                }
                else {
                    if (Log.isDebugEnabled()) {
                        Log.debug("Asterisk-IM HangupTask: User " + phoneUser.getUsername() +
                                " has " + callSessionCount +
                                " call sessions,  not restoring presence. Destroying CallSession{id=" +
                                event.getUniqueId() + ", channel=" + event.getChannel() + "}");
                    }
                }

                // finally destroy the session.
                if (callSessionFactory.destroyPhoneSession(event.getUniqueId()) == null) {
                    // Events of ZOMBIE channels may have a different ID so try to locate
                    // the CallSession to destroy based on the channel
                    if (event.getChannel().contains("<ZOMBIE>")) {
                        CallSession destroyedSession = callSessionFactory.destroyPhoneSession(
                                event.getChannel().replace("<ZOMBIE>", ""),
                                phoneUser.getUsername());
                        if (Log.isDebugEnabled()) {
                            Log.debug("Asterisk-IM HangupTask: Found ZOMBIE channel so " +
                                    "trying to destroy CallSession based on channel " +
                                    "instead of id. User: " + phoneUser.getUsername() +
                                    " channel to destroy" + event.getChannel() +
                                    " . Destruction status: " + destroyedSession == null ? "FAILED" : "SUCCEEDED");
                        }
                    }
                }

                if (callSessionCount > 1) {
                    for (CallSession session : callSessionFactory.getUserCallSessions(phoneUser.getUsername())) {
                        Log.debug("Asterisk-IM HangupTask: Remaining CallSession " + session);
                    }
                }

                // just in case this was a fake session, kill the fake session.
                // This should be ok to do, since noone should be orginating a call and hanging up at the same time
                // for a device
                callSessionFactory.destroyPhoneSession(device);

            }
        }
        catch (Throwable e) {
            Log.error(e.getMessage(), e);
        }
    }

    public void handleRinging(NewChannelEvent event) {

        String device = getDevice(event.getChannel());

        try {
            PhoneUser phoneUser = phoneManager.getPhoneUserByDevice(device);

            //If there is no jid for this device don't do anything else
            if (phoneUser == null) {
                return;
            }

            Log.debug("Asterisk-IM RingTask called for user " + phoneUser);

            // try and see if we have a fake call session
            // This will be created if there was an originated call
            CallSession fakeSession = getCallSessionFactory().destroyPhoneSession(device);


            CallSession callSession = getCallSessionFactory()
                    .getCallSession(event.getUniqueId(), phoneUser.getUsername());

            callSession.setChannel(event.getChannel());

            Message message = new Message();
            message.setID(event.getUniqueId()); //just put something in here
            message.setFrom(phoneManager.getComponentJID());

            if (fakeSession != null) {

                callSession.setCallerID(fakeSession.getCallerID());
                callSession.setDialedJID(fakeSession.getDialedJID());

                PhoneEvent dialEvent =
                        new PhoneEvent(event.getUniqueId(), PhoneEvent.Type.DIALED, device);

                dialEvent.addElement("callerID").setText(fakeSession.getCallerID());
                callSession.setCallerID(fakeSession.getCallerID());

                if (fakeSession.getDialedJID() != null) {
                    dialEvent.addElement("jid", fakeSession.getDialedJID().toString());
                }
                message.getElement().add(dialEvent);

            }
            else {
                PhoneEvent phoneEvent =
                        new PhoneEvent(event.getUniqueId(), PhoneEvent.Type.RING, device);
                String callerID = StringUtils.stripTags(event.getCallerId());
                phoneEvent.addElement("callerID").setText(callerID);
                callSession.setCallerID(callerID);

                message.getElement().add(phoneEvent);
            }

            // Send the message to each of jids for this user
            SessionManager sessionManager = XMPPServer.getInstance().getSessionManager();
            Collection<ClientSession> sessions = sessionManager.getSessions(phoneUser.getUsername());
            for (ClientSession session : sessions) {
                message.setTo(session.getAddress());
                phoneManager.sendPacket(message);
            }
        }
        catch (Throwable e) {
            Log.error(e.getMessage(), e);
        }
    }


}
