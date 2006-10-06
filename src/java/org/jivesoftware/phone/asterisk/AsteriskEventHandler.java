/**
 * $Revision$
 * $Date$
 *
 * Copyright (C) 1999-2005 Jive Software. All rights reserved.
 * This software is the proprietary information of Jive Software. Use is subject to license terms.
 */
package org.jivesoftware.phone.asterisk;

import org.jivesoftware.phone.PhoneUser;
import static org.jivesoftware.phone.asterisk.AsteriskUtil.getDevice;
import org.jivesoftware.util.Log;
import org.jivesoftware.util.StringUtils;
import org.asteriskjava.manager.event.NewStateEvent;
import org.asteriskjava.manager.event.DialEvent;
import org.asteriskjava.manager.event.HangupEvent;
import org.asteriskjava.manager.event.ManagerEvent;
import org.asteriskjava.manager.ManagerEventListener;

/**
 * Handles events that are delivered from an asterisk connection
 *
 * @author Andrew Wright
 */
public class AsteriskEventHandler implements ManagerEventListener {
    private AsteriskPhoneManager phoneManager;
    private long serverID;
    private CallSessionFactory callSessionFactory;

    public AsteriskEventHandler(long serverID, AsteriskPhoneManager asteriskPhoneManager,
                                CallSessionFactory callSessionFactory)
    {
        this.serverID = serverID;
        this.phoneManager = asteriskPhoneManager;
        this.callSessionFactory = callSessionFactory;
    }

    public void onManagerEvent(ManagerEvent event) {
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

            CallSession session = callSessionFactory.getCallSession(event.getUniqueId());
            if (session == null) {
                session = callSessionFactory.createCallSession(serverID,
                        event.getUniqueId(), phoneUser.getUsername());
            }

            session.setChannel(device);
            session.setCallerID(event.getCallerId());
            session.setCallerIDName(event.getCallerIdName());
            callSessionFactory.modifyCallSession(session, CallSession.Status.onphone);
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

            //If there is no jid for this device don't do anything else
            if (phoneUser == null) {
                Log.debug("Asterisk-IM HangupTask not active " + device);
                callSessionFactory.destroyPhoneSession(event.getUniqueId());
                return;
            }

            Log.debug("Asterisk-IM HangupTask called for user " + phoneUser);

            // If the user does not have any more call sessions, set back
            // the presence to what it was before they received any calls
            synchronized (phoneUser.getUsername().intern()) {
                int callSessionCount = callSessionFactory.getUserCallSessions(
                        phoneUser.getUsername()).size();
                // This is less than or equal to one as we have yet to destroy the session handled
                // in the hangup event.
                if (Log.isDebugEnabled()) {
                    Log.debug("Asterisk-IM HangupTask: User " + phoneUser.getUsername() +
                            " has " + callSessionCount +
                            " call sessions,  not restoring presence. Destroying CallSession{id=" +
                            event.getUniqueId() + ", channel=" + event.getChannel() + "}");
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
                                " . Destruction status: " + destroyedSession == null ?
                                    "FAILED" : "SUCCEEDED");
                        }
                    }
                }

                if (callSessionCount >= 1) {
                    for (CallSession session : callSessionFactory.
                            getUserCallSessions(phoneUser.getUsername()))
                    {
                        Log.debug("Asterisk-IM HangupTask: Remaining CallSession " + session);
                    }
                }

                // just in case this was a fake session, kill the fake session.
                // This should be ok to do, since noone should be orginating a call and hanging
                // up at the same time for a device
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
            CallSession session = callSessionFactory.getCallSession(event.getDestUniqueId());
            if (session == null) {
                session = callSessionFactory.createCallSession(serverID, event.getDestUniqueId(),
                                destPhoneUser.getUsername());
            }

            session.setChannel(destDevice);
            String callerID = StringUtils.stripTags(event.getCallerId());
            session.setCallerID(callerID);
            session.setCallerIDName(event.getCallerIdName());
            callSessionFactory.modifyCallSession(session, CallSession.Status.ringing);
        }
        catch (Throwable e) {
            Log.error(e);
        }
    }


    protected void handleDialSource(PhoneUser srcUser, String srcDevice, DialEvent event) {
        try {
            CallSession session = callSessionFactory.getCallSession(event.getDestUniqueId());
            if (session == null) {
                session = callSessionFactory.createCallSession(serverID, event.getDestUniqueId(),
                                srcUser.getUsername());
            }
            session.setChannel(srcDevice);
            session.setCallerID(event.getCallerId());
            session.setCallerIDName(event.getCallerIdName());
            callSessionFactory.modifyCallSession(session, CallSession.Status.dialed);
        }
        catch (Throwable e) {
            Log.error(e.getMessage(), e);
        }
    }
}
