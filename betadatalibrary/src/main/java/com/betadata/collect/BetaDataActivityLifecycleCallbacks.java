
package com.betadata.collect;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.TextUtils;

import com.betadata.collect.common.AopConstants;
import com.betadata.collect.common.SpConstant;
import com.betadata.collect.data.DbAdapter;
import com.betadata.collect.data.DbParams;
import com.betadata.collect.data.persistent.PersistentFirstDay;
import com.betadata.collect.data.persistent.PersistentFirstStart;
import com.betadata.collect.util.AopUtil;
import com.betadata.collect.util.BetaDataTimer;
import com.betadata.collect.util.BetaDataUtils;
import com.betadata.collect.util.BetaSpUtils;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Author: 李巷阳
 * Date: 2019/7/25
 * Version: V2.0.0
 * Part:实现全周期监听Activity
 * Description:
 */
@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
class BetaDataActivityLifecycleCallbacks implements Application.ActivityLifecycleCallbacks {
    private static final String TAG = "BT.LifecycleCallbacks";
    private SimpleDateFormat mIsFirstDayDateFormat;
    private Context mContext;
    private boolean resumeFromBackground = false;// 默认第一次为false,如果用户设置为后台，发送appEnd后，再次进入，设置为true.
    private final BetaDataAPI mBetaDataInstance;
    private final PersistentFirstStart mFirstStart;
    private final PersistentFirstDay mFirstDay;
    private CountDownTimer mCountDownTimer;
    private DbAdapter mDbAdapter;
    private JSONObject activityProperty = new JSONObject();
    private JSONObject endDataProperty = new JSONObject();
    private boolean isAutoTrackEnabled;
    private static final String EVENT_TIMER = "event_timer";

    BetaDataActivityLifecycleCallbacks(BetaDataAPI instance, PersistentFirstStart firstStart,
                                       PersistentFirstDay firstDay, Context context) {
        this.mBetaDataInstance = instance;
        this.mFirstStart = firstStart;
        this.mFirstDay = firstDay;
        this.mContext = context;
        this.mDbAdapter = DbAdapter.getInstance();
        if (Looper.myLooper() == null) {
            new Thread( new Runnable() {
                @Override
                public void run() {
                    Looper.prepare();
                    initTimerAndObserver();
                    Looper.loop();
                }
            } ).start();
        } else {
            initTimerAndObserver();
        }
    }


    @Override
    public void onActivityCreated(Activity activity, Bundle bundle) {
    }

    /**
     * 处理append ,appinstall,appstart事件
     *
     * @param activity
     */
    @Override
    public void onActivityStarted(Activity activity) {
        try {
            // 获取是否开启自动采集
            isAutoTrackEnabled = mBetaDataInstance.isOpenAutoTrack();
//            // 如果
//            if (!isAutoTrackEnabled) {
//                // 存储用户安装第一天时间
//                checkFirstDay();
//                return;
//            }
            // 构建 Title 和 Screen 的名称
            activityProperty = AopUtil.buildTitleAndScreenName( activity );
            BetaDataUtils.mergeJSONObject( activityProperty, endDataProperty );
            // 判断当前时间减去app上次onPause的时间，是否大于我们设定的默认时间。
            boolean sessionTimeOut = isSessionTimeOut();
            // session超时并且数据库中没有存储end事件,则发送trackAppEnd
            if (sessionTimeOut && !mDbAdapter.getAppEndState()) {
                trackAppEnd();
            }
            // 判断session超时并且存储app_end_state发送成功
            if (sessionTimeOut || mDbAdapter.getAppEndState()) {
                // 设置appEndState默认状态
                mDbAdapter.commitAppEndState( false );
                // 存储第一天时间
                checkFirstDay();
                // 判断是否第一次启动，默认是true
                boolean firstStart = mFirstStart.get();
                try {
                    // 设置从设备开机到现在的时间，单位毫秒，含系统深度睡眠时间。
                    mBetaDataInstance.appBecomeActive();
                } catch (Exception e) {
                    BetaDataLog.printStackTrace( e );
                }
                //按home键，发送appEnd后，从后台恢复，则为true。重新开启线程管理器。
                if (resumeFromBackground) {
                    isAutoTrackEnabled = mBetaDataInstance.isOpenAutoTrack();
                    mBetaDataInstance.resumeTrackScreenOrientation();
                    mBetaDataInstance.resumeTrackTaskThread();
                }
                try {
                    // 判断没有app start有没有被忽略
                    if (!mBetaDataInstance.isAutoTrackEventTypeIgnored( BetaDataAPI.AutoTrackEventType.APP_START )) {

                        JSONObject properties = new JSONObject();
                        // 判断第一次安装
                        if (firstStart) {
                            long mCurrenTime= BetaDataUtils.getCurrenTimestampMillis(mContext);
                            BetaSpUtils.save(mContext, SpConstant.INSTALL_TIME,mCurrenTime);
                            // 发送安装事件
                            mBetaDataInstance.track( AopConstants.BETA_APP_INSTALL, properties );
                        }
                        // 发送开启事件
                        BetaDataUtils.mergeJSONObject( activityProperty, properties );
                        AopUtil.analyzeJson( AopConstants.BETA_TITLE,"",properties );
                        mBetaDataInstance.track( AopConstants.BETA_APP_START, properties );
                    }
                    // 没有忽略append
                    if (!mBetaDataInstance.isAutoTrackEventTypeIgnored( BetaDataAPI.AutoTrackEventType.APP_END )) {
                        // 存储AppStartTime；
                        // SystemClock.elapsedRealtime()为了避免用户更改本地时间。
                        // 从设备开机到现在的时间，单位毫秒。
                        long realtime = SystemClock.elapsedRealtime();
                        mDbAdapter.commitAppStartTime( realtime );
                        mBetaDataInstance.trackTimer( AopConstants.BETA_APP_END, TimeUnit.MILLISECONDS );
                    }
                    // 如果第一次启动，更改为false。
                    if (firstStart) {
                        mFirstStart.commit( false );
                    }
                } catch (Exception e) {
                    BetaDataLog.i( TAG, e );
                }
                resumeFromBackground = true;
            }
        } catch (Exception e) {
            BetaDataLog.printStackTrace( e );
        }
    }

