/**
 * Copyright (C) 1999-2004 Jive Software. All rights reserved.
 *
 * This software is the proprietary information of Jive Software. Use is subject to license terms.
 */
package org.jivesoftware.phone;

import org.jivesoftware.phone.database.PhoneDAO;

import java.util.List;

/**
 * Base class for PhoneManagers that handles non pbx dependent code
 *
 * @author Andrew Wright
 */
public abstract class BasePhoneManager implements PhoneManager {

    private PhoneDAO phoneDAO;

    protected BasePhoneManager(PhoneDAO phoneDAO) {
        this.phoneDAO = phoneDAO;
    }

    public PhoneUser getByDevice(String device) {
        return phoneDAO.getByDevice(device);
    }

    public PhoneUser getByUsername(String username) {
        return phoneDAO.getByUsername(username);
    }

    public List<PhoneUser> getAll() {
        return phoneDAO.getALL();
    }

    public void remove(PhoneUser phoneJid) {
        phoneDAO.remove(phoneJid);
    }

    public void save(PhoneUser phoneJid) {
        phoneDAO.save(phoneJid);
    }

    public PhoneUser getByID(long phoneUserID) {
        return phoneDAO.getByID(phoneUserID);
    }

    public PhoneDevice getDevice(String device) {
        return phoneDAO.getDevice(device);
    }

    public void close() {
        phoneDAO.close();
    }

    protected PhoneDAO getPhoneDAO() {
        return phoneDAO;
    }

}
