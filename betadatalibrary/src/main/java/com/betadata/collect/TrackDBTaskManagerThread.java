
 
package com.betadata.collect;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class TrackDBTaskManagerThread implements Runnable {
    private TrackTaskManager mTrackTaskManager;
    /**
     * 创建一个可重用固定线程数的线程池
     */
    private ExecutorService mPool;
    /**
     * 创建一个可重用固定线程数的线程池
     */
    private static final int POOL_SIZE = 1;
    /**
     * 轮询时间，单位：毫秒
     */
    private static final int SLEEP_TIME = 300;
    /**
     * 是否停止
     */
    private boolean isStop = false;

    public TrackDBTaskManagerThread() {
        try {
            this.mTrackTaskManager = TrackTaskManager.getInstance();
            mPool = Executors.newFixedThreadPool(POOL_SIZE);
        } catch (Exception e) {
            BetaDataLog.printStackTrace(e);
        }
    }

    @Override
    public void run() {
        try {
            while (!isStop) {
                Runnable downloadTask = mTrackTaskManager.getEventDBTask();
                if (downloadTask != null) {
                    mPool.execute(downloadTask);
                } else {//如果当前任务队列中没有下载任务downloadTask
                    try {
                        // 查询任务完成失败的,重新加载任务队列
                        // 轮询
                        Thread.sleep(SLEEP_TIME);
                    } catch (InterruptedException e) {
                        BetaDataLog.printStackTrace(e);
                    }
                }
            }

            if (isStop) {
                Runnable downloadTask = mTrackTaskManager.getEventDBTask();
                while (downloadTask != null) {
                    mPool.execute(downloadTask);
                    downloadTask = mTrackTaskManager.getEventDBTask();
                }
                mPool.shutdown();
                BetaDataThreadPool.getInstance().shutdown();
            }
        } catch (Exception e) {
            BetaDataLog.printStackTrace(e);
        }
    }

    public void setStop(boolean isStop) {
        this.isStop = isStop;
    }

}