    /**
     * 处理appscreen事件
     *
     * @param activity
     */
    @Override
    public void onActivityResumed(final Activity activity) {
        try {
            mDbAdapter.commitAppStart( true );
            // app screen是否被忽略
            if (isAutoTrackEnabled && !mBetaDataInstance.isActivityAutoTrackAppViewScreenIgnored( activity.getClass() )
                    && !mBetaDataInstance.isAutoTrackEventTypeIgnored( BetaDataAPI.AutoTrackEventType.APP_VIEW_SCREEN )) {
                try {
                    // 获取页面名称
                    String mScreen_name = activity.getClass().getSimpleName();
                    // 获取activity的预置属性
                    String activityPropertyStr = activityProperty.toString();
                    // 判断如果不是此activity的预置属性，则需要重新获取。
                    if (!activityPropertyStr.contains( mScreen_name )) {
                        // 构建 Title 和 Screen 的名称
                        activityProperty = AopUtil.buildTitleAndScreenName( activity );
                    }
                    JSONObject properties = new JSONObject();
                    BetaDataUtils.mergeJSONObject( activityProperty, properties );

                    // 添加  _screen_name  表示 Activity 的包名.类名
                    JSONObject trackProperties = new JSONObject();

                    trackProperties.put( AopConstants.BETA_SCREEN_NAME, mScreen_name );
                    BetaDataUtils.mergeJSONObject( trackProperties, properties );

                    mBetaDataInstance.track( AopConstants.BETA_APP_PAGEVIEW, properties );

                } catch (Exception e) {
                    BetaDataLog.i( TAG, e );
                }
            }
            /**
             * 1.isAutoTrackEnabled：是否开启自动追踪
             * 2.判断AppEnd事件不被忽略
             * 处理app崩溃 添加append时间
             */
            if (isAutoTrackEnabled && !mBetaDataInstance.isAutoTrackEventTypeIgnored( BetaDataAPI.AutoTrackEventType.APP_END )) {
                BetaDataTimer.getInstance().timer(new Runnable() {
                    @Override
                    public void run() {
                        commitAppEndData();
                        // 每隔15秒,把内存数据添加到数据库中。
                        mBetaDataInstance.flushDataSync();
                    }
                }, 1000, 15000 );
            }
        } catch (Exception e) {
            BetaDataLog.printStackTrace( e );
        }
    }

    @Override
    public void onActivityPaused(Activity activity) {
        if (!isAutoTrackEnabled) {
            return;
        }
        try {
            // 开启是否发送append倒计时
            mCountDownTimer.start();
            mDbAdapter.commitAppStart( false );
            // 关闭线程池中的计时器
            BetaDataTimer.getInstance().cancelTimerTask();
            // 更新append data
            commitAppEndData();
        } catch (Exception e) {
            BetaDataLog.printStackTrace( e );
        }
    }

    @Override
    public void onActivityStopped(Activity activity) {
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
    }

