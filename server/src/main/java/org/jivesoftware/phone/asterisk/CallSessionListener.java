/**
 * $RCSfile:  $
 * $Revision:  $
 * $Date:  $
 *
 * Copyright (C) 2006 Jive Software. All rights reserved.
 * This software is the proprietary information of Jive Software. Use is subject to license terms.
 */
package org.jivesoftware.phone.asterisk;

import org.jivesoftware.phone.asterisk.CallSession;

/**
 *
 */
public interface CallSessionListener {
    void callSessionCreated(CallSession session);

    void callSessionDestroyed(CallSession session);

    void callSessionModified(CallSession session, CallSession.Status oldStatus);
}
