/**
 * $RCSfile: PhoneClient.java,v $
 * $Revision: 1.11 $
 * $Date: 2005/07/05 18:41:09 $
 *
 * Copyright (C) 1999-2004 Jive Software. All rights reserved.
 *
 * This software is the proprietary information of Jive Software. Use is subject to license terms.
 */
package org.jivesoftware.phone.client;

import org.jivesoftware.phone.client.action.DialAction;
import org.jivesoftware.phone.client.action.ForwardAction;
import org.jivesoftware.phone.client.event.PhoneEventDispatcher;
import org.jivesoftware.phone.client.event.PhoneEventExtensionElement;
import org.jivesoftware.phone.client.event.PhoneEventPacketListener;
import org.jivesoftware.smack.*;
import org.jivesoftware.smack.filter.*;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.disco.packet.DiscoverInfo;
import org.jivesoftware.smackx.disco.packet.DiscoverItems;
import org.jxmpp.jid.Jid;
import org.jxmpp.util.XmppStringUtils;

/**
 * Provides the ability to Phone Openfire plugin.
 *
 * @author Andrew Wright
 */
public class PhoneClient {

    private XMPPConnection conn;
    private PhoneEventDispatcher eventDispatcher;
    private Jid component;
    private ServiceDiscoveryManager serviceDiscoveryManager;

    /**
     * Creates a new instance of the Phone client.
     *
     * @param conn XMPP Connection to use for the phone client
     */
    public PhoneClient(XMPPConnection conn) throws XMPPException, SmackException.NotConnectedException, SmackException.NoResponseException, InterruptedException {
        this.conn = conn;

        if(!conn.isAuthenticated()) {
            throw new PhoneException("Connection is not authenticated!");
        }

        eventDispatcher = new PhoneEventDispatcher();
        conn.addStanzaListener(new PhoneEventPacketListener(eventDispatcher),
                new StanzaExtensionFilter(PhoneEventExtensionElement.ELEMENT_NAME,
                        PhoneEventExtensionElement.NAMESPACE));

        serviceDiscoveryManager = ServiceDiscoveryManager.getInstanceFor(conn);
        DiscoverItems items = serviceDiscoveryManager.discoverItems(conn.getXMPPServiceDomain());

        // Attempt to discover the component jid and see if this user can use the phone service
        for (DiscoverItems.Item item : items.getItems()) {
            if ("phone".equals(item.getName())) {
                component = item.getEntityID();
                break;
            }
        }

        if (component == null) {
            throw new PhoneException("Server does not have a phone services");
        }

        DiscoverInfo info = serviceDiscoveryManager.discoverInfo(component, conn.getUser().getLocalpart().asUnescapedString());
        if (!info.containsFeature("http://jivesoftware.com/phone")) {
            throw new PhoneException("User does not have phone support");
        }

    }

    /**
     * Dials the extension with the user's default device. Extension can be in a short (local
     * pbx format) or a full phone number.
     * <p/>
     * After the user has answered a {@link OnPhoneEvent} with a Call object should be dispatched.
     *
     * @param extension extension to dialByExtension
     * @throws PhoneActionException thrown if dialing cannot complete
     */
    public void dialByExtension(String extension) throws PhoneActionException {

        DialAction action = new DialAction();
        action.setExtension(extension);
        action.setTo(component);
        action.setFrom(conn.getUser());

        final IQ iq;
        try {
            iq = conn.sendIqRequestAndWaitForResponse(action);
        } catch (SmackException.NotConnectedException e) {
            throw new PhoneActionException("Not connected!");
        } catch (InterruptedException e) {
            throw new PhoneActionException("Interrupted");
        } catch (XMPPException.XMPPErrorException e) {
            throw new PhoneActionException(e.getStanzaError().toString());
        } catch (SmackException.NoResponseException e) {
            throw new PhoneActionException("No response received from the server");
        }

        if (!(iq instanceof DialAction)) {
            throw new PhoneActionException("Did not acquire the proper response!");
        }
    }

