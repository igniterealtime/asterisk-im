/**
 * $RCSfile: BasePhoneEventListener.java,v $
 * $Revision: 1.4 $
 * $Date: 2005/06/29 21:35:13 $
 *
 * Copyright (C) 1999-2004 Jive Software. All rights reserved.
 *
 * This software is the proprietary information of Jive Software. Use is subject to license terms.
 */
package org.jivesoftware.phone.client;

/**
 * Convenient base class to use for creating PhoneEventListeners
 *
 * @author Andrew Wright
 */
public class BasePhoneEventListener implements PhoneEventListener {


    public final void handle(PhoneEvent event) {

        if(event instanceof RingEvent) {
            handleRing((RingEvent) event);
        }
        else if (event instanceof OnPhoneEvent) {
            handleOnPhone((OnPhoneEvent) event);
        }
        else if (event instanceof HangUpEvent) {
            handleHangUp((HangUpEvent) event);
        }
        else if (event instanceof DialedEvent) {
            handleDialed((DialedEvent) event);
        }

    }

    public void handleOnPhone(OnPhoneEvent event) {

    }

    public void handleHangUp(HangUpEvent event) {

    }

    public void handleRing(RingEvent event) {

    }

    public void handleDialed(DialedEvent event) {

    }
}
