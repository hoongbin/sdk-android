
 
package com.betadata.collect;

import java.util.LinkedList;

public class TrackTaskManager {
    /**
     * 请求线程队列
     */
    private final LinkedList<Runnable> mTrackEventTasks;
    private final LinkedList<Runnable> mEventDBTasks;
    private static TrackTaskManager trackTaskManager;

    public static synchronized TrackTaskManager getInstance() {
        try {
            if (null == trackTaskManager) {
                trackTaskManager = new TrackTaskManager();
            }
        } catch (Exception e) {
            BetaDataLog.printStackTrace(e);
        }
        return trackTaskManager;
    }

    private TrackTaskManager() {
        mTrackEventTasks = new LinkedList<>();
        mEventDBTasks = new LinkedList<>();
    }

    public void addTrackEventTask(Runnable trackEvenTask) {
        try {
            synchronized (mTrackEventTasks) {
                mTrackEventTasks.addLast(trackEvenTask);
            }
        } catch (Exception e) {
            BetaDataLog.printStackTrace(e);
        }
    }

    public Runnable getTrackEventTask() {
        try {
            synchronized (mTrackEventTasks) {
                if (mTrackEventTasks.size() > 0) {
                    return mTrackEventTasks.removeFirst();
                }
            }
        } catch (Exception e) {
            BetaDataLog.printStackTrace(e);
        }
        return null;
    }

    public void addEventDBTask(Runnable evenDBTask) {
        try {
            synchronized (mEventDBTasks) {
                mEventDBTasks.addLast(evenDBTask);
            }
        } catch (Exception e) {
            BetaDataLog.printStackTrace(e);
        }
    }

    public Runnable getEventDBTask() {
        try {
            synchronized (mEventDBTasks) {
                if (mEventDBTasks.size() > 0) {
                    return mEventDBTasks.removeFirst();
                }
            }
        } catch (Exception e) {
            BetaDataLog.printStackTrace(e);
        }
        return null;
    }

}
