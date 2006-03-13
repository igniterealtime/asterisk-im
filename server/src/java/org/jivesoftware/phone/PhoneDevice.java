/**
 * $RCSfile: PhoneDevice.java,v $
 * $Revision: 1.11 $
 * $Date: 2005/06/29 23:33:30 $
 *
 * Copyright (C) 1999-2004 Jive Software. All rights reserved.
 *
 * This software is the proprietary information of Jive Software. Use is subject to license terms.
 */
package org.jivesoftware.phone;

import org.jivesoftware.database.JiveID;

/**
 * @author Andrew Wright
 */
@JiveID(101)
public class PhoneDevice {

    private long id;
    private long phoneUserID;
    private String callerId;
    private String device;
    private boolean primary = false;
    private boolean monitored = false;

    private String extension;

    public PhoneDevice() {
    }

    public PhoneDevice(String device) {
        this.device = device;
    }

    public long getID() {
        return id;
    }

    public void setID(long id) {
        this.id = id;
    }

    public long getPhoneUserID() {
        return phoneUserID;
    }

    public void setPhoneUserID(long phoneUserID) {
        this.phoneUserID = phoneUserID;
    }

    public String getCallerId() {
        return callerId;
    }

    public void setCallerId(String callerId) {
        this.callerId = callerId;
    }

    public String getDevice() {
        return device;
    }

    public void setDevice(String device) {
        this.device = device;
    }

    public boolean isPrimary() {
        return primary;
    }

    public void setPrimary(boolean primary) {
        this.primary = primary;
    }

    /**
     * Returns true if this device should be monitored
     *
     * @return true if this device should be monitored
     */
    public boolean isMonitored() {
        return monitored;
    }

    /**
     * set whether or not this device should be monitored
     *
     * @param monitored true if this device should be monitored
     */
    public void setMonitored(boolean monitored) {
        this.monitored = monitored;
    }

    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final PhoneDevice that = (PhoneDevice) o;

        if (id != that.id) {
            return false;
        }
        if (monitored != that.monitored) {
            return false;
        }
        if (phoneUserID != that.phoneUserID) {
            return false;
        }
        if (primary != that.primary) {
            return false;
        }
        if (callerId != null ? !callerId.equals(that.callerId) : that.callerId != null) {
            return false;
        }
        if (device != null ? !device.equals(that.device) : that.device != null) {
            return false;
        }

        return !(extension != null ? !extension.equals(that.extension) : that.extension != null);

    }

    public int hashCode() {
        int result;
        result = (int) (id ^ (id >>> 32));
        result = 29 * result + (int) (phoneUserID ^ (phoneUserID >>> 32));
        result = 29 * result + (callerId != null ? callerId.hashCode() : 0);
        result = 29 * result + (device != null ? device.hashCode() : 0);
        result = 29 * result + (primary ? 1 : 0);
        result = 29 * result + (monitored ? 1 : 0);
        result = 29 * result + (extension != null ? extension.hashCode() : 0);
        return result;
    }


    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("PhoneDevice");
        sb.append("{id=").append(id);
        sb.append(", phoneUserID=").append(phoneUserID);
        sb.append(", callerId='").append(callerId).append('\'');
        sb.append(", device='").append(device).append('\'');
        sb.append(", primary=").append(primary);
        sb.append(", monitored=").append(monitored);
        sb.append(", extension='").append(extension).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
