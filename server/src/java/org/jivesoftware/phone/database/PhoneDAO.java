package org.jivesoftware.phone.database;

import org.jivesoftware.phone.PhoneUser;
import org.jivesoftware.phone.PhoneDevice;

import java.util.List;

/**
 * @author Andrew Wright
 */
public interface PhoneDAO {

    PhoneUser getByDevice(String device);

    PhoneUser getByUsername(String username);

    PhoneUser getByID(long phoneUserID);

    void remove(PhoneUser phoneUser);

    void save(PhoneUser phoneUser);

    List<PhoneUser> getALL();

    void close();

    PhoneDevice getDevice(String deviceName);
}
