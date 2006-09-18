/**
 * $RCSfile: PacketHandler.java,v $
 * $Revision: 1.20 $
 * $Date: 2005/07/05 18:41:09 $
 *
 * Copyright (C) 1999-2004 Jive Software. All rights reserved.
 *
 * This software is the proprietary information of Jive Software. Use is subject to license terms.
 */
package org.jivesoftware.phone.xmpp;

import org.jivesoftware.phone.util.PhoneConstants;
import org.jivesoftware.phone.xmpp.element.PhoneAction;
import org.jivesoftware.phone.PhoneManager;
import org.jivesoftware.phone.PhonePlugin;
import org.jivesoftware.phone.PhoneException;
import org.jivesoftware.phone.PhoneUser;
import org.jivesoftware.util.Log;
import org.dom4j.Element;
import org.xmpp.packet.*;

import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Processes the packets the come into the phone component
 * 
 * @author Andrew Wright
 */
public class PacketHandler implements PhoneConstants {

    private static final Logger log = Logger.getLogger(PacketHandler.class.getName());

    private PhoneManager phoneManager;
    private PhonePlugin plugin;

    public PacketHandler(PhoneManager pm, PhonePlugin pi) {
        this.phoneManager = pm;
        this.plugin = pi;
    }
    
    public void processPacket(IQ iq) {


        Element element = iq.getChildElement();
        String namespace = element.getNamespaceURI();

        if (NAMESPACE.equals(namespace)) {
            String type = element.attributeValue("type");

            if (PhoneAction.Type.DIAL.name().equals(type)) {
                handleDial(iq);
            }
            else if (PhoneAction.Type.FORWARD.name().equals(type)) {
                handleForward(iq);
            }

        }
        else if ("http://jabber.org/protocol/disco#info".equals(namespace)) {
            handleDisco(iq);
        }
        else {
            // We were given a packet we don't know how to handle, send an error back
            IQ reply = IQ.createResultIQ(iq);
            reply.setType(IQ.Type.error);
            PacketError error = new PacketError(PacketError.Condition.feature_not_implemented,
                    PacketError.Type.cancel,
                    "Unknown operation");
            reply.setError(error);
            send(reply);
        }

    }

    public void handleDial(IQ iq) {
        JID jid = iq.getFrom();

        Element phoneElement = iq.getChildElement();

        try {

            String extension = phoneElement.elementText("extension");
            if (extension != null) {
                phoneManager.originate(jid.getNode(), extension);
            }
            // try dialing by jid
            else {
                String targetJID = phoneElement.elementText("jid");
                if (targetJID == null) {
                    throw new PhoneException("No extension or jid was specified");
                }

                phoneManager.originate(jid.getNode(), new JID(targetJID));
            }

            //send reply
            IQ reply = IQ.createResultIQ(iq);
            reply.setType(IQ.Type.result);

            PhoneAction phoneAction = new PhoneAction(PhoneAction.Type.DIAL);
            reply.setChildElement(phoneAction);

            send(reply);

        }
        catch (PhoneException e) {
            Log.debug(e);
            IQ reply = IQ.createResultIQ(iq);
            reply.setType(IQ.Type.error);
            PacketError error = new PacketError(PacketError.Condition.undefined_condition,
                    PacketError.Type.cancel,
                    e.getMessage());
            reply.setError(error);
            send(reply);
        }
    }

    public void handleForward(IQ iq) {

        Element phoneElement = iq.getChildElement();

        try {
            String callSessionID = phoneElement.attributeValue("id");

            if (callSessionID == null || "".equals(callSessionID)) {
                throw new PhoneException("a call 'id' is a required attribute for type FORWARD");
            }

            String extension = phoneElement.elementText("extension");
            if (extension != null && !"".equals(extension)) {
                phoneManager.forward(callSessionID, iq.getFrom().getNode(), extension);
            }
            // try dialing by jid
            else {
                String targetJID = phoneElement.elementText("jid");
                if (targetJID == null) {
                    throw new PhoneException("No extension or jid was specified");
                }

                iq.getFrom().getNode();

                phoneManager.forward(callSessionID, iq.getFrom().getNode(), new JID(targetJID));
            }

            //send reply
            IQ reply = IQ.createResultIQ(iq);
            reply.setType(IQ.Type.result);

            PhoneAction phoneAction = new PhoneAction(PhoneAction.Type.FORWARD);
            reply.setChildElement(phoneAction);

            send(reply);

        }
        catch (PhoneException e) {
            Log.debug(e);
            IQ reply = IQ.createResultIQ(iq);
            reply.setType(IQ.Type.error);
            PacketError error = new PacketError(PacketError.Condition.undefined_condition,
                    PacketError.Type.cancel,
                    e.getMessage());
            reply.setError(error);
            send(reply);
        }

    }

    public void handleDisco(IQ iq) {

        if (iq.getType().equals(IQ.Type.error)) {
            // Log.info("Received disco error - " + iq);
            return;
        }

        if (!(iq.getType() == IQ.Type.get || iq.getType() == IQ.Type.set)) {
            // Log.debug("Not set or get - " + iq);
            return;
        }

        // if information was sent to the component itself
        if (plugin.getComponentJID().equals(iq.getTo())) {

            //try to see if there is a node on the query
            Element child = iq.getChildElement();
            String node = child.attributeValue("node");

            // category - directory (since searching is possible)
            // type - phone

            // features
            // var - http://jabber.org/protocol/disco#info
            // var - jabber:iq:version

            IQ reply = IQ.createResultIQ(iq);
            reply.setType(IQ.Type.result);
            reply.setChildElement(iq.getChildElement().createCopy());

            Element queryElement = reply.getChildElement();

            // Create and add the identity
            Element identity = queryElement.addElement("identity");
            identity.addAttribute("category", "directory");
            identity.addAttribute("type", "phone");
            identity.addAttribute("name", "phone");

            // Create and add a the feature provided by the workgroup
            // Create and add a the disco#info feature
            Element feature = queryElement.addElement("feature");
            feature.addAttribute("var", "http://jabber.org/protocol/disco#info");

            if (node == null) {

                // Indicate that we can provide information about the software version being used
                feature = queryElement.addElement("feature");
                feature.addAttribute("var", "jabber:iq:version");

            }
            else {
                // This is a query against a specific user
                try {

                    PhoneUser user = phoneManager.getPhoneUserByUsername(node);

                    // if there is a user they have support
                    if (user != null) {

                        // var http://jivesoftware.com/xmlns/phone
                        feature = queryElement.addElement("feature");
                        feature.addAttribute("var", "http://jivesoftware.com/phone");

                    }
                }
                catch (Exception e) {
                    log.log(Level.SEVERE, e.getMessage(), e);
                }

            }
            send(reply);
        }
        else {
            // todo implement
        }


    }

    private void send(Packet packet) {
        plugin.sendPacket(packet);
    }


}