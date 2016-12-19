/**
 * $RCSfile: Call.java,v $
 * $Revision: 1.2 $
 * $Date: 2005/06/25 00:05:40 $
 *
 * Copyright (C) 1999-2004 Jive Software. All rights reserved.
 *
 * This software is the proprietary information of Jive Software. Use is subject to license terms.
 */
package org.jivesoftware.phone.client;

import org.jivesoftware.phone.client.event.PhoneEventDispatcher;

/**
 * @author Andrew Wright
 */
public class Call {

    private final String id;
    private boolean active;

    Call(final String id, final PhoneEventDispatcher dispatcher) {
        this.id = id;
        this.active = true;

        dispatcher.addListener(new BasePhoneEventListener() {
            public void handleHangUp(HangUpEvent event) {

                if(id.equals(event.getCallID())) {
                    active = false;
                    dispatcher.removeListener(this);
                }
            }
        });

    }

    public String getId() {
        return id;
    }

    public boolean isActive() {
        return active;
    }


}
