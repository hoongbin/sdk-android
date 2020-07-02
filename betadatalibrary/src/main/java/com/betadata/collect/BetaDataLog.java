
package com.betadata.collect;

import android.util.Log;


public class BetaDataLog {
    private static BetaDataAPI mBetaDataAPI;

    private BetaDataLog() {

    }

    public static void init(BetaDataAPI BetaDataAPI) {
        mBetaDataAPI = BetaDataAPI;
    }

    public static void d(String tag, String msg) {
        if (mBetaDataAPI.isDebugMode()) {
            info(tag, msg,null);
        }
    }

    public static void d(String tag, String msg, Throwable tr) {
        if (mBetaDataAPI.isDebugMode()) {
            info(tag, msg, tr);
        }

    }

    public static void i(String tag, String msg) {
        if (BetaDataAPI.ENABLE_LOG) {
            info(tag, msg,null);
        }
    }

    public static void i(String tag, Throwable tr) {
        if (BetaDataAPI.ENABLE_LOG) {
            info(tag,"",tr);
        }
    }

    public static void i(String tag, String msg, Throwable tr) {
        if (BetaDataAPI.ENABLE_LOG) {
            info(tag,msg,tr);
        }
    }

    public static void e(String tag, String msg) {
        if (BetaDataAPI.ENABLE_LOG) {
            info_e(tag, msg,null);
        }
    }
    /**
     * 此方法谨慎修改
     * 插件配置 disableLog 会修改此方法
     * @param tag String
     * @param msg String
     * @param tr Throwable
     */
    public static void info_e(String tag, String msg, Throwable tr) {
        try {
            Log.e(tag, msg, tr);
        } catch (Exception e) {
            printStackTrace(e);
        }
    }

    /**
     * 此方法谨慎修改
     * 插件配置 disableLog 会修改此方法
     * @param tag String
     * @param msg String
     * @param tr Throwable
     */
    public static void info(String tag, String msg, Throwable tr) {
        try {
            Log.i(tag, msg, tr);
        } catch (Exception e) {
            printStackTrace(e);
        }
    }

    /**
     * 此方法谨慎修改
     * 插件配置 disableLog 会修改此方法
     * @param e Exception
     */
    public static void printStackTrace(Exception e) {
        if (e != null) {
            e.printStackTrace();
        }
    }
}