    /**
     * 发送append事件
     */
    private void trackAppEnd() {
        // 判断数据库中是否有ppEnd事件
        if (mDbAdapter.getAppEndState()) {
            return;
        }
        try {
            // 停止采集屏幕方向
            mBetaDataInstance.stopTrackScreenOrientation();
            // 计算事件活跃时长,添加至eventTimer
            mBetaDataInstance.appEnterBackground();
        } catch (Exception e) {
            BetaDataLog.printStackTrace( e );
        }

        if (isAutoTrackEnabled) {
            try {
                // 如果AppEnd没有被忽略
                if (!mBetaDataInstance.isAutoTrackEventTypeIgnored( BetaDataAPI.AutoTrackEventType.APP_END )) {
                    String jsonEndData = mDbAdapter.getAppEndData();
                    JSONObject endDataJsonObject = null;
                    if (!TextUtils.isEmpty( jsonEndData )) {
                        endDataJsonObject = new JSONObject( jsonEndData );
                        if (endDataJsonObject.has( EVENT_TIMER )) {
                            long startTime = mDbAdapter.getAppStartTime();
                            long endTime = endDataJsonObject.getLong( EVENT_TIMER );
                            EventTimer eventTimer = new EventTimer( TimeUnit.MILLISECONDS, startTime, endTime );
                            // 存储AppEnd的时间
                            mBetaDataInstance.trackTimer( AopConstants.BETA_APP_END, eventTimer );
                            // 清除onPause存储的时间
                            endDataJsonObject.remove( EVENT_TIMER );
                        }
                    }
                    JSONObject properties = new JSONObject();
                    if (endDataJsonObject != null) {
                        properties = new JSONObject( endDataJsonObject.toString() );
                    }
                    // 清除页面的url
                    mBetaDataInstance.clearLastScreenUrl();

                    // 发送AppEnd
                    mBetaDataInstance.track( AopConstants.BETA_APP_END, properties );
                }
            } catch (Exception e) {
                BetaDataLog.i( TAG, e );
            }
        }
        try {
            // 添加成功
            mDbAdapter.commitAppEndState( true );
        } catch (Exception e) {
            BetaDataLog.printStackTrace( e );
        }
    }

    /**
     * 存储append的session时间值
     */
    private void commitAppEndData() {
        try {
            AopUtil.analyzeJson( AopConstants.BETA_TITLE,"",endDataProperty );
            endDataProperty.put( EVENT_TIMER, SystemClock.elapsedRealtime() );
            mDbAdapter.commitAppEndData( endDataProperty.toString() );
            mDbAdapter.commitAppPausedTime( System.currentTimeMillis() );
        } catch (Exception e) {
            BetaDataLog.printStackTrace( e );
        }
    }

    /**
     * 根据设定的默认sesssion时间,判断是否发送append事件.
     *
     * @return true 超时，false 未超时
     */
    private boolean isSessionTimeOut() {
        long currentTime = System.currentTimeMillis() > 946656000000L ? System.currentTimeMillis() : 946656000000L;
        boolean sessionTimeOut = Math.abs( currentTime - mDbAdapter.getAppPausedTime() ) > mDbAdapter.getSessionIntervalTime();
        BetaDataLog.d( TAG, "SessionTimeOut:" + sessionTimeOut );
        return sessionTimeOut;
    }

    private void initTimerAndObserver() {
        // 初始化计时器
        initCountDownTimer();
        // 监听数据库变化
        registerObserver();
    }

    /**
     * 当用户App Puase时，我们开启倒计时。SessionTime是在DbAdapter里面配置,默认30秒。
     * 如果onResume时，就关闭倒计时。
     */
    private void initCountDownTimer() {
        mCountDownTimer = new CountDownTimer( mDbAdapter.getSessionIntervalTime(), 10 * 1000 ) {
            @Override
            public void onTick(long l) {
                BetaDataLog.d( TAG, "time:" + l );
            }

            @Override
            public void onFinish() {
                BetaDataLog.d( TAG, "timeFinish" );
                trackAppEnd();
                resumeFromBackground = true;
                mBetaDataInstance.stopTrackTaskThread();
            }
        };
    }

    /**
     * 检查 DateFormat 是否为空，如果为空则进行初始化
     */
    private void checkFirstDay() {
        try {
            if (mFirstDay.get() == null) {
                if (mIsFirstDayDateFormat == null) {
                    mIsFirstDayDateFormat = new SimpleDateFormat( "yyyy-MM-dd", Locale.getDefault() );
                }

                mFirstDay.commit( mIsFirstDayDateFormat.format( System.currentTimeMillis() ) );
            }
        } catch (Exception ex) {
            BetaDataLog.printStackTrace( ex );
        }
    }

    // 注册数据库内容监听
    private void registerObserver() {
        final BetaActivityStateObserver activityStateObserver = new BetaActivityStateObserver( new Handler( Looper.myLooper() ) );
        mContext.getContentResolver().registerContentObserver( DbParams.getInstance().getAppStartUri(), false, activityStateObserver );
        mContext.getContentResolver().registerContentObserver( DbParams.getInstance().getSessionTimeUri(), false, activityStateObserver );
        mContext.getContentResolver().registerContentObserver( DbParams.getInstance().getAppEndStateUri(), false, activityStateObserver );
    }

    private class BetaActivityStateObserver extends ContentObserver {
        /**
         * Creates a content observer.
         *
         * @param handler The handler to run {@link #onChange} on, or null if none.
         */
        BetaActivityStateObserver(Handler handler) {
            super( handler );
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange( selfChange, uri );
            // 触发事件,则会回调此方法
            try {
                // 如果是appstart 取消计时器
                if (DbParams.getInstance().getAppStartUri().equals( uri )) {
                    if (mCountDownTimer != null) {
                        mCountDownTimer.cancel();
                    }
                } else if (DbParams.getInstance().getAppEndStateUri().equals( uri )) {
                    mBetaDataInstance.flush( 3000 );
                }
                BetaDataLog.e( "BObserver", uri.toString() );
            } catch (Exception e) {
                BetaDataLog.printStackTrace( e );
            }
        }
    }

}
