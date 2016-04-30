/**
 * $RCSfile: PhoneClientDebugger.java,v $
 * $Revision: 1.5 $
 * $Date: 2005/07/05 18:41:09 $
 *
 * Copyright (C) 1999-2004 Jive Software. All rights reserved.
 *
 * This software is the proprietary information of Jive Software. Use is subject to license terms.
 */

package org.jivesoftware.phone.client;

import org.jivesoftware.phone.client.action.PhoneActionIQProvider;
import org.jivesoftware.phone.client.action.PhoneActionPacket;
import org.jivesoftware.phone.client.event.PhoneEventExtensionElement;
import org.jivesoftware.phone.client.event.PhoneEventPacketExtensionProvider;
import org.jivesoftware.smack.*;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Useful for testing the client api, however the user interface is currenlty horrible. I want to fix this one day.
 *
 * @author Andrew Wright
 */
public class PhoneClientDebugger extends JFrame implements ActionListener, PhoneEventListener {

    private static final Logger log = Logger.getLogger(PhoneClientDebugger.class.getName());

    static {
        SmackConfiguration.DEBUG = true;

        try {

            ProviderManager.addExtensionProvider("phone-event",
                    PhoneEventExtensionElement.NAMESPACE,
                    new PhoneEventPacketExtensionProvider());


            ProviderManager.addIQProvider("phone-action",
                    PhoneActionPacket.NAMESPACE,
                    new PhoneActionIQProvider());
        }
        catch (Exception e) {
            log.log(Level.SEVERE, e.getMessage(), e);
            throw new ExceptionInInitializerError(e);
        }
    }

    private PhoneClient client;
    private AbstractXMPPConnection conn;

    private JTextField input;
    private JTextField username;
    private JTextField password;
    private JTextField server;
    private Call call;

    public PhoneClientDebugger() throws Exception {
        super("Phone Client Debugger");
        getContentPane().setLayout(new BorderLayout());
        setSize(200, 150);

        getContentPane().add(buildForm());
        pack();

        setVisible(true);
    }

    private JComponent buildForm() {

        JPanel panel = new JPanel();

        panel.setLayout(new GridLayout(5, 2));

        JLabel label = new JLabel("Server");
        label.setHorizontalAlignment(JLabel.RIGHT);
        panel.add(label);
        server = new JTextField(20);
        panel.add(server);

        label = new JLabel("Username");
        label.setHorizontalAlignment(JLabel.RIGHT);
        panel.add(label);
        username = new JTextField(20);
        panel.add(username);

        label = new JLabel("Password");
        label.setHorizontalAlignment(JLabel.RIGHT);
        panel.add(label);
        password = new JTextField(20);
        panel.add(password);

        label = new JLabel("Input");
        label.setHorizontalAlignment(JLabel.RIGHT);
        panel.add(label);
        input = new JTextField(20);
        panel.add(input);

        Box buttonPanel = new Box(BoxLayout.X_AXIS);
        panel.add(buttonPanel);

        JButton button = new JButton("Connect");
        button.setActionCommand("connect");
        button.addActionListener(this);
        buttonPanel.add(button);

        button = new JButton("Disconnect");
        button.setActionCommand("disconnect");
        button.addActionListener(this);
        buttonPanel.add(button);

        button = new JButton("Exit");
        button.setActionCommand("exit");
        button.addActionListener(this);
        buttonPanel.add(button);

        buttonPanel = new Box(BoxLayout.X_AXIS);
        panel.add(buttonPanel);

        button = new JButton("Call");
        button.setActionCommand("call");
        button.addActionListener(this);
        buttonPanel.add(button);

        button = new JButton("Forward");
        button.setActionCommand("forward");
        button.addActionListener(this);
        buttonPanel.add(button);

        return panel;
    }

    public void handle(PhoneEvent event) {

        if(event instanceof  RingEvent ) {

            call = ((RingEvent) event).getCall();

        }


    }


    public void actionPerformed(ActionEvent e) {

        String command = e.getActionCommand();

        if ("call".equals(command)) {

            String extensionText = input.getText();

            if (extensionText != null && !"".equals(extensionText)) {
                try {

                    if(extensionText.indexOf("@") > -1) {
                        client.dialByJID(extensionText);
                    }
                    else {
                        client.dialByExtension(extensionText);
                    }
                }
                catch (PhoneActionException ex) {
                    log.log(Level.SEVERE, ex.getMessage(), ex);
                }
            }


        }
        else if ("connect".equals(command)) {

            try {
                conn = new XMPPTCPConnection(username.getText(), password.getText(), server.getText());
                conn.connect();
                conn.login();
                client = new PhoneClient(conn);
                client.addEventListener(this);
            }
            catch (IOException e1) {
                log.log(Level.SEVERE, e1.getMessage(), e1);
                throw new RuntimeException(e1);
            }
            catch (XMPPException e1) {
                log.log(Level.SEVERE, e1.getMessage(), e1);
                throw new RuntimeException(e1);
            }
            catch (SmackException e1) {
                log.log(Level.SEVERE, e1.getMessage(), e1);
                throw new RuntimeException(e1);
            }

        }
        else if ("disconnect".equals(command)) {

            conn.disconnect();

        }
        else if ("exit".equals(command)) {
            try {
                conn.disconnect();
            }
            finally {
                System.exit(0);
            }
        }
        else if ("forward".equals(command)) {

            String extensionText = input.getText();

            if(call != null && extensionText != null && !"".equals(extensionText)) {
                try {
                    if(extensionText.indexOf("@") > -1) {
                        client.forwardByJID(call, extensionText);
                    }
                    else {
                        client.forward(call, extensionText);
                    }
                }
                catch (PhoneActionException e1) {
                    log.log(Level.SEVERE, e1.getMessage(), e1);
                }
            }

        }

    }


    public static void main(String[] args) {
        try {
            SwingUtilities.invokeAndWait(new Runnable() {

                public void run() {
                    try {
                        new PhoneClientDebugger();
                    }
                    catch (Exception e) {
                        log.log(Level.SEVERE, e.getMessage(), e);
                        System.exit(1);
                    }
                }

            });
        }
        catch (Exception e) {
            log.log(Level.SEVERE, e.getMessage(), e);
            System.exit(1);
        }

    }

}
