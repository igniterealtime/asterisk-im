package org.jivesoftware.phone.database;

import org.jivesoftware.phone.PhoneDevice;
import org.jivesoftware.phone.PhoneUser;
import org.jivesoftware.phone.PhoneServer;

import java.util.List;
import java.util.Collection;

/**
 * @author Andrew Wright
 * @since 1.0
 */
public interface PhoneDAO {

    PhoneUser getPhoneUserByDevice(String device);

    PhoneUser getByUsername(String username);

    PhoneUser getPhoneUserByID(long phoneUserID);

    void remove(PhoneUser phoneUser);

    List<PhoneUser> getPhoneUsers();

    PhoneDevice getDevice(String deviceName);

    PhoneDevice getPhoneDeviceByID(long id);

    List<PhoneDevice> getPhoneDeviceByUserID(long userID);

    void insert(PhoneUser user);

    void insert(PhoneDevice device);

    /**
     * Inserts the phone server.
     *
     * @param server the phone server to be inserted.
     */
    void insert(PhoneServer server);

    void update(PhoneUser user);

    void update(PhoneDevice device);

    /**
     * Updates the phone server.
     *
     * @param server the phone server to be updated.
     */
    void update(PhoneServer server);

    void remove(PhoneDevice device);

    /**
     * Removes a phone server.
     *
     * @param server the phone server to be removed.
     */
    void remove(PhoneServer server);

    /**
     * Returns the primary device for a {@link PhoneUser}
     *
     * @param phoneUserID the id of the phone user
     * @return the primary device for the phone user
     */
    PhoneDevice getPrimaryDevice(long phoneUserID);

    List<PhoneDevice> getPhoneDevicesByUsername(String username);

    PhoneServer getPhoneServerByServerName(String serverName);

    PhoneServer getPhoneServerByID(long id);

    Collection<PhoneServer> getPhoneServers();

    Collection<PhoneDevice> getPhoneDevicesByServerName(String serverName);

    Collection<PhoneDevice> getPhoneDevicesByServerID(long id);
}
