package org.jivesoftware.phone;

import org.xmpp.packet.JID;

import java.util.List;

/**
 * Used for acquiring Phone information
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
    PhoneUser getByDevice(String device);

 
    /**
     * Returns the pbx device that is associated with a particular JID
     *
     * @param username jid to find a device for
     * @return the pbx device that is associated with a particular JID
     */
    PhoneUser getByUsername(String username);

    /**
     * Returns a list of all the PhoneUsers in the system
     *
     * @return list of all the users
     */
    List<PhoneUser> getAll();

    /**
     * Removes a specific phoneUser from the system
     *
     * @param phoneUser phone user to remove
     */
    void remove(PhoneUser phoneUser);

    /**
     * Persists changes to a phone user
     *
     * @param phoneUser user to persist changes for
     */
    void save(PhoneUser phoneUser);

    /**
     * Finds a PhoneUser object that has a specific id
     *
     * @param phoneUserID id of the device
     * @return user who matches the id
     */
    PhoneUser getByID(long phoneUserID);

    /**
     * Dials an extension
     *
     * @param username username of the user dialing
     * @param extension extension to dial, this could local to the pbx or an outbound number
     * @throws PhoneException thrown if dialing cannot be completed
     */
    void dial(String username, String extension) throws PhoneException;


    /**
     * Dials someone by a jid
     *
     * @param username username of the person dialing
     * @param target the target jid to dial
     * @throws PhoneException thrown if dialing cannot be completed
     */
    void dial(String username, JID target) throws PhoneException;

    /**
     * Forwards a call to a different extension
     *
     * @param callSessionID the call session id to forward
     * @param extension extension to forward too
     * @throws PhoneException thrown if the forward cannot be completed
     */
    void forward(String callSessionID, String extension) throws PhoneException;

    /**
     * Used to release resources this manager might be holding on too
     */
    void close();

    /**
     * Returns a list of all devices the system nows about.
     *
     * @return
     * @throws PhoneException
     */
    List<String> getDevices() throws PhoneException;

    void invite(String callSessionID, String extension) throws PhoneException;

    /**
     * Acquire a phone device by its name
     *
     * @param device name of the phone device
     * @return the phone device object
     */
    PhoneDevice getDevice(String device);

    String monitor(String channel) throws PhoneException;

    void stopMonitor(String channel) throws PhoneException;
}
