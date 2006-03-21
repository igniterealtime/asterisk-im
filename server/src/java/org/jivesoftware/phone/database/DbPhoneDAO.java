/**
 * $RCSfile: DbPhoneDAO.java,v $
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
import org.jivesoftware.database.DbConnectionManager;
import org.jivesoftware.database.SequenceManager;
import org.jivesoftware.util.Log;

import java.util.List;
import java.util.ArrayList;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.sql.PreparedStatement;


/**
 * @author Andrew Wright
 */
@DAOInfo("JDBC")
public class DbPhoneDAO implements PhoneDAO {

    public PhoneUser getPhoneUserByDevice(String device) {

        String sql = "SELECT phoneUser.userID, phoneUser.username from phoneUser, phoneDevice " +
                "WHERE phoneUser.userID = phoneDevice.userID AND phoneDevice.device = ? ";

        PhoneUser phoneUser = null;
        Connection con = null;
        PreparedStatement psmt = null;
        ResultSet rs = null;

        try {

            con = DbConnectionManager.getConnection();
            psmt = con.prepareStatement(sql);
            psmt.setString(1, device);
            rs = psmt.executeQuery();

            if (rs.next()) {
                phoneUser = read(new PhoneUser(), rs);

            }

        }
        catch (SQLException e) {
            Log.error(e.getMessage(), e);
        }
        finally {
            DbConnectionManager.closeConnection(rs, psmt, con);
        }
        return phoneUser;
    }

    public PhoneDevice getDevice(String deviceName) {

        String sql = "SELECT deviceID, device, extension, callerId, userID " +
                "from phoneDevice WHERE device = ?";

        PhoneDevice device = null;
        Connection con = null;
        PreparedStatement psmt = null;
        ResultSet rs = null;

        try {
            con = DbConnectionManager.getConnection();
            psmt = con.prepareStatement(sql);
            psmt.setString(1, deviceName);
            rs = psmt.executeQuery();

            if (rs.next()) {
                device = read(new PhoneDevice(), rs);
            }

        }
        catch (SQLException e) {
            Log.error(e.getMessage(), e);
        }
        finally {
            DbConnectionManager.closeConnection(rs, psmt, con);
        }

        return device;
    }

    public PhoneUser getByUsername(String username) {

        String sql = "SELECT phoneUser.userID, phoneUser.username from phoneUser " +
                "WHERE phoneUser.username = ?";

        PhoneUser phoneUser = null;
        Connection con = null;
        PreparedStatement psmt = null;
        ResultSet rs = null;

        try {

            con = DbConnectionManager.getConnection();
            psmt = con.prepareStatement(sql);
            psmt.setString(1, username);
            rs = psmt.executeQuery();

            if (rs.next()) {
                phoneUser = read(new PhoneUser(), rs);
            }

        }
        catch (SQLException e) {
            Log.error(e.getMessage(), e);
        }
        finally {
            DbConnectionManager.closeConnection(rs, psmt, con);
        }
        return phoneUser;
    }

    public PhoneUser getPhoneUserByID(long id) {

        String sql = "SELECT phoneUser.userID, phoneUser.username from phoneUser " +
                "WHERE phoneUser.userID = ?";

        PhoneUser phoneUser = null;
        Connection con = null;
        PreparedStatement psmt = null;
        ResultSet rs = null;

        try {

            con = DbConnectionManager.getConnection();
            psmt = con.prepareStatement(sql);
            psmt.setLong(1, id);
            rs = psmt.executeQuery();

            if (rs.next()) {
                phoneUser = read(new PhoneUser(), rs);
            }

        }
        catch (SQLException e) {
            Log.error(e.getMessage(), e);
        }
        finally {
            DbConnectionManager.closeConnection(rs, psmt, con);
        }
        return phoneUser;
    }

    public void remove(PhoneUser phoneUser) {

        String sql = "DELETE FROM phoneDevice WHERE userID = ?";

        Connection con = null;
        PreparedStatement psmt = null;

        try {
            con = DbConnectionManager.getConnection();
            psmt = con.prepareStatement(sql);
            psmt.setLong(1, phoneUser.getID());
            psmt.executeUpdate();
            psmt.close();

            sql = "DELETE FROM phoneUser WHERE userID = ?";

            con = DbConnectionManager.getConnection();
            psmt = con.prepareStatement(sql);
            psmt.setLong(1, phoneUser.getID());
            psmt.executeUpdate();
        }
        catch (SQLException e) {
            Log.error(e.getMessage(), e);
        }
        finally {
            DbConnectionManager.closeConnection(psmt, con);
        }

    }

