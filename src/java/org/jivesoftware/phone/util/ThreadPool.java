/**
 * $RCSfile: ThreadPool.java,v $
 * $Revision: 1.9 $
 * $Date: 2005/06/23 16:29:57 $
 *
 * Copyright (C) 1999-2004 Jive Software. All rights reserved.
 *
 * This software is the proprietary information of Jive Software. Use is subject to license terms.
 */
package org.jivesoftware.phone.util;


import org.jivesoftware.phone.database.HibernateUtil;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

/**
 * @author Andrew Wright
 */
public final class ThreadPool {

    private static final Logger log = Logger.getLogger(ThreadPool.class.getName());

    private static ExecutorService executor = null;

    private ThreadPool() {
    }


    public static void init() {
        if(executor == null) {
            executor = createThreadPool();

        } else if (executor.isTerminated()) {
            executor = createThreadPool();
        }
    }

    public static ExecutorService getThreadPool() {
        return executor;
    }

    public static void shutdown() {
        log.fine("Attempting to shutdown Phone thread pool");
        executor.shutdownNow();
        executor = null;
    }

    private static ExecutorService createThreadPool() {
        return Executors.newFixedThreadPool(50);
    }

}
