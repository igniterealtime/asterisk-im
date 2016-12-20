/**
 * $RCSfile:  $
 * $Revision:  $
 * $Date:  $
 *
 * Copyright (C) 2006 Jive Software. All rights reserved.
 * This software is the proprietary information of Jive Software. Use is subject to license terms.
 */
package org.jivesoftware.phone.queue;

import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Bean for information pertaining to phone queues.
 */
public class PhoneQueue {
    private String name;
    private List<String> devices = new ArrayList<String>();
    private long serverID;

    public PhoneQueue(String name) {
        this.name = name;
    }

    public PhoneQueue(long serverID, String name) {
        this.serverID = serverID;
        this.name = name;
    }

    public long getServerID() {
        return serverID;
    }

    public void setServerID(long serverID) {
        this.serverID = serverID;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Collection<String> getDevices() {
        return Collections.unmodifiableCollection(devices);
    }

    public void addDevice(String device) {
        devices.add(device);
    }

    public void addDevices(Collection<String> devices) {
        devices.addAll(devices);
    }

    public void removeDevice(String device) {
        devices.remove(device);
    }

    public void removeDevices(Collection<String> devices) {
        devices.removeAll(devices);
    }
}
