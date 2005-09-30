/**
 * $RCSfile: AsteriskPhoneManager.java,v $
 * $Revision: 1.13 $
 * $Date: 2005/07/02 00:22:51 $
 *
 * Copyright (C) 1999-2004 Jive Software. All rights reserved.
 *
 * This software is the proprietary information of Jive Software. Use is subject to license terms.
 */
package org.jivesoftware.phone.asterisk;

import net.sf.asterisk.manager.ManagerConnection;
import net.sf.asterisk.manager.action.*;
import net.sf.asterisk.manager.response.CommandResponse;
import net.sf.asterisk.manager.response.MailboxCountResponse;
import net.sf.asterisk.manager.response.ManagerError;
import net.sf.asterisk.manager.response.ManagerResponse;
import org.jivesoftware.phone.*;
import static org.jivesoftware.phone.asterisk.ManagerConnectionPoolFactory.getManagerConnectionPool;
import org.jivesoftware.phone.database.PhoneDAO;
import org.jivesoftware.phone.util.PhoneConstants;
import org.jivesoftware.util.JiveConstants;
import static org.jivesoftware.util.JiveGlobals.getProperty;
import org.xmpp.packet.JID;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Asterisk dependent implementation of {@link PhoneManager}
 *
 * @author Andrew Wright
 */
@PBXInfo(make = "Asterisk", version = "1.x")
public class AsteriskPhoneManager extends BasePhoneManager implements PhoneConstants {

    private static final Logger log = Logger.getLogger(AsteriskPhoneManager.class.getName());

    public AsteriskPhoneManager(PhoneDAO dao) {
        super(dao);
    }


    public void dial(String username, String extension) throws PhoneException {
        dial(username, extension, null);
    }

    public void dial(String username, JID target) throws PhoneException {

        PhoneUser targetUser = getByUsername(target.getNode());

        if (targetUser == null) {
            throw new PhoneException("User is not configured on this server");
        }

        String extension = targetUser.getPrimaryDevice().getExtension();

        if (extension == null) {
            throw new PhoneException("User has not identified a number with himself");
        }


        dial(username, extension, target);
    }

    public void forward(String callSessionID, String extension) throws PhoneException {
        forward(callSessionID, extension, null);
    }

    public void forward(String callSessionID, JID target) throws PhoneException {

        PhoneUser targetUser = getByUsername(target.getNode());

        if (targetUser == null) {
            throw new PhoneException("User is not configured on this server");
        }

        String extension = targetUser.getPrimaryDevice().getExtension();

        if (extension == null) {
            throw new PhoneException("User has not identified a number with himself");
        }

        forward(callSessionID, targetUser.getPrimaryDevice().getExtension(), target);

    }

    public void stopMonitor(String channel) throws PhoneException {

        StopMonitorAction action = new StopMonitorAction();
        action.setChannel(channel);


        ManagerConnection con = null;
        try {

            con = getManagerConnectionPool().getConnection();

            ManagerResponse managerResponse = con.sendAction(action);

            if (managerResponse instanceof ManagerError) {
                log.warning(managerResponse.getMessage());
                throw new PhoneException(managerResponse.getMessage());
            }

        }
        catch (PhoneException pe) {
            throw pe;
        }
        catch (Exception e) {
            log.log(Level.SEVERE, e.getMessage(), e);
            throw new PhoneException(e.getMessage());
        }
        finally {
            close(con);
        }
    }

    public MailboxStatus mailboxStatus(String mailbox) throws PhoneException {

        MailboxCountAction action = new MailboxCountAction();
        action.setMailbox(mailbox);

        ManagerConnection con = null;
        try {

            con = getManagerConnectionPool().getConnection();

            ManagerResponse managerResponse = con.sendAction(action);

            if (managerResponse instanceof ManagerError) {
                log.warning(managerResponse.getMessage());
                throw new PhoneException(managerResponse.getMessage());
            } else if (managerResponse instanceof MailboxCountResponse) {
                MailboxCountResponse mailboxStatus = (MailboxCountResponse) managerResponse;
                int oldMessages = mailboxStatus.getOldMessages();
                int newMessages = mailboxStatus.getNewMessages();
                return new MailboxStatus(mailbox, oldMessages, newMessages);
            } else {
                log.severe("Did not receive a MailboxCountResponseEvent!");
                throw new PhoneException("Did not receive a MailboxCountResponseEvent!");
            }

        }
        catch (PhoneException pe) {
            throw pe;
        }
        catch (Exception e) {
            log.log(Level.SEVERE, e.getMessage(), e);
            throw new PhoneException(e.getMessage());
        }
        finally {
            close(con);
        }
    }

