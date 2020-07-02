
 
package com.betadata.collect;

import android.os.SystemClock;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

class EventTimer {
    private final TimeUnit timeUnit;
    private long startTime;
    private long endTime;
    private long eventAccumulatedDuration;
    private boolean isPaused = false;

    EventTimer(TimeUnit timeUnit, long startTime) {
        this.startTime = startTime;
        this.timeUnit = timeUnit;
        this.eventAccumulatedDuration = 0;
        this.endTime = -1;
    }
    public EventTimer(TimeUnit timeUnit, long startTime, long endTime) {
        this.timeUnit = timeUnit;
        this.startTime = startTime;
        this.endTime = endTime;
        this.eventAccumulatedDuration = 0;
    }

    String duration() {
        if (isPaused) {
            endTime = startTime;
        } else {
            endTime = endTime < 0 ? SystemClock.elapsedRealtime() : endTime;
        }
        long duration = endTime - startTime + eventAccumulatedDuration;
        try {
            if (duration < 0 || duration > 24 * 60 * 60 * 1000) {
                return String.valueOf(0);
            }
            float durationFloat;
            if (timeUnit == TimeUnit.MILLISECONDS) {
                durationFloat = duration;
            } else if (timeUnit == TimeUnit.SECONDS) {
                durationFloat = duration / 1000.0f;
            } else if (timeUnit == TimeUnit.MINUTES) {
                durationFloat = duration / 1000.0f / 60.0f;
            } else if (timeUnit == TimeUnit.HOURS) {
                durationFloat = duration / 1000.0f / 60.0f / 60.0f;
            } else {
                durationFloat = duration;
            }
            return durationFloat < 0 ? String.valueOf(0) : String.format(Locale.CHINA, "%.3f", durationFloat);
        } catch (Exception e) {
            return String.valueOf(0);
        }
    }

    long getStartTime() {
        return startTime;
    }

    void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    long getEndTime() {
        return endTime;
    }

    long getEventAccumulatedDuration() {
        return eventAccumulatedDuration;
    }

    void setEventAccumulatedDuration(long eventAccumulatedDuration) {
        this.eventAccumulatedDuration = eventAccumulatedDuration;
    }

    void setTimerState(boolean isPaused, long elapsedRealtime) {
        this.isPaused = isPaused;
        if (isPaused) {
            eventAccumulatedDuration = eventAccumulatedDuration + elapsedRealtime - startTime;
        }
        startTime = elapsedRealtime;
    }

    boolean isPaused() {
        return isPaused;
    }
}
