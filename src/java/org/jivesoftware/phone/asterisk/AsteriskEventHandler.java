/**
 * $Revision$
 * $Date$
 *
 * Copyright (C) 1999-2005 Jive Software. All rights reserved.
 * This software is the proprietary information of Jive Software. Use is subject to license terms.
 */
package org.jivesoftware.phone.asterisk;

import net.sf.asterisk.manager.ManagerEventHandler;
import net.sf.asterisk.manager.event.DialEvent;
import net.sf.asterisk.manager.event.HangupEvent;
import net.sf.asterisk.manager.event.ManagerEvent;
import net.sf.asterisk.manager.event.NewStateEvent;
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
 * Handles events that are delivered from an asterisk connection
 *
 * @author Andrew Wright
 */
public class AsteriskEventHandler implements ManagerEventHandler {

    private AsteriskPhoneManager phoneManager;

    public AsteriskEventHandler(AsteriskPhoneManager asteriskPhoneManager) {
        this.phoneManager = asteriskPhoneManager;
    }


    public void handleEvent(ManagerEvent event) {

        if (event instanceof HangupEvent) {
            handleHangupEvent((HangupEvent) event);
        }
        else if (event instanceof NewStateEvent) {
            handleNewStateEvent((NewStateEvent) event);
        }
        else if (event instanceof DialEvent) {
            handleDialEvent((DialEvent) event);
        }

    }

    protected void handleNewStateEvent(NewStateEvent event) {

        if ("Up".equals(event.getState())) {

            if (Log.isDebugEnabled()) {
                Log.debug("Asterisk-IM: Processing NewState:UP event channel : "
                        + event.getChannel() + " id: " + event.getUniqueId());
            }
            handleOnPhone(event);
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

    protected void handleHangupEvent(HangupEvent event) {
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

    protected void handleDialEvent(DialEvent event) {


        String destDevice = getDevice(event.getDestination());
        String sourceDevice = getDevice(event.getSrc());

        try {
            PhoneUser destPhoneUser = phoneManager.getPhoneUserByDevice(destDevice);
            PhoneUser srcPhoneUser = phoneManager.getPhoneUserByDevice(sourceDevice);


            if (destPhoneUser != null) {

                handleDialDestination(destPhoneUser, destDevice, event);
            }

            if (srcPhoneUser != null) {

                handleDialSource(srcPhoneUser, sourceDevice, event);
            }

        }
        catch (Throwable e) {
            Log.error(e);
        }
    }

    private void handleDialDestination(PhoneUser destPhoneUser, String destDevice, DialEvent event) {
        try {
            Log.debug("Asterisk-IM RingTask called for user " + destPhoneUser);


            CallSession destCallSession = getCallSessionFactory()
                    .getCallSession(event.getDestUniqueId(), destPhoneUser.getUsername());


            destCallSession.setChannel(destDevice);

            Message message = new Message();
            message.setID(event.getDestUniqueId()); //just put something in here
            message.setFrom(phoneManager.getComponentJID());

            String callerIDName = event.getCallerIdName();


            PhoneEvent phoneEvent =
                    new PhoneEvent(event.getDestUniqueId(), PhoneEvent.Type.RING, destDevice);
            String callerID = StringUtils.stripTags(event.getCallerId());
            phoneEvent.addElement("callerID").setText(callerID != null ? callerID : "");
            phoneEvent.addElement("callerIDName").setText(callerIDName != null ? callerIDName : "");

            destCallSession.setCallerID(callerID);

            message.getElement().add(phoneEvent);

            // Send the message to each of jids for this user
            SessionManager sessionManager = XMPPServer.getInstance().getSessionManager();
            Collection<ClientSession> sessions = sessionManager.getSessions(destPhoneUser.getUsername());
            for (ClientSession session : sessions) {
                message.setTo(session.getAddress());
                phoneManager.sendPacket(message);
            }
        }
        catch (Throwable e) {
            Log.error(e);
        }
    }


    protected void handleDialSource(PhoneUser srcUser, String srcDevice, DialEvent event) {

        try {

            CallSession callSession = getCallSessionFactory()
                    .getCallSession(event.getSrcUniqueId(), srcUser.getUsername());
            callSession.setChannel(srcDevice);


            Message message = new Message();
            message.setID(event.getSrcUniqueId());
            message.setFrom(phoneManager.getComponentJID());


            String callerID = event.getCallerId();
            String callerIDName = event.getCallerIdName();


            callSession.setCallerID(callerID);

            PhoneEvent phoneEvent =
                    new PhoneEvent(event.getSrcUniqueId(), PhoneEvent.Type.DIALED, srcDevice);
            message.getElement().add(phoneEvent);

            phoneEvent.addElement("callerID").setText(callerID != null ? callerID : "");
            phoneEvent.addElement("callerIDName").setText(callerIDName != null ? callerIDName : "");

            // Send the message to each of jids for this user
            SessionManager sessionManager = XMPPServer.getInstance().getSessionManager();
            Collection<ClientSession> sessions = sessionManager.getSessions(srcUser.getUsername());
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
