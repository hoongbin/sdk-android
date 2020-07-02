
package com.betadata.collect;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BetaDataThreadPool {
    private static BetaDataThreadPool singleton;
    private static ExecutorService executorService;
    private static final int POOL_SIZE = 3;
    public synchronized static BetaDataThreadPool getInstance() {
        if (singleton == null || executorService == null ||
                executorService.isShutdown() || executorService.isTerminated()) {
            singleton = new BetaDataThreadPool();
            executorService = Executors.newFixedThreadPool(POOL_SIZE);
        }
        return singleton;
    }

    public void execute(Runnable runnable) {
        try {
            initThreadPool();
            if (runnable != null) {
                executorService.execute(runnable);
            }
        } catch (Exception e) {
            BetaDataLog.printStackTrace(e);
        }
    }

    public void shutdown() {
        if (executorService != null) {
            executorService.shutdown();
        }
    }

    private void initThreadPool() {
        if (executorService.isShutdown() || executorService.isTerminated()) {
            executorService = Executors.newFixedThreadPool(POOL_SIZE);
        }
    }
}