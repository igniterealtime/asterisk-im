/**
 * $RCSfile: AsteriskEventHandler.java,v $
 * $Revision: 1.10 $
 * $Date: 2005/07/01 23:56:27 $
 *
 * Copyright (C) 1999-2004 Jive Software. All rights reserved.
 *
 * This software is the proprietary information of Jive Software. Use is subject to license terms.
 */
package org.jivesoftware.phone.asterisk;

import net.sf.asterisk.manager.ManagerEventHandler;
import net.sf.asterisk.manager.event.*;
import org.dom4j.Attribute;
import org.dom4j.Element;
import org.jivesoftware.messenger.ClientSession;
import org.jivesoftware.messenger.SessionManager;
import org.jivesoftware.messenger.XMPPServer;
import static org.jivesoftware.messenger.XMPPServer.getInstance;
import org.jivesoftware.phone.*;
import static org.jivesoftware.phone.CallSessionFactory.getCallSessionFactory;
import static org.jivesoftware.phone.PhoneManagerFactory.close;
import static org.jivesoftware.phone.PhoneManagerFactory.getPhoneManager;
import org.jivesoftware.phone.element.PhoneEvent;
import org.jivesoftware.phone.element.PhoneEvent.Type;
import org.jivesoftware.phone.element.PhoneStatus;
import org.jivesoftware.phone.element.PhoneStatus.Status;
import org.jivesoftware.phone.util.PhoneConstants;
import static org.jivesoftware.phone.util.ThreadPool.getThreadPool;
import org.jivesoftware.util.StringUtils;
import org.xmpp.packet.JID;
import org.xmpp.packet.Message;
import org.xmpp.packet.Presence;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Used to handle all events coming from Asterisk. Various tasks will be executed based on tasks
 * that we are expecting, such as set a users status as away when they answer the phone.
 *
 * @author Andrew Wright
 */
public class AsteriskEventHandler implements ManagerEventHandler, PhoneConstants {

    private static final Logger log = Logger.getLogger(AsteriskEventHandler.class.getName());

    /**
     * Used to store old presence objects before a person goes on the phone
     */
    private Map<JID, Collection<Presence>> previousPresenceMap =
            new ConcurrentHashMap<JID, Collection<Presence>>();


    private AsteriskPlugin asteriskPlugin;

    public AsteriskEventHandler(AsteriskPlugin asteriskPlugin) {
        this.asteriskPlugin = asteriskPlugin;
    }


    public void handleEvent(ManagerEvent event) {

        if (event instanceof ChannelEvent) {
            handleChannelEvent((ChannelEvent) event);
        } else if (event instanceof LinkEvent) {
            getThreadPool().execute(new LinkTask((LinkEvent) event));
        }

    }

    public void handleChannelEvent(ChannelEvent event) {

        ExecutorService executor = getThreadPool();

        if (executor == null) {
            log.severe("Phone Thread pool was not initialized, returning!");
            return;
        } else if (executor.isShutdown()) {
            log.warning("Phone Thread pool has been shutdown, plugin shutdown must be in progress! " +
                    "Not processing event");
            return;
        }

        if (event instanceof NewStateEvent) {
            NewStateEvent nsEvent = (NewStateEvent) event;
            String state = nsEvent.getState();
            if ("Up".equals(state)) {
                executor.execute(new OnPhoneTask(nsEvent));
            }

        } else if (event instanceof NewChannelEvent) {
            NewChannelEvent ncEvent = (NewChannelEvent) event;
            String state = ncEvent.getState();

            // This will actually cause the ring task to be potentially executed twice
            // Once for the Ring state and then again for the Ringing state.
            // I am not sure if I will eventually change this just to work for the ring state
            // Or keep it for both
            if (state.equals("Ringing")) {
                executor.execute(new RingTask(ncEvent));
            } else if (state.equals("Ring")) {
                executor.execute(new DialedTask(ncEvent));
            }

        } else if (event instanceof HangupEvent) {
            executor.execute(new HangupTask((HangupEvent) event));
        }
    }

    /**
     * Adds link event information into the call session
     */
    private class LinkTask implements Runnable {

        private LinkEvent event;

        public LinkTask(LinkEvent event) {
            this.event = event;
        }

