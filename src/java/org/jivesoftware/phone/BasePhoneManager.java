/**
 * Copyright (C) 1999-2004 Jive Software. All rights reserved.
 * Copyright (C) 2006 headissue GmbH; Jens Wilke. All rights reserved.
 *
 * This software is the proprietary information of Jive Software. Use is subject to license terms.
 */
package org.jivesoftware.phone;

import org.jivesoftware.phone.database.PhoneDAO;
import org.jivesoftware.util.Log;
import org.jivesoftware.wildfire.ClientSession;
import org.jivesoftware.wildfire.SessionManager;
import org.jivesoftware.wildfire.XMPPServer;
import org.xmpp.packet.JID;

import java.util.Collection;
import java.util.List;

/**
 * Base class for PhoneManagers that handles non pbx dependent code
 *
 * @author Andrew Wright
 */
public abstract class BasePhoneManager implements PhoneManager {

    private PhoneDAO phoneDAO;
	protected XMPPServer server = XMPPServer.getInstance();

    protected BasePhoneManager(PhoneDAO phoneDAO) {
        this.phoneDAO = phoneDAO;
    }

    public PhoneUser getPhoneUserByDevice(String device) {
        return phoneDAO.getPhoneUserByDevice(device);
    }

    public PhoneUser getPhoneUserByUsername(String username) {
        return phoneDAO.getByUsername(username);
    }

    public List<PhoneUser> getAllPhoneUsers() {
        return phoneDAO.getPhoneUsers();
    }

    public void remove(PhoneUser phoneJid) {
        phoneDAO.remove(phoneJid);
    }

    public void remove(PhoneDevice phoneDevice) {
        phoneDAO.remove(phoneDevice);
    }

    public void update(PhoneDevice phoneDevice) {
        phoneDAO.update(phoneDevice);
    }

    public void update(PhoneUser phoneUser) {
        phoneDAO.update(phoneUser);
    }

    public PhoneUser getPhoneUserByID(long phoneUserID) {
        return phoneDAO.getPhoneUserByID(phoneUserID);
    }

    public Collection<PhoneDevice> getAllPhoneDevices() {
        return phoneDAO.getPhoneDevices();
    }

    public List<PhoneDevice> getPhoneDevicesByUserID(long phoneUserID) {
        return phoneDAO.getPhoneDeviceByUserID(phoneUserID);
    }

    public List<PhoneDevice> getPhoneDevicesByUsername(String username) {
        return phoneDAO.getPhoneDevicesByUsername(username);
    }

    public PhoneDevice getPhoneDeviceByID(long phoneDeviceID) {
        return phoneDAO.getPhoneDeviceByID(phoneDeviceID);
    }

    public PhoneDevice getPrimaryDevice(long phoneUserID) {
        return phoneDAO.getPrimaryDevice(phoneUserID);
    }

    public PhoneDevice getDevice(String device) {
        return phoneDAO.getDevice(device);
    }

    public void insert(PhoneUser phoneUser) {
        phoneDAO.insert(phoneUser);
    }

    public void insert(PhoneDevice phoneDevice) {
        phoneDAO.insert(phoneDevice);
    }

    protected PhoneDAO getPhoneDAO() {
        return phoneDAO;
    }

    public PhoneDevice getPhoneDeviceByDevice(String device) {
        return phoneDAO.getDevice(device);
    }

    public Collection<PhoneServer> getPhoneServers() {
        return phoneDAO.getPhoneServers();
    }

    public PhoneServer createPhoneServer(String name, String serverAddress, int port,
                                         String username, String password)
    {
        if(name == null || serverAddress == null || port < 0 || username == null ||
                password == null) {
            return null;
        }
        PhoneServer server = new PhoneServer();
        server.setName(name);
        server.setHostname(serverAddress);
        server.setUsername(username);
        server.setPassword(password);
        server.setPort(port);

        phoneDAO.insert(server);
        return server;
    }

    public PhoneServer getPhoneServerByID(long serverID) {
        return phoneDAO.getPhoneServerByID(serverID);
    }

    public PhoneServer updatePhoneServer(long serverID, String serverName, String serverAddress,
                                         int serverPort, String username, String password) {
        if (serverName == null || serverAddress == null || serverPort <= 0 || username == null ||
                password == null || serverID <= 0) {
            return null;
        }
        PhoneServer server = new PhoneServer();
        server.setID(serverID);
        server.setName(serverName);
        server.setHostname(serverAddress);
        server.setUsername(username);
        server.setPassword(password);
        server.setPort(serverPort);

        phoneDAO.update(server);
        return server;
    }

    public void removePhoneServer(long serverID) {
        Collection<PhoneDevice> devices = getPhoneDevicesByServerID(serverID);
        for (PhoneDevice device : devices) {
            remove(device);
        }
        phoneDAO.removePhoneServer(serverID);
    }

    /** FIXME: rename to originate ;jw */
    public abstract void dial(String username, String extension, JID jid) throws PhoneException;
    
    public abstract void forward(String callSessionID, String username, String extension, JID jid)
            throws PhoneException;

    
    public void originate(String username, String extension) throws PhoneException {
        dial(username, extension, null);
    }

    public void originate(String username, JID target) throws PhoneException {

        PhoneUser targetUser = getPhoneUserByUsername(target.getNode());

        if (targetUser == null) {
            throw new PhoneException("User is not configured on this server");
        }

        String extension = getPrimaryDevice(targetUser.getID()).getExtension();

        if (extension == null) {
            throw new PhoneException("User has not identified a number with himself");
        }


        dial(username, extension, target);
    }

    public void forward(String callSessionID, String username, String extension) throws PhoneException {
        forward(callSessionID, username, extension, null);
    }

    public void forward(String callSessionID, String username, JID target) throws PhoneException {

        PhoneUser targetUser = getPhoneUserByUsername(target.getNode());

        if (targetUser == null) {
            throw new PhoneException("User is not configured on this server");
        }

        PhoneDevice primaryDevice = getPrimaryDevice(targetUser.getID());

        String extension = primaryDevice.getExtension();

        if (extension == null) {
            throw new PhoneException("User has not identified a number with himself");
        }

        forward(callSessionID, username, extension, target);

    }
    
	public PhoneUser getActivePhoneUserByDevice(String device) {
		// If there is no jid for this device don't do anything else
		PhoneUser phoneUser = getPhoneUserByDevice(device);
		if (phoneUser == null) {
			Log.info("OnPhoneTask: Could not find device/jid mapping for device "
                    + device + " returning");
			return null;
		}
		Log.info("OnPhoneTask called for user " + phoneUser);
        // Acquire the xmpp sessions for the user
        SessionManager sessionManager = server.getSessionManager();
        Collection<ClientSession> sessions = sessionManager.getSessions(phoneUser.getUsername());
        // We don't care about people without a session
        if (sessions.size() == 0) {
        	Log.info("no sessions");
            return null;
        }
        return phoneUser;
	}

    public Collection<PhoneDevice> getPhoneDevicesByServerID(long serverID) {
        return phoneDAO.getPhoneDevicesByServerID(serverID);
    }

    public PhoneServer getPhoneServerByDevice(String device) {
        return phoneDAO.getPhoneServerByID(phoneDAO.getDevice(device).getServerID());
    }

}
