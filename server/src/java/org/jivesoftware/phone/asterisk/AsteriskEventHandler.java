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
import org.jivesoftware.phone.CallSession;
import org.jivesoftware.phone.CallSessionFactory;
import static org.jivesoftware.phone.CallSessionFactory.getCallSessionFactory;
import org.jivesoftware.phone.PhoneUser;
import static org.jivesoftware.phone.asterisk.AsteriskUtil.getDevice;
import org.jivesoftware.phone.xmpp.element.PhoneEvent;
import org.jivesoftware.util.Log;
import org.jivesoftware.util.StringUtils;
import org.xmpp.packet.Message;

/**
 * Handles events that are delivered from an asterisk connection
 *
 * @author Andrew Wright
 */
public class AsteriskEventHandler implements ManagerEventHandler {
    private AsteriskPhoneManager phoneManager;
    private long serverID;

    public AsteriskEventHandler(long serverID, AsteriskPhoneManager asteriskPhoneManager) {
        this.serverID = serverID;
        this.phoneManager = asteriskPhoneManager;
    }

    public void handleEvent(ManagerEvent event) {

        if (event instanceof HangupEvent) {
            handleHangupEvent((HangupEvent)event);
        }
        else if (event instanceof NewStateEvent) {
            handleNewStateEvent((NewStateEvent)event);
        }
        else if (event instanceof DialEvent) {
            handleDialEvent((DialEvent)event);
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
            PhoneUser phoneUser = phoneManager.getActivePhoneUserByDevice(serverID, device);

            //If there is no jid for this device don't do anything else
            if (phoneUser == null) {
                Log.debug("Asterisk-IM OnPhoneTask: Could not find device/jid mapping for device " +
                    device + " returning");
                return;
            }

            Log.debug("Asterisk-IM OnPhoneTask called for user " + phoneUser);

            CallSession callSession = getCallSessionFactory().getCallSession(serverID,
                    event.getUniqueId(), phoneUser.getUsername());

            // Notify the client that they have answered the phone
            Message message = new Message();
            message.setID(event.getUniqueId());

            PhoneEvent phoneEvent =
                new PhoneEvent(callSession.getId(), PhoneEvent.Type.ON_PHONE, device);
            // Get the callerID to add to the phone-event. If no callerID info is available
            // then just set an empty string and let clients do the proper rendering
            String callerID = callSession.getCallerID() == null ? "" : callSession.getCallerID();
            phoneEvent.addElement("callerID").setText(callerID);
            message.getElement().add(phoneEvent);
            phoneManager.plugin.sendPacket2User(phoneUser.getUsername(), message);

            phoneManager.plugin.setPresence(phoneUser.getUsername(), "On the phone");
        }
        catch (Throwable e) {
            Log.error(e.getMessage(), e);
        }
    }

    protected void handleHangupEvent(HangupEvent event) {
        //everything after the hyphen should be skipped
        String device = getDevice(event.getChannel());

        try {
            PhoneUser phoneUser = phoneManager.getActivePhoneUserByDevice(serverID, device);

            CallSessionFactory callSessionFactory = getCallSessionFactory();

            //If there is no jid for this device don't do anything else
            if (phoneUser == null) {
                Log.debug("Asterisk-IM HangupTask not active " + device);
                callSessionFactory.destroyPhoneSession(event.getUniqueId());
                return;
            }

            Log.debug("Asterisk-IM HangupTask called for user " + phoneUser);

            // Send hang up message to user
            phoneManager.sendHangupMessage(event.getUniqueId(), device, phoneUser.getUsername());

            // If the user does not have any more call sessions, set back
            // the presence to what it was before they received any calls
            synchronized (phoneUser.getUsername().intern()) {
                int callSessionCount = callSessionFactory.getUserCallSessions(
                        phoneUser.getUsername()).size();
                // This is less than or equal to one as we have yet to destroy the session handled
                // in the hangup event.
                if (callSessionCount <= 1) {
                    phoneManager.plugin.restorePresence(phoneUser.getUsername());
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

                if (callSessionCount >= 1) {
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
            PhoneUser destPhoneUser = phoneManager.getPhoneUserByDevice(serverID, destDevice);
            PhoneUser srcPhoneUser = phoneManager.getPhoneUserByDevice(serverID, sourceDevice);


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
                .getCallSession(serverID, event.getDestUniqueId(),
                        destPhoneUser.getUsername());


            destCallSession.setChannel(destDevice);

            Message message = new Message();
            message.setID(event.getDestUniqueId()); //just put something in here
            String callerIDName = event.getCallerIdName();

            PhoneEvent phoneEvent =
                new PhoneEvent(event.getDestUniqueId(), PhoneEvent.Type.RING, destDevice);
            String callerID = StringUtils.stripTags(event.getCallerId());
            phoneEvent.addElement("callerID").setText(callerID != null ? callerID : "");
            phoneEvent.addElement("callerIDName").setText(callerIDName != null ? callerIDName : "");

            destCallSession.setCallerID(callerID);

            message.getElement().add(phoneEvent);

            phoneManager.plugin.sendPacket2User(destPhoneUser.getUsername(), message);
        }
        catch (Throwable e) {
            Log.error(e);
        }
    }


    protected void handleDialSource(PhoneUser srcUser, String srcDevice, DialEvent event) {

        try {
            CallSession callSession = getCallSessionFactory()
                .getCallSession(serverID, event.getSrcUniqueId(), srcUser.getUsername());
            callSession.setChannel(srcDevice);


            Message message = new Message();
            message.setID(event.getSrcUniqueId());


            String callerID = event.getCallerId();
            String callerIDName = event.getCallerIdName();


            callSession.setCallerID(callerID);

            PhoneEvent phoneEvent =
                new PhoneEvent(event.getSrcUniqueId(), PhoneEvent.Type.DIALED, srcDevice);
            message.getElement().add(phoneEvent);

            phoneEvent.addElement("callerID").setText(callerID != null ? callerID : "");
            phoneEvent.addElement("callerIDName").setText(callerIDName != null ? callerIDName : "");

            phoneManager.plugin.sendPacket2User(srcUser.getUsername(), message);

        }
        catch (Throwable e) {
            Log.error(e.getMessage(), e);
        }
    }
}
