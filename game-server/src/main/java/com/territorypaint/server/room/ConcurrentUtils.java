package com.territorypaint.server.room;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Shared scheduler utility for room timers and background tasks.
 */
public class ConcurrentUtils {

    private static final ScheduledExecutorService SCHEDULER = Executors.newScheduledThreadPool(4, r -> {
        Thread t = new Thread(r, "ServerScheduler");
        t.setDaemon(true);
        return t;
    });

    public static ScheduledExecutorService getScheduler() {
        return SCHEDULER;
    }
}