        public void run() {

            PhoneManager phoneManager = null;

            try {
                phoneManager = getPhoneManager();

                String device = getDevice(event.getChannel1());

                PhoneUser phoneUser = phoneManager.getByDevice(device);

                if (phoneUser != null) {
                    CallSession callSession = getCallSessionFactory().getPhoneSession(event.getUniqueId1());
                    callSession.setChannel(event.getChannel1());
                    callSession.setLinkedChannel(event.getChannel2());
                }

                // Now setup this up for the second user
                device = getDevice(event.getChannel2());
                phoneUser = phoneManager.getByDevice(device);

                if (phoneUser != null) {
                    CallSession callSession = getCallSessionFactory().getPhoneSession(event.getUniqueId2());
                    callSession.setChannel(event.getChannel2());
                    callSession.setLinkedChannel(event.getChannel1());
                }

            }
            catch (Exception e) {
                log.log(Level.SEVERE, e.getMessage(), e);
            }
            finally {
                close(phoneManager);
            }


        }


    }

    /**
     * This task is used to handle events where a user has just answered the phone.
     */
    private class OnPhoneTask implements Runnable {

        private NewStateEvent event;

        public OnPhoneTask(NewStateEvent event) {
            this.event = event;
        }

        public void run() {

            //everything after the hyphen should be skipped
            String device = getDevice(event.getChannel());

            PhoneManager phoneManager = null;

            try {
                phoneManager = getPhoneManager();
                PhoneUser phoneUser = phoneManager.getByDevice(device);

                //If there is no jid for this device don't do anything else
                if (phoneUser == null) {
                    log.finer("AnswerTask: Could not find device/jid mapping for device " +
                            device + " returning");
                    return;
                }

                CallSession callSession = getCallSessionFactory().getPhoneSession(event.getUniqueId());

                // If the device should be monitored, then start monitoring
                PhoneDevice phoneDevice = phoneManager.getDevice(device);
                if (phoneDevice.isMonitored()) {
                    PhoneManager mgr = PhoneManagerFactory.getPhoneManager();
                    try {
                        log.info("Staring monitoring on channel "+event.getChannel());
                        mgr.monitor(event.getChannel());
                        callSession.setMonitored(true);
                    }
                    finally {
                        PhoneManagerFactory.close(mgr);
                    }
                }

                JID jid = getJID(phoneUser);


                XMPPServer server = getInstance();

                // Notify the client that they have answered the phone
                Message message = new Message();
                message.setFrom(asteriskPlugin.getComponentJID());
                message.setTo(jid);

                PhoneEvent phoneEvent =
                        new PhoneEvent(callSession.getId(), Type.ON_PHONE, device);
                phoneEvent.addElement("callerID").setText(StringUtils.stripTags(event.getCallerId()));
                message.getElement().add(phoneEvent);

                asteriskPlugin.sendPacket(message);

                //Acquire the xmpp sessions for the user
                SessionManager sessionManager = server.getSessionManager();
                Collection<ClientSession> sessions = sessionManager.getSessions(jid.getNode());

                // We don't care about people without a session
                if (sessions.size() == 0) {
                    return;
                }

                log.finer("AnswerTask: setting presence to away for " + jid);

                // Iterate through all of the sessions sending out new presences for each
                Presence presence = new Presence();
                presence.setShow(Presence.Show.away);
                presence.setStatus("On the phone");

                PhoneStatus phoneStatus = new PhoneStatus(Status.ON_PHONE);
                presence.getElement().add(phoneStatus);

                Collection<Presence> prevPresences = new ArrayList<Presence>();

                for (ClientSession session : sessions) {

                    Presence prevPresence = session.getPresence();
                    prevPresences.add(prevPresence);

                    JID fullJID = session.getAddress();
                    presence.setFrom(fullJID);

                    server.getPresenceRouter().route(presence);
                }

                previousPresenceMap.put(jid, prevPresences);
            }
            catch (Exception e) {
                log.log(Level.SEVERE, e.getMessage(), e);
            }
            finally {
                close(phoneManager);
            }
        }

    }

    /**
     * This task is used to handle events where a user has just hang up the phone
     */
    private class HangupTask implements Runnable {

        private HangupEvent event;

        public HangupTask(HangupEvent event) {
            this.event = event;
        }