    /**
     * Dials the extension with the user's default device. Extension can be in a short (local
     * pbx format) or a full phone number.
     * <p/>
     * After the user has answered a {@link OnPhoneEvent} with a Call object should be dispatched.
     *
     * @param jid extension to dialByExtension
     * @throws PhoneActionException thrown if dialing cannot complete
     */
    public void dialByJID(String jid) throws PhoneActionException {

        DialAction action = new DialAction();
        action.setJid(jid);
        action.setTo(component);
        action.setFrom(conn.getUser());

        final IQ iq;
        try {
            iq = conn.sendIqRequestAndWaitForResponse(action);
        } catch (SmackException.NotConnectedException e) {
            throw new PhoneActionException("Not connected!");
        } catch (InterruptedException e) {
            throw new PhoneActionException("Interrupted");
        } catch (XMPPException.XMPPErrorException e) {
            throw new PhoneActionException(e.getStanzaError().toString());
        } catch (SmackException.NoResponseException e) {
            throw new PhoneActionException("No response received from the server");
        }

        if (!(iq instanceof DialAction)) {
            throw new PhoneActionException("Did not acquire the proper response!");
        }
    }

    /**
     * Forwards the call to another extension.
     *
     * @param call      The call to forward
     * @param extension exetension to forward the call to
     * @throws PhoneActionException thrown if there are problems forwarding the call
     */
    public void forward(Call call, String extension) throws PhoneActionException {

        if (call == null) {
            throw new PhoneActionException("passed null call object");
        }

        if (call.getId() == null) {
            throw new PhoneActionException("callID cannot be null");
        }

        ForwardAction action = new ForwardAction(call.getId());
        action.setExtension(extension);
        action.setTo(component);
        action.setFrom(conn.getUser());

        final IQ iq;
        try {
            iq = conn.sendIqRequestAndWaitForResponse(action);
        } catch (SmackException.NotConnectedException e) {
            throw new PhoneActionException("Not connected!");
        } catch (InterruptedException e) {
            throw new PhoneActionException("Interrupted");
        } catch (XMPPException.XMPPErrorException e) {
            throw new PhoneActionException(e.getStanzaError().toString());
        } catch (SmackException.NoResponseException e) {
            throw new PhoneActionException("No response received from the server");
        }

        if (!(iq instanceof ForwardAction)) {
            throw new PhoneActionException("Did not acquire the proper response!");
        }

    }

    /**
     * Forwards the call to another extension.
     *
     * @param call      The call to forward
     * @param jid jid of the person to forward too.
     * @throws PhoneActionException thrown if there are problems forwarding the call
     */
    public void forwardByJID(Call call, String jid) throws PhoneActionException {

        if (call == null) {
            throw new PhoneActionException("passed null call object");
        }

        if (call.getId() == null) {
            throw new PhoneActionException("callID cannot be null");
        }

        ForwardAction action = new ForwardAction(call.getId());
        action.setTo(component);
        action.setFrom(conn.getUser());
        action.setJID(jid);

        final IQ iq;
        try {
            iq = conn.sendIqRequestAndWaitForResponse(action);
        } catch (SmackException.NotConnectedException e) {
            throw new PhoneActionException("Not connected!");
        } catch (InterruptedException e) {
            throw new PhoneActionException("Interrupted");
        } catch (XMPPException.XMPPErrorException e) {
            throw new PhoneActionException(e.getStanzaError().toString());
        } catch (SmackException.NoResponseException e) {
            throw new PhoneActionException("No response received from the server");
        }

        if (!(iq instanceof ForwardAction)) {
            throw new PhoneActionException("Did not acquire the proper response!");
        }

    }

    /**
     * Used to see if whether a specified jid has the phone service enabled
     *
     * @param jid jid to check and see if the phone service is enabled
     * @return true if the user has the phone service
     * @throws XMPPException if there is an issue doing the disco query
     * @throws SmackException if there is an issue doing the disco query
     */
    public boolean isPhoneEnabled(String jid) throws SmackException.NotConnectedException, XMPPException.XMPPErrorException, SmackException.NoResponseException, InterruptedException {

        if (jid == null || "".equals(jid)) {
            throw new IllegalArgumentException("JID cannot be empty or null!");
        }

               if(!jid.matches(".*@.*"+conn.getXMPPServiceDomain())) {
                       return false;
               }

        DiscoverInfo info = serviceDiscoveryManager.discoverInfo(component, XmppStringUtils.parseLocalpart(jid));

        return info.containsFeature("http://jivesoftware.com/phone");
    }


    /**
     * Registers a phone event listener to this instance of the phone client
     *
     * @param listener listener to register
     */
    public void addEventListener(PhoneEventListener listener) {
        eventDispatcher.addListener(listener);
    }

    /**
     * Unregisters a phone event listener from this instance of the phone client
     *
     * @param listener listner to unregister
     */
    public void removeEventListener(PhoneEventListener listener) {
        eventDispatcher.removeListener(listener);
    }


}
