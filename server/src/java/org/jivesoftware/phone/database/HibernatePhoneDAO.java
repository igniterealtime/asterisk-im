/**
 * $RCSfile: HibernatePhoneDAO.java,v $
 * $Revision: 1.15 $
 * $Date: 2005/06/30 22:23:32 $
 *
 * Copyright (C) 1999-2004 Jive Software. All rights reserved.
 *
 * This software is the proprietary information of Jive Software. Use is subject to license terms.
 */
package org.jivesoftware.phone.database;

import org.jivesoftware.phone.PhoneUser;
import org.jivesoftware.phone.PhoneDevice;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;

import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * @author Andrew Wright
 */
@DAOInfo("Hibernate")
public class HibernatePhoneDAO implements PhoneDAO {

    private static final Logger log = Logger.getLogger(HibernatePhoneDAO.class.getName());

    private Session session;

    public HibernatePhoneDAO() {
        session = HibernateUtil.getSession();
    }

    public PhoneUser getByDevice(String device) {

        PhoneUser phoneJID = null;

        try {

            String queryString = "select user from PhoneUser user join user.devices c where c.device = ?";
            Query query = session.createQuery(queryString);
            query.setString(0, device);

            phoneJID = (PhoneUser) query.uniqueResult();
        }
        catch (HibernateException e) {
            log.log(Level.SEVERE, e.getMessage(), e);
        }
        return phoneJID;
    }

    public PhoneDevice getDevice(String deviceName) {

        PhoneDevice device = null;

        try {

            String queryString = "from PhoneDevice where device = ?";
            Query query = session.createQuery(queryString);
            query.setString(0, deviceName);

            device = (PhoneDevice) query.uniqueResult();
        }
        catch (HibernateException e) {
            log.log(Level.SEVERE, e.getMessage(), e);
        }

        return device;
    }

    public PhoneUser getByUsername(String username) {

        PhoneUser phoneJID = null;

        try {

            String queryString = "from PhoneUser where username = ?";
            Query query = session.createQuery(queryString);
            query.setString(0, username);

            phoneJID = (PhoneUser) query.uniqueResult();
        }
        catch (HibernateException e) {
            log.log(Level.SEVERE, e.getMessage(), e);
        }
        return phoneJID;
    }

    public PhoneUser getByID(long id) {

        PhoneUser phoneJID = null;
        try {
            phoneJID = (PhoneUser) session.get(PhoneUser.class, id);
        }
        catch (Exception e) {
            log.log(Level.SEVERE, e.getMessage(), e);
        }

        return phoneJID;
    }

    public void remove(PhoneUser phoneJid) {

        try {
            session.delete(phoneJid);
        }
        catch (HibernateException e) {
            log.log(Level.SEVERE, e.getMessage(), e);
        }

    }

    public void save(PhoneUser phoneJid) {

        try {
            session.saveOrUpdate(phoneJid);
        }
        catch (HibernateException e) {
            log.log(Level.SEVERE, e.getMessage(), e);
        }

    }

    @SuppressWarnings({"unchecked"})
    public List<PhoneUser> getALL() {

        try {
            return session.createQuery("from PhoneUser user order by user.username asc").list();
        }
        catch (HibernateException e) {
            log.log(Level.SEVERE, e.getMessage(), e);
            return Collections.emptyList();
        }

    }


    public void close() {
        try {
            session.flush();
        }
        finally {
            HibernateUtil.close(session);
        }
    }
}
