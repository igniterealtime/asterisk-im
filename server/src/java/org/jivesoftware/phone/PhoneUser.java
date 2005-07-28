/**
 * $RCSfile: PhoneUser.java,v $
 * $Revision: 1.1 $
 * $Date: 2005/06/24 19:32:50 $
 *
 * Copyright (C) 1999-2004 Jive Software. All rights reserved.
 *
 * This software is the proprietary information of Jive Software. Use is subject to license terms.
 */
package org.jivesoftware.phone;

import org.jivesoftware.database.JiveID;

import java.util.HashSet;
import java.util.Set;

/**
 * Used to represent a user/channel relationship.
 *
 * @author Andrew Wright
 */
@JiveID(100)
public class PhoneUser implements java.io.Serializable {

    private Long id;
    private String username;
    private Set<PhoneDevice> devices;

    public PhoneUser() {
    }

    public PhoneUser(String username) {
        this.username = username;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }


    public PhoneDevice getPrimaryDevice() {

        for (PhoneDevice channel : devices) {
            if(channel.isPrimary()) {
                return channel;
            }
        }

        throw new IllegalStateException("No primary channel found!");
    }

    public Set<PhoneDevice> getDevices() {
        return devices;
    }

    public void setDevices(Set<PhoneDevice> devices) {
        this.devices = devices;
    }

    public void addDevice(PhoneDevice channel) {
        if(devices == null) {
            devices = new HashSet<PhoneDevice>();
        }
        devices.add(channel);
    }


}