        public void run() {
            //everything after the hyphen should be skipped
            String device = getDevice(event.getChannel());

            PhoneManager phoneManager = null;

            try {
                phoneManager = getPhoneManager();
                PhoneUser phoneUser = phoneManager.getByDevice(device);

                //If there is no jid for this device don't do anything else
                if (phoneUser == null) {
                    return;
                }

                // Check and see if we are monitoring the call, if we are cancel the monitor
                /*
                CallSession callSession = CallSessionFactory.getCallSessionFactory().getPhoneSession(event.getUniqueId());
                if (callSession.isMonitored()) {
                    PhoneManager mgr = PhoneManagerFactory.getPhoneManager();
                    try {
                        mgr.stopMonitor(device);
                    }
                    finally {
                        PhoneManagerFactory.close(mgr);
                    }
                }
                */

                JID jid = getJID(phoneUser);

                Message message = new Message();
                message.setFrom(asteriskPlugin.getComponentJID());
                message.setTo(jid);

                PhoneEvent phoneEvent =
                        new PhoneEvent(event.getUniqueId(), Type.HANG_UP, device);
                message.getElement().add(phoneEvent);

                asteriskPlugin.sendPacket(message);

                // Set the user's presence back to what it was before the phone call
                Collection<Presence> presences = previousPresenceMap.remove(jid);
                if (presences != null) {
                    for (Presence presence : presences) {

                        Element presenceElement = presence.getElement();

                        Element phoneStatusElement = presenceElement.element("phone-status");
                        // If the phone-status attribute exists check to see if the
                        if (phoneStatusElement != null) {

                            Attribute statusAtt = phoneStatusElement.attribute("status");

                            if (!Status.AVAILABLE.name().equals(statusAtt.getText())) {
                                statusAtt.setText(Status.AVAILABLE.name());
                            }

                        }
                        // The attribute doesn't exist add new attribute
                        else {

                            PhoneStatus status = new PhoneStatus(Status.AVAILABLE);
                            presence.getElement().add(status);

                        }

                        getInstance().getPresenceRouter().route(presence);
                    }
                }

                // finally destroy the session.
                getCallSessionFactory().destroyPhoneSession(event.getUniqueId());
            }
            catch (Exception e) {
                log.log(Level.SEVERE, e.getMessage(), e);
            }
            finally {
                close(phoneManager);
            }
        }
    }

    /**
     * Used to send a message to a user when their phone is ringing
     */
    private class RingTask implements Runnable {

        private NewChannelEvent event;

        public RingTask(NewChannelEvent event) {
            this.event = event;
        }

        public void run() {

            String device = getDevice(event.getChannel());

            PhoneManager phoneManager = null;

            try {
                phoneManager = getPhoneManager();
                PhoneUser phoneUser = phoneManager.getByDevice(device);

                //If there is no jid for this device don't do anything else
                if (phoneUser == null) {
                    return;
                }


                CallSession callSession = getCallSessionFactory()
                        .getPhoneSession(event.getUniqueId());

                callSession.setChannel(event.getChannel());

                JID jid = getJID(phoneUser);

                Message message = new Message();
                message.setFrom(asteriskPlugin.getComponentJID());
                message.setTo(jid);

                PhoneEvent phoneEvent =
                        new PhoneEvent(event.getUniqueId(), Type.RING, device);
                phoneEvent.addElement("callerID").setText(StringUtils.stripTags(event.getCallerId()));
                message.getElement().add(phoneEvent);

                asteriskPlugin.sendPacket(message);
            }
            catch (Exception e) {
                log.log(Level.SEVERE, e.getMessage(), e);
            }
            finally {
                close(phoneManager);
            }
        }
    }


    /**
     * Used to send a message to a user when their phone is ringing
     */
    private class DialedTask implements Runnable {

        private NewChannelEvent event;

        public DialedTask(NewChannelEvent event) {
            this.event = event;
        }

        public void run() {

            String device = getDevice(event.getChannel());

            PhoneManager phoneManager = null;

            try {
                phoneManager = getPhoneManager();
                PhoneUser phoneUser = phoneManager.getByDevice(device);

                //If there is no jid for this device don't do anything else
                if (phoneUser == null) {
                    return;
                }


                CallSession callSession = getCallSessionFactory()
                        .getPhoneSession(event.getUniqueId());

                callSession.setChannel(event.getChannel());

                JID jid = getJID(phoneUser);

                Message message = new Message();
                message.setFrom(asteriskPlugin.getComponentJID());
                message.setTo(jid);

                PhoneEvent phoneEvent =
                        new PhoneEvent(event.getUniqueId(), Type.DIALED, device);
                message.getElement().add(phoneEvent);

                asteriskPlugin.sendPacket(message);
            }
            catch (Exception e) {
                log.log(Level.SEVERE, e.getMessage(), e);
            }
            finally {
                close(phoneManager);
            }
        }
    }

    /**
     * Strips the hyphen out of fullChannel names. Asterisk will pass fullChannel names such as
     * SIP/6131-53f. The -53f is unique per call, not per user. So will strip this part out
     * to get the correct fullChannel.
     *
     * @param fullChannel full Channel
     * @return the fullChannel with out the final hyphen section
     */
    private String getDevice(String fullChannel) {
        return fullChannel.split("-")[0];
    }


    private JID getJID(PhoneUser user) {
        String serverName = getInstance().getServerInfo().getName();
        return new JID(user.getUsername(), serverName, null);
    }

}
