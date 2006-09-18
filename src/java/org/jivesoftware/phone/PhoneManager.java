package org.jivesoftware.phone;

import org.xmpp.packet.JID;

import java.util.List;
import java.util.Collection;
import java.util.Map;

/**
 * Used for acquiring Phone information and performing phone actions like dialing.
 *
 * @author Andrew Wright
 */
public interface PhoneManager {

    /**
     * Returns the JID that associated with a specific device
     *
     * @param device pbx device to find a jid for
     * @return the JID that associated with a specific device
     */
    PhoneUser getPhoneUserByDevice(String device);


    /**
     * Returns the pbx device that is associated with a particular JID
     *
     * @param username jid to find a device for
     * @return the pbx device that is associated with a particular JID
     */
    PhoneUser getPhoneUserByUsername(String username);

    /**
     * Returns a list of all the PhoneUsers in the system
     *
     * @return list of all the users
     */
    List<PhoneUser> getAllPhoneUsers();

    /**
     * Removes a specific phoneUser from the system
     *
     * @param phoneUser phone user to remove
     */
    void remove(PhoneUser phoneUser);

    /**
     * Removes the specified phoneDevice from the system
     *
     * @param phoneDevice phone device to remove
     */
    void remove(PhoneDevice phoneDevice);

    /**
     * Updates a phoneDevice in the system.
     *
     * @param phoneDevice phone device to update
     */
    void update(PhoneDevice phoneDevice);

    /**
     * Updates a phoneUser in the system.
     *
     * @param phoneUser phone user to update
     */
    void update(PhoneUser phoneUser);

    Collection<PhoneDevice> getAllPhoneDevices();

    /**
     * Finds a PhoneUser object that has a specific id. If there is no {@link PhoneUser} matching a phoneUserID
     * null will be returned.
     *
     * @param phoneUserID id of the device
     * @return user who matches the id
     */
    PhoneUser getPhoneUserByID(long phoneUserID);

    /**
     * Returns a List of {@link PhoneDevice} objects that are associated with specified
     * phoneUserID. If there are no {@link PhoneDevice} matching the phoneUserID then an empty list
     * will be returned.
     *
     * @param phoneUserID phone user's id
     * @return phone devices that are associated to the give phone user
     */
    List<PhoneDevice> getPhoneDevicesByUserID(long phoneUserID);

    /**
     * Returns a List of {@link PhoneDevice} object that are associated with specified username.
     * If there are no {@link PhoneDevice} matching the username then an empty list
     * will be returned.
     *
     * @param username phone username
     * @return devices athat are associated to the give phone user
     */
    List<PhoneDevice> getPhoneDevicesByUsername(String username);

    /**
     * Returns a {@link PhoneDevice} with a matching phoneDeviceID. If there is no phone device matching the
     * id then null will be returned.
     *
     * @param phoneDeviceID The id of the phoneDevice
     * @return the {@link PhoneDevice} with the matching id
     */
    PhoneDevice getPhoneDeviceByID(long phoneDeviceID);

    /**
     * Returns the primary device for a {@link PhoneUser}.
     *
     * @param phoneUserID id of the PhoneUser to get the PhoneDevice for
     * @return The primary PhoneDevice for a PhoneUser
     */
    PhoneDevice getPrimaryDevice(long phoneUserID);

    /**
     * Dials an extension
     *
     * @param username  username of the user dialing
     * @param extension extension to dial, this could local to the pbx or an outbound number
     * @throws PhoneException thrown if dialing cannot be completed
     */
    void originate(String username, String extension) throws PhoneException;


    /**
     * Dials someone by a jid
     *
     * @param username username of the person dialing
     * @param target   the target jid to dial
     * @throws PhoneException thrown if dialing cannot be completed
     */
    void originate(String username, JID target) throws PhoneException;

    /**
     * Forwards a call to a different extension
     *
     * @param callSessionID the call session id to forward
     * @param username
     * @param extension     extension to forward too
     * @throws PhoneException thrown if the forward cannot be completed
     */
    void forward(String callSessionID, String username, String extension) throws PhoneException;

    /**
     * Returns a map of all devices the system knows about.
     *
     * @return
     * @throws PhoneException
     */
    Map<Long,Collection<String>> getConfiguredDevices() throws PhoneException;

    /**
     * Returns a collection of all devices on a given server.
     *
     * @param serverID the serverID to grab the list of devices for.
     * @return
     * @throws PhoneException
     */
    Collection<String> getConfiguredDevicesByServerID(long serverID) throws PhoneException;

    /**
     * Acquire a phone device by its name
     *
     * @param device name of the phone device
     * @return the phone device object
     */
    Collection<PhoneDevice> getDevices(String device);

    /**
     * Used to see how many messages are in a mailbox
     *
     * @param serverID the serverID of the server that this mailbox resides on
     * @param mailbox the mailbox to check
     * @return mailbox status object
     * @throws PhoneException thrown if there are problems with the asterisk manager
     */
    MailboxStatus mailboxStatus(long serverID, String mailbox) throws PhoneException;

    /**
     * Forward a call an existing call to another device that has been registered to a
     * specific JID.
     *
     * @param callSessionID The session id of the current call.
     * @param username      Username of the current caller
     * @param target        JID of the target to transfer the call to
     * @throws PhoneException Thrown if there are any problems with the astersisk manager
     */
    void forward(String callSessionID, String username, JID target) throws PhoneException;

    /**
     * Inserts a new {@link PhoneUser} into the system.
     * Once this phone user is added a valid id will be set into the object.
     *
     * @param phoneUser The new phone user to add
     */
    void insert(PhoneUser phoneUser);

    /**
     * Inserts a new {@link PhoneDevice} into the system
     * Once this device is added a valid id will be set into the object.
     *
     * @param phoneDevice the phone device to add
     */
    void insert(PhoneDevice phoneDevice);

    void destroy();

    /**
     * Returns the user of the device if active session exist, null otherwise
     */
    public PhoneUser getActivePhoneUserByDevice(String device);

    /**
     * Returns a list of all PhoneServers currently being managed.
     *
     * @return a list of all PhoneServers currently being managed.
     */
    Collection<PhoneServer> getPhoneServers();

    /**
     * Creates a new phone server.
     *
     * @param name the name used to identify the server to the user.
     * @param serverAddress the address on which the server can be connected to.
     * @param port the port to connect to the server on
     * @param username the username to use to connect to the server.
     * @param password the password to use to connect to the server.
     * @return the created phone server.
     */
    PhoneServer createPhoneServer(String name, String serverAddress, int port, String username,
                                  String password);

    PhoneServer getPhoneServerByID(long serverID);

    PhoneServer updatePhoneServer(long serverID, String serverName, String serverAddress,
                                  int serverPort, String username, String password);

    PhoneServerStatus getPhoneServerStatus(long serverID);

    /**
     * Removes a phone server and all of its associated devices.
     *
     * @param serverID the serverID of the server to be removed.
     */
    void removePhoneServer(long serverID);

    Collection<PhoneDevice> getPhoneDevicesByServerID(long serverID);

    PhoneServer getPhoneServerByDevice(String device);

    enum PhoneServerStatus {
        connected,
        error,
        disconnected
    }
}