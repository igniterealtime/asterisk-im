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
    private Boolean primary;
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
        this.primary = primary;
    }

    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

}
