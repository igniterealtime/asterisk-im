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

}
