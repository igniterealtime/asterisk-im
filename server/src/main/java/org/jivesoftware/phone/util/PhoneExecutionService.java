/**
 * $RCSfile:  $
 * $Revision:  $
 * $Date:  $
 *
 * Copyright (C) 2006 Jive Software. All rights reserved.
 * This software is the proprietary information of Jive Software. Use is subject to license terms.
 */
package org.jivesoftware.phone.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 *
 */
public class PhoneExecutionService {
    private static final ExecutorService service = Executors.newCachedThreadPool();

    public static ExecutorService getService() {
        return service;
    }

    private PhoneExecutionService() { }
}
