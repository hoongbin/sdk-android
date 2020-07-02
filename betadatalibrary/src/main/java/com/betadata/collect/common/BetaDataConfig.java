package com.betadata.collect.common;

/**
 * Author: 李巷阳
 * Date: 2019-10-28
 * Version: V2.0.0
 * Part:
 * Description:
 */
public class BetaDataConfig {

    public static int mFlushIntervalDefault = 15 * 1000; /* Flush 默认时间间隔 */
    public static int mFlushBulkSizeDefault = 30;  /* 数据库存储数据量阈值 */
    public static int mFlushCacheSize = 3;// 默认 内存缓存的 数量
    public static int mSessionTime = 30 * 1000;// 若 App 在后台超过设定事件，则认为当前 Session 结束，发送 _AppEnd 事件
    public static long mMaxCacheSize = 32 * 1024 * 1024; //数据库缓存大小 默认 32MB

}
