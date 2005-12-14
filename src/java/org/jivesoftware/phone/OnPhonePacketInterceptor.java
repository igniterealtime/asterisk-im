/**
 * Copyright (C) 1999-2005 Jive Software. All rights reserved.
 *
 * This software is the proprietary information of Jive Software. Use is subject to license terms.
 */
package org.jivesoftware.phone;

import org.jivesoftware.wildfire.Session;
import org.jivesoftware.wildfire.XMPPServer;
import org.jivesoftware.wildfire.event.SessionEventListener;
import org.jivesoftware.wildfire.interceptor.PacketInterceptor;
import org.jivesoftware.wildfire.interceptor.PacketRejectedException;
import org.jivesoftware.phone.element.PhoneStatus;
import org.jivesoftware.phone.util.UserPresenceUtil;
import org.jivesoftware.phone.asterisk.AsteriskPlugin;
import org.jivesoftware.util.Log;
import org.xmpp.packet.JID;
import org.xmpp.packet.Packet;
import org.xmpp.packet.Presence;

import java.util.Arrays;
import java.util.Collection;

/**
 * Prevents presence from changing while a user is on the phone. It will however queue the presence packet to be applied
 * once the user has hung up the phone.
 *
 * @author Andrew Wright
 */
public class OnPhonePacketInterceptor implements PacketInterceptor, SessionEventListener {

    private AsteriskPlugin asteriskPlugin;

    public OnPhonePacketInterceptor(AsteriskPlugin asteriskPlugin) {
        this.asteriskPlugin = asteriskPlugin;
    }
    public void interceptPacket(Packet packet, Session session, boolean incoming, boolean processed)
            throws PacketRejectedException {

        // Only process incoming Presence packets before they have been processed by the server
        // For the case that a client was abruptly disconnected no unavailable presence will be
        // received. Therefore, we implement the SessionEventListener interface to react when
        // the session is distroyed
        if (!processed && incoming && packet instanceof Presence) {

            final JID from = packet.getFrom();

            // Only process packets sent by users on our server
            if (!XMPPServer.getInstance().getServerInfo().getName().equals(from.getDomain())) {
                return;
            }

            final Presence presence = (Presence) packet;

            // Ignore unavailable presences. That case is covered by #sessionDestroyed(Session)
            if (Presence.Type.unavailable.equals(presence.getType())) {
                return;
            }

            // If the presence type is error we should just ignore it
            if (Presence.Type.error.equals(presence.getType())) {
                return;
            }
            // We should also ignore probe presence types
            else if (Presence.Type.probe.equals(presence.getType())) {
                return;
            }
            // If this presence is directed to an individual then ignore it
            else if (presence.getTo() != null) {
                return;
            }

            final String username = from.getNode();

            // If the user is anonymous, or is a service
            if (username == null) {
                return;
            }

            synchronized (username.intern()) {
                final Collection<Presence> presences = UserPresenceUtil.getPresences(username);

                // If the user is on the phone (there are presences) then
                // queue the new presence and reject the packet
                if (presences != null && presences.size() > 0) {

                    // Reject the packet if sessions exist
                    if (!CallSessionFactory.getCallSessionFactory().getUserCallSessions(username).isEmpty()) {

                        // find an existing old presence packet for this full jid and replace it with the new one
                        for (Presence current : presences) {

                            if (current.getFrom().equals(packet.getFrom())) {
                                Log.debug("OnPhonePacketInterceptor: removing old presence from queue for jid " + packet.getFrom());
                                presences.remove(current);
                            }

                        }
                        if (Log.isDebugEnabled()) {
                            Log.debug(
                                    "OnPhonePacketInterceptor: adding presence to queue for jid " +
                                            packet.getFrom() + ". Rejecting presence: " +
                                            presence);
                        }
                        presences.add((Presence) packet);

                        //Throw an exception to prevent the presence from being processed any further
                        throw new PacketRejectedException("Status will change after user is off the phone!");
                    }
                    else {
                        Log.error("OnPhonePacketInterceptor: Cannot reject status for "+packet.getFrom()+" because" +
                                "the user still has call sessions!");

                        // Go ahead and clean up the presences, because there shouldn't actually be any presences
                        // in here
                        UserPresenceUtil.removePresences(packet.getFrom().getNode());
                    }
                } else if (!CallSessionFactory.getCallSessionFactory().getUserCallSessions(username).isEmpty()) {

                    // Process available presences only if plugin is not being shutdown
                    if (asteriskPlugin.isComponentReady() && !Presence.Type.unavailable.equals(presence.getType())) {
                        if (Log.isDebugEnabled()) {
                            Log.debug(
                                    "OnPhonePacketInterceptor: No queued presences, queuing current presence and setting presence to \"Away:On Phone\" for: " +
                                            presence);
                        }

                        UserPresenceUtil.setPresences(username, Arrays.asList(presence.createCopy()));

                        // Iterate through all of the sessions sending out new presences for each
                        //Presence presence = new Presence();
                        presence.setShow(Presence.Show.away);
                        presence.setStatus("On the phone");
                        presence.setFrom(from);


                        PhoneStatus phoneStatus = new PhoneStatus(PhoneStatus.Status.ON_PHONE);
                        presence.getElement().add(phoneStatus);
                    }

                }
            }
        }
    }

    public void sessionCreated(Session session) {
        //Do nothing
    }

    public void sessionDestroyed(Session session) {
        JID userFullJID = session.getAddress();

        // Ignore sessions with no queued presences when the user went offline
        if (UserPresenceUtil.getPresences(userFullJID.getNode()) == null) {
            return;
        }

        // Remove old presences since the session has been destroyed
        if (Log.isDebugEnabled()) {
            Log.debug("OnPhonePacketInterceptor: User went offline. Removing queued presences for jid " +
                    userFullJID);
        }
        UserPresenceUtil.removePresences(userFullJID);
    }

    public void anonymousSessionCreated(Session session) {
        //Do nothing
    }

    public void anonymousSessionDestroyed(Session session) {
        //Do nothing
    }

}
