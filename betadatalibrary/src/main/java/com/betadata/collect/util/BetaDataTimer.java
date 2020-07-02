
 
package com.betadata.collect.util;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class BetaDataTimer {
    private static BetaDataTimer instance;
    private ScheduledExecutorService mScheduledExecutorService;
    public static BetaDataTimer getInstance() {
        if (instance == null) {
            instance = new BetaDataTimer();
        }
        return instance;
    }

    private BetaDataTimer() {
        mScheduledExecutorService = Executors.newScheduledThreadPool(1);
    }

    /**
     * start a timer task
     * @param runnable Runnable
     * @param initialDelay long
     * @param timePeriod long
     */
    public void timer(final Runnable runnable, long initialDelay, long timePeriod) {
        if (mScheduledExecutorService == null || mScheduledExecutorService.isShutdown()) {
            mScheduledExecutorService = Executors.newScheduledThreadPool(1);
        }

        mScheduledExecutorService.scheduleAtFixedRate(runnable, initialDelay, timePeriod, TimeUnit.MILLISECONDS);

    }

    /**
     * cancel timer task
     */
    public void cancelTimerTask() {
        if (mScheduledExecutorService != null) {
            mScheduledExecutorService.shutdown();
        }
    }
}