    public PhoneDevice getPhoneDeviceByID(long id) {
        String sql = "SELECT deviceID, device, extension, callerId, isPrimary, userID " +
                "FROM phoneDevice WHERE deviceID = ?";

        PhoneDevice device = null;
        Connection con = null;
        PreparedStatement psmt = null;
        ResultSet rs = null;

        try {
            con = DbConnectionManager.getConnection();
            psmt = con.prepareStatement(sql);
            psmt.setLong(1, id);
            rs = psmt.executeQuery();

            if (rs.next()) {
                device = read(new PhoneDevice(), rs);
            }
        }
        catch (SQLException e) {
            Log.error(e.getMessage(), e);
        }
        finally {
            DbConnectionManager.closeConnection(rs, psmt, con);
        }

        return device;
    }

    public List<PhoneUser> getPhoneUsers() {

        String sql = "SELECT userID from phoneUser";

        ArrayList<PhoneUser> list = new ArrayList<PhoneUser>();
        Connection con = null;
        PreparedStatement psmt = null;
        ResultSet rs = null;

        try {
            con = DbConnectionManager.getConnection();
            psmt = con.prepareStatement(sql);
            rs = psmt.executeQuery();

            while (rs.next()) {
                long id = rs.getLong(1);
                PhoneUser user = getPhoneUserByID(id);

                if (user != null) {
                    list.add(user);
                }

            }
        }
        catch (SQLException e) {
            Log.error(e.getMessage(), e);
        }
        finally {
            DbConnectionManager.closeConnection(rs, psmt, con);
        }

        return list;
    }

    public List<PhoneDevice> getPhoneDeviceByUserID(long userID) {

        String sql = "SELECT deviceID FROM phoneDevice WHERE userID = ?";

        ArrayList<PhoneDevice> list = new ArrayList<PhoneDevice>();
        Connection con = null;
        PreparedStatement psmt = null;
        ResultSet rs = null;

        try {
            con = DbConnectionManager.getConnection();
            psmt = con.prepareStatement(sql);
            psmt.setLong(1, userID);
            rs = psmt.executeQuery();

            while (rs.next()) {
                long id = rs.getLong(1);
                PhoneDevice device = getPhoneDeviceByID(id);
                if (device != null) {
                    list.add(device);
                }
            }

        }
        catch (SQLException sqle) {
            Log.error(sqle.getMessage(), sqle);
        }
        finally {
            DbConnectionManager.closeConnection(rs, psmt, con);
        }

        return list;
    }

    public void insert(final PhoneUser user) {

        String sql = "INSERT INTO phoneUser (userID, username) VALUES (?,?)";

        PreparedStatement psmt = null;
        Connection con = null;

        long id = SequenceManager.nextID(user);

        try {
            con = DbConnectionManager.getConnection();
            psmt = con.prepareStatement(sql);
            psmt.setLong(1, id);
            psmt.setString(2, user.getUsername());
            psmt.executeUpdate();

            user.setID(id);
        }
        catch (SQLException e) {
            Log.error(e.getMessage(), e);
        }
        finally {
            DbConnectionManager.closeConnection(psmt, con);
        }
    }

    public void update(PhoneUser user) {

        String sql = "UPDATE phoneUser SET username = ? WHERE userID = ?";

        PreparedStatement psmt = null;
        Connection con = null;

        try {
            con = DbConnectionManager.getConnection();
            psmt = con.prepareStatement(sql);
            psmt.setString(1, user.getUsername());
            psmt.setLong(2, user.getID());
            psmt.executeUpdate();
        }
        catch (SQLException e) {
            Log.error(e.getMessage(), e);
        }
        finally {
            DbConnectionManager.closeConnection(psmt, con);
        }

    }

