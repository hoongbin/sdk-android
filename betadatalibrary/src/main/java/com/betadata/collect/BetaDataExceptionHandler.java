

package com.betadata.collect;



import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.betadata.collect.common.AopConstants;
import com.betadata.collect.util.BetaDataTimer;

import org.json.JSONObject;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

public class BetaDataExceptionHandler implements Thread.UncaughtExceptionHandler {
    private static final int SLEEP_TIMEOUT_MS = 3000;

    private static BetaDataExceptionHandler sInstance;
    private Context mContext;
    private Class mIntentClz;
    private final Thread.UncaughtExceptionHandler mDefaultExceptionHandler;
    BetaDataExceptionHandler(Context context,Class clz) {
        this.mContext = context;
        this.mIntentClz = clz;
        mDefaultExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    public synchronized static void init(Context context,Class clz) {
        if (sInstance == null) {
            sInstance = new BetaDataExceptionHandler(context, clz);
        }
    }

    @Override
    public void uncaughtException(final Thread t, final Throwable e) {
        try {
            // Only one worker thread - giving priority to storing the event first and then flush
            BetaDataAPI.allInstances(new BetaDataAPI.InstanceProcessor() {
                @Override
                public void process(BetaDataAPI sensorsData) {
                    try {
                        final JSONObject messageProp = new JSONObject();
                        BetaDataTimer.getInstance().cancelTimerTask();
                        try {
                            Writer writer = new StringWriter();
                            PrintWriter printWriter = new PrintWriter(writer);
                            e.printStackTrace(printWriter);
                            Throwable cause = e.getCause();
                            while (cause != null) {
                                cause.printStackTrace(printWriter);
                                cause = cause.getCause();
                            }
                            printWriter.close();
                            String result = writer.toString();
                            messageProp.put(AopConstants.BETA_CRASH_REASON, result);
                        } catch (Exception ex) {
                            BetaDataLog.printStackTrace(ex);
                        }
                        sensorsData.track(AopConstants.BETA_APP_CRASH, messageProp);
                    } catch (Exception e) {
                        BetaDataLog.printStackTrace(e);
                    }
                }
            });

            BetaDataAPI.allInstances(new BetaDataAPI.InstanceProcessor() {
                @Override
                public void process(BetaDataAPI sensorsData) {
                    sensorsData.flush();
                }
            });

            if (mDefaultExceptionHandler != null) {
                Log.e("CrashReport","crash handler --->"+mDefaultExceptionHandler.getClass().getSimpleName());

                try {
                    Thread.sleep(SLEEP_TIMEOUT_MS);
                } catch (InterruptedException e1) {
                    BetaDataLog.printStackTrace(e1);
                }
                reStartApp();

            } else {
                killProcessAndExit();
            }
        } catch (Exception exception) {
            //ignored
        }
    }

    private void killProcessAndExit() {
        try {
            Thread.sleep(SLEEP_TIMEOUT_MS);
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(10);
        } catch (Exception e) {
            //ignored
        }
    }


    private void reStartApp(){
        Intent intent = new Intent(mContext, mIntentClz);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(intent);
        android.os.Process.killProcess(android.os.Process.myPid());
    }
}
