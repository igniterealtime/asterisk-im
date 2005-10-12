/**
 * Copyright (C) 1999-2005 Jive Software. All rights reserved.
 *
 * This software is the proprietary information of Jive Software. Use is subject to license terms.
 */
package org.jivesoftware.phone;

import org.jivesoftware.messenger.Session;
import org.jivesoftware.messenger.XMPPServer;
import org.jivesoftware.messenger.interceptor.PacketInterceptor;
import org.jivesoftware.messenger.interceptor.PacketRejectedException;
import org.jivesoftware.phone.element.PhoneStatus;
import org.jivesoftware.phone.util.UserPresenceUtil;
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
public class OnPhonePacketInterceptor implements PacketInterceptor {

    public void interceptPacket(Packet packet, Session session, boolean read, boolean processed)
            throws PacketRejectedException {


        if (!processed) {

            if (packet instanceof Presence) {


                final JID from = packet.getFrom();

                // Only process packets that are users on our server
                if (!XMPPServer.getInstance().getServerInfo().getName().equals(from.getDomain())) {
                    return;
                }

                final Presence presence = (Presence) packet;

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

                        // If this an unavailable status, it needs to be handled correctly, remove old presences
                        // so that presence packets aren't sent out
                        if (Presence.Type.unavailable.equals(presence.getType())) {
                            UserPresenceUtil.removePresences(username);
                        }
                        else {

                            // find an existing old presence packet for this full jid and replace it with the new one
                            for (Presence current : presences) {

                                if (current.getFrom().equals(packet.getFrom())) {
                                    Log.debug("OnPhonePacketInterceptor removing old presence for jid " + packet.getFrom());
                                    presences.remove(current);
                                }

                            }
                            Log.debug("OnPhonePacketInterceptor adding presence for jid " + packet.getFrom());
                            presences.add((Presence) packet);

                            //Throw an exception to prevent the presence from being processed any further
                            Log.debug("OnPhonePacketInterceptor Rejecting presence packet for jid " + packet.getFrom());
                            throw new PacketRejectedException("Status will change after user is off the phone!");
                        }
                    }
                    else if (!CallSessionFactory.getCallSessionFactory().getUserCallSessions(username).isEmpty()) {


                        if (!Presence.Type.unavailable.equals(presence.getType())) {
                            Log.debug("OnPhonePacketInterceptor: No existing presence, cacheing current presence setting presence to Away:On Phone");


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

    }

}
