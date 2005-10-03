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

    private Long id;
    private String callerId;
    private String device;
    private Boolean primary = false;
    private Boolean monitored = false;

    private String extension;

    public PhoneDevice() {
    }

    public PhoneDevice(String device) {
        this.device = device;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Boolean isPrimary() {
        return primary;
    }

    public void setPrimary(Boolean primary) {
        // We don't really want null values
        if(primary != null) {
            this.primary = primary;
        }
    }

    /**
     * Returns true if this device should be monitored
     *
     * @return true if this device should be monitored
     */
    public Boolean isMonitored() {
        return monitored;
    }

    /**
     * set whether or not this device should be monitored
     *
     * @param monitored true if this device should be monitored
     */
    public void setMonitored(Boolean monitored) {
        // We don't really want null values
        if(monitored != null) {
            this.monitored = monitored;
        }
    }

    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final PhoneDevice that = (PhoneDevice) o;

        if (callerId != null ? !callerId.equals(that.callerId) : that.callerId != null) return false;
        if (device != null ? !device.equals(that.device) : that.device != null) return false;
        if (extension != null ? !extension.equals(that.extension) : that.extension != null) return false;
        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (monitored != null ? !monitored.equals(that.monitored) : that.monitored != null) return false;
        if (primary != null ? !primary.equals(that.primary) : that.primary != null) return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = (id != null ? id.hashCode() : 0);
        result = 29 * result + (callerId != null ? callerId.hashCode() : 0);
        result = 29 * result + (device != null ? device.hashCode() : 0);
        result = 29 * result + (primary != null ? primary.hashCode() : 0);
        result = 29 * result + (monitored != null ? monitored.hashCode() : 0);
        result = 29 * result + (extension != null ? extension.hashCode() : 0);
        return result;
    }


    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("PhoneDevice");
        sb.append("{id=").append(id);
        sb.append(", callerId='").append(callerId).append('\'');
        sb.append(", device='").append(device).append('\'');
        sb.append(", primary=").append(primary);
        sb.append(", monitored=").append(monitored);
        sb.append(", extension='").append(extension).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
