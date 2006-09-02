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

import org.jivesoftware.database.DbConnectionManager;
import org.jivesoftware.database.SequenceManager;
import org.jivesoftware.phone.PhoneDevice;
import org.jivesoftware.phone.PhoneUser;
import org.jivesoftware.phone.PhoneServer;
import org.jivesoftware.util.Log;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Collection;
import java.util.Collections;


/**
 * JBDC implemention of {@link PhoneDAO} that uses the database to access phone information.
 *
 * @author Andrew Wright
 * @since 1.1
 */
@DAOInfo(DAOInfo.daoType.JDBC)
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

        String sql = "SELECT deviceID, device, extension, callerId, isPrimary, userID, serverID " +
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
        String sql = "SELECT deviceID, device, extension, callerId, isPrimary, userID, serverID " +
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

        String sql = "UPDATE phoneDevice SET extension = ?, callerId = ?, isPrimary = ?, " +
                " device = ?, serverID = ? WHERE deviceID  = ?";

        PreparedStatement psmt = null;
        Connection con = null;

        try {
            con = DbConnectionManager.getConnection();
            psmt = con.prepareStatement(sql);
            psmt.setString(1, device.getExtension());
            psmt.setString(2, device.getCallerId());
            psmt.setLong(3, device.isPrimary() ? 1 : 0);
            psmt.setString(4, device.getDevice());
            psmt.setLong(5, device.getID());
            psmt.setLong(6, device.getServerID());
            psmt.executeUpdate();
        }
        catch (SQLException e) {
            Log.error(e.getMessage(), e);
        }
        finally {
            DbConnectionManager.closeConnection(psmt, con);
        }
    }

    public void update(PhoneServer server) {
        String sql = "UPDATE phoneServer SET serverName = ?, hostname = ?, port = ?, " +
                "username = ?, password = ? WHERE serverID = ?";

        PreparedStatement psmt = null;
        Connection con = null;

        try {
            con = DbConnectionManager.getConnection();
            psmt = con.prepareStatement(sql);
            psmt.setString(1, server.getName());
            psmt.setString(2, server.getHostname());
            psmt.setInt(3, server.getPort());
            psmt.setString(4, server.getUsername());
            psmt.setString(5, server.getPassword());
            psmt.setLong(6, server.getID());
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
                "(deviceID, extension, callerId, isPrimary, userID, device, serverID) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";

        long id = SequenceManager.nextID(device);

        PreparedStatement psmt = null;
        Connection con = null;

        try {
            con = DbConnectionManager.getConnection();
            psmt = con.prepareStatement(sql);
            psmt.setLong(1, id);
            psmt.setString(2, device.getExtension());
            psmt.setString(3, device.getCallerId());
            psmt.setLong(4, device.isPrimary() ? 1 : 0);
            psmt.setLong(5, device.getPhoneUserID());
            psmt.setString(6, device.getDevice());
            psmt.setLong(7, device.getServerID());
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

    public void insert(PhoneServer server) {
        String sql = "INSERT INTO phoneServer (serverID, serverName, hostname, port, username, " +
                "password) VALUES (?, ?, ?, ?, ?, ?)";

        PreparedStatement psmt = null;
        Connection con = null;

        long id = SequenceManager.nextID(server);

        try {
            con = DbConnectionManager.getConnection();
            psmt = con.prepareStatement(sql);
            psmt.setLong(1, id);
            psmt.setString(2, server.getName());
            psmt.setString(3, server.getHostname());
            psmt.setInt(4, server.getPort());
            psmt.setString(5, server.getUsername());
            psmt.setString(6, server.getPassword());
            psmt.executeUpdate();
            server.setID(id);
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

    public void removePhoneServer(long serverID) {
        String sql = "DELETE FROM phoneServer WHERE serverID = ?";

        PreparedStatement psmt = null;
        Connection con = null;

        try {
            con = DbConnectionManager.getConnection();
            psmt = con.prepareStatement(sql);
            psmt.setLong(1, serverID);
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

        String sql = "SELECT deviceID, device, extension, callerId, isPrimary, userID, serverID " +
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

    public PhoneServer getPhoneServerByServerName(String serverName) {
        String sql = "SELECT serverID, serverName, hostname, port, username, password " +
                "FROM phoneServer WHERE name = ?";

        PhoneServer server = null;
        PreparedStatement psmt = null;
        Connection con = null;
        ResultSet rs = null;
        try {
            con = DbConnectionManager.getConnection();
            psmt = con.prepareStatement(sql);
            psmt.setString(1, serverName);
            rs = psmt.executeQuery();

            if (rs.next()) {
                server = readServer(rs);
            }
        }
        catch (SQLException e) {
            Log.error(e);
        }
        finally {
            DbConnectionManager.closeConnection(rs, psmt, con);
        }

        return server;
    }

    public PhoneServer getPhoneServerByID(long id) {
        String sql = "SELECT serverID, serverName, hostname, port, username, password FROM phoneServer " +
                "WHERE serverID = ?";

        PhoneServer server = null;
        PreparedStatement psmt = null;
        Connection con = null;
        ResultSet rs = null;
        try {
            con = DbConnectionManager.getConnection();
            psmt = con.prepareStatement(sql);
            psmt.setLong(1, id);
            rs = psmt.executeQuery();

            if (rs.next()) {
                server = readServer(rs);
            }
        }
        catch (SQLException e) {
            Log.error(e);
        }
        finally {
            DbConnectionManager.closeConnection(rs, psmt, con);
        }

        return server;
    }

    public Collection<PhoneServer> getPhoneServers() {
        String sql = "SELECT serverID, serverName, hostname, port, username, password FROM phoneServer";

        List<PhoneServer> servers = new ArrayList<PhoneServer>();
        PreparedStatement psmt = null;
        Connection con = null;
        ResultSet rs = null;
        try {
            con = DbConnectionManager.getConnection();
            psmt = con.prepareStatement(sql);
            rs = psmt.executeQuery();

            while (rs.next()) {
                servers.add(readServer(rs));
            }
        }
        catch (SQLException e) {
            Log.error(e);
        }
        finally {
            DbConnectionManager.closeConnection(rs, psmt, con);
        }

        return Collections.unmodifiableCollection(servers);
    }

    public Collection<PhoneDevice> getPhoneDevicesByServerName(String serverName) {
        String sql = "SELECT serverID FROM phoneServer WHERE name = ?";

        Connection con = null;
        PreparedStatement psmt = null;
        ResultSet rs = null;
        long serverID = -1;
        try {
            con = DbConnectionManager.getConnection();
            psmt = con.prepareStatement(sql);
            psmt.setString(1, serverName);
            rs = psmt.executeQuery();

            if (rs.next()) {
                serverID = rs.getLong(1);
            }
        }
        catch (SQLException e) {
            Log.error(e.getMessage(), e);
        }
        finally {
            DbConnectionManager.closeConnection(rs, psmt, con);
        }

        if(serverID <= 0) {
            //noinspection unchecked
            return Collections.EMPTY_LIST;
        }
        else {
            return getPhoneDevicesByServerID(serverID);
        }
    }

    public Collection<PhoneDevice> getPhoneDevicesByServerID(long serverID) {
        String sql = "SELECT deviceID, device, extension, callerId, isPrimary, userID, serverID " +
                "FROM phoneDevice WHERE serverID = ?";

        Connection con = null;
        PreparedStatement psmt = null;
        ResultSet rs = null;
        List<PhoneDevice> devices = new ArrayList<PhoneDevice>();
        try {
            con = DbConnectionManager.getConnection();
            psmt = con.prepareStatement(sql);
            psmt.setLong(1, serverID);
            rs = psmt.executeQuery();

            while (rs.next()) {
                devices.add(read(new PhoneDevice(), rs));
            }
        }
        catch (SQLException e) {
            Log.error(e.getMessage(), e);
        }
        finally {
            DbConnectionManager.closeConnection(rs, psmt, con);
        }

        return Collections.unmodifiableCollection(devices);
    }

    public Collection<PhoneDevice> getPhoneDevices() {
        String sql = "SELECT deviceID, device, extension, callerId, isPrimary, userID, serverID " +
                "FROM phoneDevice";

        Connection con = null;
        PreparedStatement psmt = null;
        ResultSet rs = null;
        List<PhoneDevice> devices = new ArrayList<PhoneDevice>();
        try {
            con = DbConnectionManager.getConnection();
            psmt = con.prepareStatement(sql);
            rs = psmt.executeQuery();

            while (rs.next()) {
                devices.add(read(new PhoneDevice(), rs));
            }
        }
        catch (SQLException e) {
            Log.error(e.getMessage(), e);
        }
        finally {
            DbConnectionManager.closeConnection(rs, psmt, con);
        }

        return Collections.unmodifiableCollection(devices);
    }

    private PhoneDevice read(PhoneDevice device, ResultSet rs) throws SQLException {
        device.setID(rs.getLong("deviceID"));
        device.setPhoneUserID(rs.getLong("userID"));
        device.setDevice(rs.getString("device"));
        device.setExtension(rs.getString("extension"));
        device.setCallerId(rs.getString("callerId"));
        device.setServerID(rs.getLong("serverID"));
        device.setPrimary(rs.getLong("isPrimary") == 1);
        return device;
    }

    private PhoneUser read(PhoneUser user, ResultSet rs) throws SQLException {
        user.setID(rs.getLong("userID"));
        user.setUsername(rs.getString("username"));
        return user;
    }

    private PhoneServer readServer(ResultSet rs) throws SQLException {
        PhoneServer server = new PhoneServer();
        server.setID(rs.getLong("serverID"));
        server.setName(rs.getString("serverName"));
        server.setHostname(rs.getString("hostname"));
        server.setPort(rs.getInt("port"));
        server.setUsername(rs.getString("username"));
        server.setPassword(rs.getString("password"));
        return server;
    }
}