    public void invite(String callSessionID, String extension) throws PhoneException {

        CallSession phoneSession = CallSessionFactory.getCallSessionFactory()
                .getPhoneSession(callSessionID);

        RedirectAction action = new RedirectAction();

        // the channel should be the person that called us
        action.setChannel(phoneSession.getLinkedChannel());
        action.setExtraChannel(phoneSession.getChannel());
        action.setExten(extension);
        action.setPriority(1);

        String context = getProperty(Properties.CONTEXT, DEFAULT_CONTEXT);
        if ("".equals(context)) {
            context = DEFAULT_CONTEXT;
        }

        action.setContext(context);

        ManagerConnection con = null;
        try {
            con = getManagerConnectionPool().getConnection();
            ManagerResponse managerResponse = con.sendAction(action);


            if (managerResponse instanceof ManagerError) {
                log.warning(managerResponse.getMessage());
                throw new PhoneException(managerResponse.getMessage());
            }

        }
        catch (PhoneException pe) {
            throw pe;
        }
        catch (Exception e) {
            log.log(Level.SEVERE, e.getMessage(), e);
            throw new PhoneException(e.getMessage());
        }
        finally {
            close(con);
        }

    }

    public List<String> getDevices() throws PhoneException {
        List<String> devices = getSipDevices();

        Collections.sort(devices);

        // todo Add IAX support
        return devices;
    }

    @SuppressWarnings({"unchecked"})
    protected List<String> getSipDevices() throws PhoneException {

        ManagerConnection con = null;

        try {

            con = getManagerConnectionPool().getConnection();

            CommandAction action = new CommandAction();
            action.setCommand("sip show peers");

            ManagerResponse managerResponse = con.sendAction(action);
            if (managerResponse instanceof ManagerError) {
                log.warning(managerResponse.getMessage());
                throw new PhoneException(managerResponse.getMessage());
            }

            CommandResponse response = (CommandResponse) managerResponse;
            List<String> results = response.getResult();

            ArrayList<String> list = new ArrayList<String>();
            boolean isFirst = true; // The first entry is Name, we want to skip that one
            for (String result : results) {
                if (!isFirst) {
                    list.add("SIP/" + result.split("/")[0]);
                }
                isFirst = false;
            }

            return list;

        }
        catch (Exception e) {
            log.log(Level.SEVERE, e.getMessage(), e);
            throw new PhoneException(e);
        }
        finally {
            close(con);
        }

    }

    private static void close(ManagerConnection con) {

        if (con != null) {
            try {
                con.logoff();
            }
            catch (Exception e) {
                log.log(Level.SEVERE, "trouble closing connection : " + e.getMessage(), e);
            }
        }

    }

    public void dial(String username, String extension, JID jid) throws PhoneException {

        //acquire the jidUser object
        PhoneUser user = getByUsername(username);

        ManagerConnection con = null;

        try {
            con = getManagerConnectionPool().getConnection();

            PhoneDevice primaryDevice = user.getPrimaryDevice();

            OriginateAction action = new OriginateAction();
            action.setChannel(primaryDevice.getDevice());
            action.setCallerId(primaryDevice.getCallerId() != null ? primaryDevice.getCallerId() :
                    getProperty(Properties.DEFAULT_CALLER_ID, ""));
            action.setExten(extension);
            String context = getProperty(Properties.CONTEXT, DEFAULT_CONTEXT);
            if ("".equals(context)) {
                context = DEFAULT_CONTEXT;
            }

            action.setAsync(true);
            action.setContext(context);
            action.setPriority(1);

            String variables = getProperty(Properties.DIAL_VARIABLES);

            if(variables != null) {

                String[] varArray = variables.split(",");

                StringBuffer buff = new StringBuffer();
                for (String aVarArray : varArray) {
                    buff.append(aVarArray);
                }

                action.setVariable(buff.toString());
            }

            con.sendAction(action, 5 * JiveConstants.SECOND);

            // BEWARE EVIL HACK, when you can actually get a uniqueID from the response we should use that instead
            // We will create a call session for this device and then later parse out the info
            CallSession phoneSession = CallSessionFactory.getCallSessionFactory().getPhoneSession(primaryDevice.getDevice());
            phoneSession.setCallerID(extension);

            if (jid != null) {
                phoneSession.setDialedJID(jid);
            }

        }
        catch (Exception e) {
            log.log(Level.SEVERE, e.getMessage(), e);
            throw new PhoneException("Unabled to dial extention " + extension, e);
        }
        finally {
            close(con);
        }

    }

    public void forward(String callSessionID, String extension, JID jid) throws PhoneException {


        CallSession phoneSession = CallSessionFactory.getCallSessionFactory()
                .getPhoneSession(callSessionID);

        phoneSession.setForwardedExtension(extension);
        phoneSession.setForwardedJID(jid);

        RedirectAction action = new RedirectAction();

        // The channel should be the person that called us
        action.setChannel(phoneSession.getLinkedChannel());
        action.setExten(extension);
        action.setPriority(1);


        String context = getProperty(Properties.CONTEXT, DEFAULT_CONTEXT);
        if ("".equals(context)) {
            context = DEFAULT_CONTEXT;
        }

        action.setContext(context);

        ManagerConnection con = null;
        try {
            con = getManagerConnectionPool().getConnection();
            ManagerResponse managerResponse = con.sendAction(action);


            if (managerResponse instanceof ManagerError) {
                log.warning(managerResponse.getMessage());
                throw new PhoneException(managerResponse.getMessage());
            }

        }
        catch (PhoneException pe) {
            throw pe;
        }
        catch (Exception e) {
            log.log(Level.SEVERE, e.getMessage(), e);
            throw new PhoneException(e.getMessage());
        }
        finally {
            close(con);
        }


    }


}