    public void update(PhoneDevice device) {

        String sql = "UPDATE phoneDevice SET extension = ?, callerId = ?, isPrimary = ? " +
                "WHERE deviceID  = ?";

        PreparedStatement psmt = null;
        Connection con = null;

        try {
            con = DbConnectionManager.getConnection();
            psmt = con.prepareStatement(sql);
            psmt.setString(1, device.getExtension());
            psmt.setString(2, device.getCallerId());
            psmt.setBoolean(3, device.isPrimary());
            psmt.setLong(4, device.getID());
            psmt.executeUpdate();
        }
        catch (SQLException e) {
            Log.error(e.getMessage(), e);
        }
        finally {
            DbConnectionManager.closeConnection(psmt, con);
        }
    }

    public void insert(PhoneDevice device) {

        String sql = "INSERT INTO phoneDevice " +
                "(deviceID, extension, callerId, isPrimary, userID, device) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        long id = SequenceManager.nextID(device);

        PreparedStatement psmt = null;
        Connection con = null;

        try {
            con = DbConnectionManager.getConnection();
            psmt = con.prepareStatement(sql);
            psmt.setLong(1, id);
            psmt.setString(2, device.getExtension());
            psmt.setString(3, device.getCallerId());
            psmt.setBoolean(4, device.isPrimary());
            psmt.setLong(5, device.getPhoneUserID());
            psmt.setString(6, device.getDevice());
            psmt.executeUpdate();
            device.setID(id);
        }
        catch (SQLException e) {
            Log.error(e.getMessage(), e);
        }
        finally {
            DbConnectionManager.closeConnection(psmt, con);
        }

    }

    public void remove(PhoneDevice device) {
        String sql = "DELETE FROM phoneDevice WHERE deviceID = ?";

        PreparedStatement psmt = null;
        Connection con = null;

        try {
            con = DbConnectionManager.getConnection();
            psmt = con.prepareStatement(sql);
            psmt.setLong(1, device.getID());
            psmt.executeUpdate();
        }
        catch (SQLException e) {
            Log.error(e.getMessage(), e);
        }
        finally {
            DbConnectionManager.closeConnection(psmt, con);
        }
    }

    public PhoneDevice getPrimaryDevice(long phoneUserID) {

        String sql = "SELECT deviceID, device, extension, callerId, isPrimary, userID " +
                "FROM phoneDevice WHERE userID = ? AND isPrimary = 1";

        PhoneDevice device = null;
        Connection con = null;
        PreparedStatement psmt = null;
        ResultSet rs = null;

        try {
            con = DbConnectionManager.getConnection();
            psmt = con.prepareStatement(sql);
            psmt.setLong(1, phoneUserID);
            rs = psmt.executeQuery();

            if (rs.next()) {
                device = read(new PhoneDevice(), rs);
            }
        }
        catch (SQLException e) {
            Log.error(e.getMessage(), e);
        }
        finally {
            DbConnectionManager.closeConnection(rs, psmt, con);
        }

        return device;
    }

    public List<PhoneDevice> getPhoneDevicesByUsername(String username) {
        String sql = "SELECT deviceID FROM phoneDevice, phoneUser " +
                "WHERE phoneDevice.userID = phoneUser.userID AND phoneUser.username = ?";

        ArrayList<PhoneDevice> list = new ArrayList<PhoneDevice>();
        Connection con = null;
        PreparedStatement psmt = null;
        ResultSet rs = null;

        try {
            con = DbConnectionManager.getConnection();
            psmt = con.prepareStatement(sql);
            psmt.setString(1, username);
            rs = psmt.executeQuery();

            while (rs.next()) {
                long id = rs.getLong(1);
                PhoneDevice device = getPhoneDeviceByID(id);
                if (device != null) {
                    list.add(device);
                }
            }

        }
        catch (SQLException sqle) {
            Log.error(sqle.getMessage(), sqle);
        }
        finally {
            DbConnectionManager.closeConnection(rs, psmt, con);
        }

        return list;
    }

    private PhoneDevice read(PhoneDevice device, ResultSet rs) throws SQLException {
        device.setID(rs.getLong("deviceID"));
        device.setPhoneUserID(rs.getLong("userID"));
        device.setDevice(rs.getString("device"));
        device.setExtension(rs.getString("extension"));
        device.setCallerId(rs.getString("callerId"));
        device.setPrimary(rs.getBoolean("isPrimary"));
        return device;
    }

    private PhoneUser read(PhoneUser user, ResultSet rs) throws SQLException {
        user.setID(rs.getLong("userID"));
        user.setUsername(rs.getString("username"));
        return user;
    }


}
