
package com.betadata.collect;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.webkit.WebView;

import com.betadata.collect.common.AopConstants;
import com.betadata.collect.common.BetaDataConfig;
import com.betadata.collect.common.BetaDataConstant;
import com.betadata.collect.data.DbAdapter;
import com.betadata.collect.data.PersistentLoader;
import com.betadata.collect.data.persistent.PersistentDistinctId;
import com.betadata.collect.data.persistent.PersistentFirstDay;
import com.betadata.collect.data.persistent.PersistentFirstStart;
import com.betadata.collect.data.persistent.PersistentFirstTrackInstallation;
import com.betadata.collect.data.persistent.PersistentFirstTrackInstallationWithCallback;
import com.betadata.collect.data.persistent.PersistentLoginId;
import com.betadata.collect.data.persistent.PersistentSuperProperties;
import com.betadata.collect.exceptions.InvalidDataException;
import com.betadata.collect.util.BetaDataUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;


/**
 * Author: 李巷阳
 * Date: 2019/8/3
 * Version: V2.1.0
 * Part: 初始化配置信息，拼装tract信息
 * Description:
 */
public class BetaDataAPI implements IBetaDataAPI {

    static final int VTRACK_SUPPORTED_MIN_API = 16; // 可视化埋点功能最低API版本
    private int mFlushNetworkPolicy = BetaDataUtils.NetworkType.TYPE_3G | BetaDataUtils.NetworkType.TYPE_4G | BetaDataUtils.NetworkType.TYPE_WIFI;
    static String mAndroidId = null;//AndroidID
    static boolean mIsMainProcess = false;// 当前进程是否是主进程
    static Boolean ENABLE_LOG = false;
    private static final Map<Context, BetaDataAPI> sInstanceMap = new HashMap<>();
    private static BetaDataGPSLocation mGPSLocation;
    private String mServerUrl;    /* BetaAnalytics 地址 */
    private DebugMode mDebugMode = DebugMode.DEBUG_OFF; /* Debug模式选项 */

    private boolean mIsOpenAutoTrack;    /* SDK 自动采集事件 */
    private String mLastScreenUrl;   /* 上个页面的Url*/
    private JSONObject mLastScreenTrackProperties;
    private boolean mTrackFragmentAppViewScreen;   /* _AppViewScreen 事件是否支持 Fragment*/
    private boolean mEnableReactNativeAutoTrack;
    private boolean mClearReferrerWhenAppEnd = false;
    private boolean mFlushInBackground = true;   /*进入后台是否上传数据*/
    private final Context mContext;
    private final AnalyticsMessages mMessages;
    private final PersistentDistinctId mDistinctId;
    private final PersistentLoginId mPersistentLoginId;
    private final PersistentSuperProperties mSuperProperties;
    private final PersistentFirstStart mFirstStart;
    private final PersistentFirstDay mFirstDay;
    private final PersistentFirstTrackInstallation mFirstTrackInstallation;
    private final PersistentFirstTrackInstallationWithCallback mFirstTrackInstallationWithCallback;
    private final Map<String, Object> mBaseEventInfo;
    private final Map<String, EventTimer> mTrackTimer;
    private List<Integer> mAutoTrackIgnoredActivities;// 指定哪些 activity 不被AutoTrack
    private Set<Integer> mAutoTrackFragments;//  判断 AutoTrack 时，某个 Activity 的 AppViewScreen 是否被采集
    private final String mMainProcessName;// 主进程名称
    private String mCookie;
    private TrackTaskManager mTrackTaskManager; // 事件跟踪的 线程队列
    private TrackTaskManagerThread mTrackTaskManagerThread;// 事件跟踪的  线程队列管理器
    private TrackDBTaskManagerThread mTrackDBTaskManagerThread; // 事件跟踪的数据库  线程队列管理器
    private BetaDataThreadPool betaDataThreadPool;// beta 数据线程管理器
    private BetaDataScreenOrientationDetector mOrientationDetector;
    private BetaDataDynamicSuperProperties mDynamicSuperProperties;// 注册事件动态公共属性
    private SimpleDateFormat mIsFirstDayDateFormat;
    private BetaDataTrackEventCallBack mTrackEventCallBack;// 设置 track 事件回调

    private int mFlushInterval; /* Flush时间间隔 */
    private int mFlushBulkSize;  /* Flush数据量阈值 */



    /**
     * Debug 模式有三种：
     * DEBUG_OFF - 关闭DEBUG模式
     * DEBUG_ONLY - 打开DEBUG模式，但该模式下发送的数据仅用于调试，不进行数据导入后端
     * DEBUG_AND_TRACK - 打开DEBUG模式，并将数据导入到BetaDataAnalytics后端
     */
    public enum DebugMode {


        DEBUG_OFF( false),// 关闭debug
        DEBUG_OPEN( true);// 打开debug

        private final boolean debugMode;

        DebugMode(boolean debugMode) {
            this.debugMode = debugMode;
        }

        boolean isDebugMode() {
            return debugMode;
        }


//        DEBUG_OFF( false),// 关闭debug
//        DEBUG_ONLY( true),// 打开debug
//        DEBUG_AND_TRACK( true );// 打开debug
//        private final boolean debugWriteData;

//        DebugMode(boolean debugMode, boolean debugWriteData) {
//            this.debugMode = debugMode;
//            this.debugWriteData = debugWriteData;
//        }
//
//        boolean isDebugMode() {
//            return debugMode;
//        }
//
//        boolean isDebugWriteData() {
//            return debugWriteData;
//        }
    }

    /**
     * 获取BetaDataAPI单例
     *
     * @return
     */
    public static BetaDataAPI sharedInstance() {
        synchronized (sInstanceMap) {
            if (sInstanceMap.size() > 0) {
                Iterator<BetaDataAPI> iterator = sInstanceMap.values().iterator();
                if (iterator.hasNext()) {
                    return iterator.next();
                }
            }
            return new BetaDataAPIEmptyImplementation();
        }
    }

    /**
     * 获取BetaDataAPI单例
     *
     * @param context App的Context
     * @return BetaDataAPI单例
     */
    public static BetaDataAPI sharedInstance(Context context) {


        if (null == context) {
            return new BetaDataAPIEmptyImplementation();
        }

        synchronized (sInstanceMap) {
            final Context appContext = context.getApplicationContext();
            BetaDataAPI instance = sInstanceMap.get( appContext );

            if (null == instance) {
                BetaDataLog.i( BetaDataConstant.BT_TAG, "The static method sharedInstance(context, serverURL, debugMode) should be called before calling sharedInstance()" );
                return new BetaDataAPIEmptyImplementation();
            }
            return instance;
        }
    }

    /**
     * 初始化并获取BetaDataAPI单例
     *
     * @param context   App 的 Context
     * @param serverURL 用于收集事件的服务地址
     * @return BetaDataAPI单例
     */
    public static BetaDataAPI sharedInstance(Context context, DebugMode debugMode, String serverURL, String appid, String token) {
        return initBetaData( context, serverURL, debugMode, appid, token );
    }


    /**
     * 初始化BetaDataAPI配置文件
     *
     * @param context
     * @param serverURL
     * @param debugMode
     * @param appid
     * @param token
     * @return
     */
    private static BetaDataAPI initBetaData(Context context, String serverURL, DebugMode debugMode, String appid, String token) {
        if (null == context) {
            return new BetaDataAPIEmptyImplementation();
        }

        synchronized (sInstanceMap) {
            final Context appContext = context.getApplicationContext();
            BetaDataAPI instance = sInstanceMap.get( appContext );
            if (null == instance) {
                instance = new BetaDataAPI( appContext, serverURL, debugMode, appid, token );
                sInstanceMap.put( appContext, instance );
            }
            return instance;
        }
    }

    BetaDataAPI() {
        mContext = null;
        mMessages = null;
        mDistinctId = null;
        mPersistentLoginId = null;
        mSuperProperties = null;
        mFirstStart = null;
        mFirstDay = null;
        mFirstTrackInstallation = null;
        mFirstTrackInstallationWithCallback = null;
        mBaseEventInfo = null;
        mTrackTimer = null;
        mMainProcessName = null;
    }


    BetaDataAPI(Context context, String serverURL, DebugMode debugMode, String appid, String token) {
        mContext = context;
        mDebugMode = debugMode;
        mServerUrl = serverURL;

        // 获取包名
        final String packageName = context.getApplicationContext().getPackageName();
        // 初始化
        mAutoTrackIgnoredActivities = new ArrayList<>();
        // 初始化自动跟踪的事件集合
        mAutoTrackEventTypeList = new CopyOnWriteArraySet<>();
        try {
            // 清除本地的userAgent
            BetaDataUtils.cleanUserAgent( mContext );
        } catch (Exception e) {
            BetaDataLog.printStackTrace( e );
        }
        Bundle configBundle = null;
        try {
            BetaDataLog.init( this );
            final ApplicationInfo appInfo = context.getApplicationContext().getPackageManager()
                    .getApplicationInfo( packageName, PackageManager.GET_META_DATA );
            configBundle = appInfo.metaData;
        } catch (final PackageManager.NameNotFoundException e) {
            BetaDataLog.printStackTrace( e );
        }

        if (null == configBundle) {
            configBundle = new Bundle();
        }

        // 如果关闭debug,ENABLE_LOG就为false
        if (debugMode == DebugMode.DEBUG_OFF) {
            // 设置是否开启 log
            ENABLE_LOG = configBundle.getBoolean( "com.betadata.analytics.android.EnableLogging",
                    false );
        } else {
            // 设置是否开启 log
            ENABLE_LOG = configBundle.getBoolean( "com.betadata.analytics.android.EnableLogging",
                    true );
        }
        //打开debug模式，弹出提示
//        SHOW_DEBUG_INFO_VIEW = configBundle.getBoolean( "com.betadata.analytics.android.ShowDebugInfoView", true );
        // 设置两次数据发送的最小时间间隔
        mFlushInterval = configBundle.getInt( "com.betadata.analytics.android.FlushInterval", BetaDataConfig.mFlushIntervalDefault );
        // 设置本地缓存日志的最大条目数
        mFlushBulkSize = configBundle.getInt( "com.betadata.analytics.android.FlushBulkSize", BetaDataConfig.mFlushBulkSizeDefault );
        // 自动采集事件
        mIsOpenAutoTrack = configBundle.getBoolean( "com.betadata.analytics.android.AutoTrack", true );
        // 进入后台是否上传数据
        mFlushInBackground = configBundle.getBoolean( "com.betadata.analytics.android.FlushInBackground", true );
        // 获取主进程名称
        String mainProcessName = BetaDataUtils.getMainProcessName( context );
        if (TextUtils.isEmpty( mainProcessName )) {
            mMainProcessName = configBundle.getString( "com.betadata.analytics.android.MainProcessName" );
        } else {
            mMainProcessName = mainProcessName;
        }
        // 当前进程是否是主进程
        mIsMainProcess = BetaDataUtils.isMainProcess( context, mMainProcessName );
        // 内存缓存的 数量
        int flushCacheSize = configBundle.getInt( "com.betadata.analytics.android.FlushCacheSize", BetaDataConfig.mFlushCacheSize );
        // 初始化数据库,并且初始化内容提供者
        DbAdapter.getInstance( context, packageName );
        // 初始化管理与内部数据库和传感器数据服务器之间的事件通信
        mMessages = AnalyticsMessages.getInstance( mContext, flushCacheSize, appid, token );
        // 获取设备唯一标识
        mAndroidId = BetaDataUtils.getAndroidID( mContext );
        // 初始化数据存储层
        PersistentLoader.initLoader( context );
        // 操作SharedPreferences里面的对象,返回此对象在SharedPreferences的管理器。
        // 未登录时，设置用户的唯一ID 管理器
        mDistinctId = (PersistentDistinctId) PersistentLoader.loadPersistent( PersistentLoader.PersistentName.DISTINCT_ID );
        // 登录时，萌股ID  管理器
        mPersistentLoginId = (PersistentLoginId) PersistentLoader.loadPersistent( PersistentLoader.PersistentName.LOGIN_ID );
        // 事件公共属性  管理器
        mSuperProperties = (PersistentSuperProperties) PersistentLoader.loadPersistent( PersistentLoader.PersistentName.SUPER_PROPERTIES );
        // 是否第一次启动  管理器
        mFirstStart = (PersistentFirstStart) PersistentLoader.loadPersistent( PersistentLoader.PersistentName.FIRST_START );
        // 用户第一次安装  管理器
        mFirstTrackInstallation = (PersistentFirstTrackInstallation) PersistentLoader.loadPersistent( PersistentLoader.PersistentName.FIRST_INSTALL );
        // 用户第一次跟踪安装 管理器
        mFirstTrackInstallationWithCallback = (PersistentFirstTrackInstallationWithCallback) PersistentLoader.loadPersistent( PersistentLoader.PersistentName.FIRST_INSTALL_CALLBACK );
        // 第一次安装 管理器
        mFirstDay = (PersistentFirstDay) PersistentLoader.loadPersistent( PersistentLoader.PersistentName.FIRST_DAY );
        // 事件任务 集合 管理器
        mTrackTaskManager = TrackTaskManager.getInstance();
        // 事件线程池 消耗线程队列
        mTrackTaskManagerThread = new TrackTaskManagerThread();
        // 数据库线程池  消耗线程队列
        mTrackDBTaskManagerThread = new TrackDBTaskManagerThread();
        // 启动线程队列,开启消耗事件集合.
        betaDataThreadPool = BetaDataThreadPool.getInstance();
        betaDataThreadPool.execute( mTrackTaskManagerThread );
        betaDataThreadPool.execute( mTrackDBTaskManagerThread );
        // 初始化Application.ActivityLifecycleCallbacks 主要监听app安装，app的start以及app的end事件。
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            final Application app = (Application) context.getApplicationContext();
            // 初始化BetaDataActivityLifecycleCallbacks 监听切换前后台状态 回调
            final BetaDataActivityLifecycleCallbacks lifecycleCallbacks = new BetaDataActivityLifecycleCallbacks( this, mFirstStart, mFirstDay, context );
            app.registerActivityLifecycleCallbacks( lifecycleCallbacks );
        }
        // 初始化 预置基本属性
        mBaseEventInfo = initBaseEventInfo();
        // 初始化 计算页面时长集合
        mTrackTimer = new HashMap<>();


    }

    /**
     * 是否首次触发事件
     *
     * @return
     */
    public PersistentFirstStart getFirstStart() {
        return mFirstStart;
    }

    /**
     * AutoTrack 默认采集的事件类型
     */
    public enum AutoTrackEventType {
        APP_START( AopConstants.BETA_APP_START, 1 << 0 ),
        APP_END( AopConstants.BETA_APP_END, 1 << 1 ),
        APP_CLICK( AopConstants.BETA_APP_CLICK, 1 << 2 ),
        APP_VIEW_SCREEN( AopConstants.BETA_APP_PAGEVIEW, 1 << 3 );
        private final String eventName;
        private final int eventValue;

        public String getEventName() {
            return eventName;
        }

        public int getEventValue() {
            return eventValue;
        }

        public static AutoTrackEventType autoTrackEventTypeFromEventName(String eventName) {
            if (TextUtils.isEmpty( eventName )) {
                return null;
            }

            if (AopConstants.BETA_APP_START.equals( eventName )) {
                return APP_START;
            } else if (AopConstants.BETA_APP_END.equals( eventName )) {
                return APP_END;
            } else if (AopConstants.BETA_APP_CLICK.equals( eventName )) {
                return APP_CLICK;
            } else if (AopConstants.BETA_APP_PAGEVIEW.equals( eventName )) {
                return APP_VIEW_SCREEN;
            }

            return null;
        }

        AutoTrackEventType(String eventName, int eventValue) {
            this.eventName = eventName;
            this.eventValue = eventValue;
        }


    }


    /**
     * 事件拼装方法
     *
     * @param eventType
     * @param eventName
     * @param eventProperties    event_properties 事件
     * @param userProperties     user_properties 事件
     * @param originalDistinctId
     */
    private void trackEvent(final EventType eventType, final String eventName, final JSONObject eventProperties, final JSONObject userProperties, final String originalDistinctId) {


        final EventTimer eventTimer;
        // 根据事件名称，从时长集合中,获取对应事件时长。
        if (eventName != null) {
            synchronized (mTrackTimer) {
                eventTimer = mTrackTimer.get( eventName );
                mTrackTimer.remove( eventName );
            }
        } else {
            eventTimer = null;
        }

        try {
            JSONObject mBaseEventProperties = new JSONObject();
            try {
                if (eventType.isTrack()) {

                    // 判断需要获取预置基本信息
                    mBaseEventProperties = new JSONObject( mBaseEventInfo );
                    init_carrier( mBaseEventProperties );// 初始化运营商参数
                    // 如果某个事件的属性，在所有事件中都会出现,这个事件属性就是 mSuperProperties。
                    synchronized (mSuperProperties) {
                        JSONObject superProperties = mSuperProperties.get();
                        BetaDataUtils.mergeJSONObject( superProperties, mBaseEventProperties );
                    }
                    try {
                        //  设置动态公共属性
                        if (mDynamicSuperProperties != null) {
                            JSONObject dynamicSuperProperties = mDynamicSuperProperties.getDynamicSuperProperties();
                            if (dynamicSuperProperties != null) {
                                BetaDataUtils.mergeJSONObject( dynamicSuperProperties, mBaseEventProperties );
                            }
                        }
                    } catch (Exception e) {
                        BetaDataLog.printStackTrace( e );
                    }
                    // 当前网络状况
                    String networkType = BetaDataUtils.networkType( mContext );
                    mBaseEventProperties.put( AopConstants.BETA_WIFI, networkType.equals( "WIFI" ) );
                    mBaseEventProperties.put( AopConstants.BETA_NETWORK_TYPE, networkType );


                    if (mGPSLocation != null) {
                        double latitude = mGPSLocation.getLatitude();
                        double longitude = mGPSLocation.getLongitude();
                        mBaseEventProperties.put( AopConstants.BETA_COUNTRY, mGPSLocation.getCountry() );
                        mBaseEventProperties.put( AopConstants.BETA_PROVINCE, mGPSLocation.getProvince() );
                        mBaseEventProperties.put( AopConstants.BETA_CITY, mGPSLocation.getCity() );
                        mBaseEventProperties.put( AopConstants.BETA_HOUSING_ESTATE, mGPSLocation.getHousing_estate() );
                        //地图定位
                        Map<String, Double> aMapLocation = new HashMap<String, Double>();
                        aMapLocation.put( "lat", latitude );
                        aMapLocation.put( "lon", longitude );
                        JSONObject mapJson = new JSONObject( aMapLocation );
                        mBaseEventProperties.put( AopConstants.BETA_LBS, mapJson );
                    }

                }
                // 设置手机唯一标识
                setPhoneIdentification(eventProperties);

                BetaDataUtils.mergeJSONObject( eventProperties, mBaseEventProperties );


                // 添加event_duration统计时长
                if (null != eventTimer) {
                    try {
                        Double duration = Double.valueOf( eventTimer.duration() );
                        if (duration > 0) {
                            mBaseEventProperties.put( AopConstants.BETA_EVENT_DURATION, duration );
                            BetaDataLog.e("BETA_EVENT_DURATION",duration+"");
                        }
                    } catch (Exception e) {
                        BetaDataLog.printStackTrace( e );
                    }
                }

                // 获取用户当前的唯一标识


                String mLoginId = BetaDataAPI.sharedInstance().getLoginId();
                if (mLoginId != null && !"".equals( mLoginId ) && mLoginId.length() != 16) {
                    mBaseEventProperties.put( AopConstants.BETA_SECOND_ID, mLoginId );
                    if (userProperties != null) {
                        userProperties.put( AopConstants.BETA_SECOND_ID, mLoginId );
                    }
                }

                final JSONObject dataObj = new JSONObject();

                dataObj.put( AopConstants.BETA_EVENT, eventName );
                dataObj.put( AopConstants.BETA_TIME, BetaDataUtils.getCurrenTimestampMillis(mContext) );


                boolean isEnterDb = isEnterDb( eventName, mBaseEventProperties );
                if (!isEnterDb) {
                    BetaDataLog.d( BetaDataConstant.BT_TAG, eventName + " event can not enter database" );
                    return;
                }
                // 如果首次安装 或者 用户首次开启(用户退出登录后,mFirstStart会为true),则添加_is_fist。
                if (AopConstants.BETA_APP_INSTALL.equals( eventName ) || (mFirstStart != null && mFirstStart.get())) {
                    // 不能等于点击事件
                    if (!AopConstants.BETA_APP_CLICK.equals( eventName )) {
                        mBaseEventProperties.put( AopConstants.BETA_IS_FIRST, true );
                        mFirstStart.commit( false );
                    }

                }


                if (eventType.isProfile()) {
                    dataObj.put( AopConstants.BETA_USERS_PROPERTIES, userProperties );
                    dataObj.put( AopConstants.BETA_EVENTS_PROPERTIES, mBaseEventProperties );
                } else {
                    dataObj.put( AopConstants.BETA_EVENTS_PROPERTIES, mBaseEventProperties );
                }

                BetaDataLog.e( BetaDataConstant.BT_TAG_SEND_MESSAGE, "1.追踪事件成功:" + dataObj.toString() );
                mMessages.enqueueEventMessage( eventName, dataObj );

            } catch (JSONException e) {
                throw new InvalidDataException( "Unexpected property" );
            }
        } catch (Exception e) {
            BetaDataLog.printStackTrace( e );
        }
    }

    /**
     * 设置手机唯一标识
     * imei 和 oaid  和 android_id
     * @param eventProperties
     * @throws JSONException
     */
    private void setPhoneIdentification(JSONObject eventProperties) throws JSONException {
        String imei= BetaPhoneInfoManager.getInstance().getImei();
        String oaid= BetaPhoneInfoManager.getInstance().getOaid();
        String android_id= BetaDataUtils.getAndroidID(mContext );

        eventProperties.put( AopConstants.BETA_IMEI,imei);
        eventProperties.put( AopConstants.BETA_OAID, oaid );
        eventProperties.put( AopConstants.BETA_ANDROID_ID,android_id);
        // - imei
        //- oaid
        //- android
        // imei > oaid > android_id
        // 检查imei不等空,则位置imei
        // 检查oaid不等空,则位置oaid
        // 否则赋值Android_id
        if(imei==null||"".equals(imei))
        {
            if(oaid==null||"".equals(oaid)){
                eventProperties.put( AopConstants.BETA_DEVICE_ID,android_id );
            }else{
                eventProperties.put( AopConstants.BETA_DEVICE_ID,oaid );
            }
        }else{
            eventProperties.put( AopConstants.BETA_DEVICE_ID,imei );
        }
    }


    /**
     * 根据传递过来的网络属性，判断是否进行数据冲洗
     *
     * @param networkType
     * @return
     */
    protected boolean isShouldFlush(String networkType) {
        return (BetaDataUtils.toNetworkType( networkType ) & mFlushNetworkPolicy) != 0;
    }

    /**
     * 获取并配置 App 的一些基本属性
     */
    private Map<String, Object> initBaseEventInfo() {
        final Map<String, Object> deviceInfo = new HashMap<>();
        deviceInfo.put( AopConstants.BETA_OS, "Android" );
        deviceInfo.put( AopConstants.BETA_SDK, "Android" );
        deviceInfo.put(AopConstants.BETA_OS_VERSION, BetaDataUtils.getPhoneOsVersion());
        deviceInfo.put(AopConstants.BETA_MODEL, BetaDataUtils.getPhoneModel());
        deviceInfo.put( AopConstants.BETA_SDK_VERSION, AopConstants.BETADATA_SDK_VERSION );
        deviceInfo.put( AopConstants.BETA_APP_VERSION, BetaDataUtils.getVersionName( mContext ) );
        deviceInfo.put( AopConstants.BETA_CHANNEL, BetaDataUtils.getAppChannelName( mContext, "UMENG_CHANNEL" ) );
        deviceInfo.put( AopConstants.BETA_CARRIER, BetaDataUtils.getOperator( mContext ) );// 运营商名称
        deviceInfo.put( AopConstants.BETA_MANUFACTURER, BetaDataUtils.getManufacturer() );
        deviceInfo.put( AopConstants.BETA_SCREEN_WIDTH, BetaDataUtils.deviceWidth( mContext ) );
        deviceInfo.put( AopConstants.BETA_SCREEN_HEIGHT, BetaDataUtils.deviceHeight( mContext ) );
        return Collections.unmodifiableMap( deviceInfo );
    }


    /**
     * 返回预置属性
     *
     * @return JSONObject 预置属性
     */
    @Override
    public JSONObject getPresetProperties() {
        JSONObject properties = new JSONObject();
        try {
            if (mBaseEventInfo != null) {
                properties = new JSONObject( mBaseEventInfo );
            }

        } catch (Exception e) {
            BetaDataLog.printStackTrace( e );
        }
        return properties;
    }

    /**
     * 设置当前 serverUrl
     *
     * @param serverUrl 当前 serverUrl
     */
    @Override
    public void setServerUrl(String serverUrl) {


    }

    /**
     * 设置是否开启 log
     *
     * @param enable boolean
     */
    @Override
    public void enableLog(boolean enable) {
        this.ENABLE_LOG = enable;
    }

    @Override
    public long getMaxCacheSize() {
        return BetaDataConfig.mMaxCacheSize;
    }

    /**
     * 设置本地缓存上限值，单位 byte，默认为 32MB：32 * 1024 * 1024
     *
     * @param maxCacheSize 单位 byte
     */
    @Override
    public void setMaxCacheSize(long maxCacheSize) {
        if (maxCacheSize > 0) {
            //防止设置的值太小导致事件丢失
            BetaDataConfig.mMaxCacheSize = Math.max( 16 * 1024 * 1024, maxCacheSize );
        }
    }

    /**
     * 设置 flush 时网络发送策略，默认 3G、4G、WI-FI 环境下都会尝试 flush
     *
     * @param networkType int 网络类型
     */
    @Override
    public void setFlushNetworkPolicy(int networkType) {
        mFlushNetworkPolicy = networkType;
    }

    /**
     * 两次数据发送的最小时间间隔，单位毫秒
     * <p>
     * 默认值为15 * 1000毫秒
     * 在每次调用track、signUp以及profileSet等接口的时候，都会检查如下条件，以判断是否向服务器上传数据:
     * <p>
     * 1. 是否是WIFI/3G/4G网络条件
     * 2. 是否满足发送条件之一:
     * 1) 与上次发送的时间间隔是否大于 flushInterval
     * 2) 本地缓存日志数目是否大于 flushBulkSize
     * <p>
     * 如果满足这两个条件，则向服务器发送一次数据；如果不满足，则把数据加入到队列中，等待下次检查时把整个队列的内
     * 容一并发送。需要注意的是，为了避免占用过多存储，队列最多只缓存20MB数据。
     *
     * @return 返回时间间隔，单位毫秒
     */
    @Override
    public int getFlushInterval() {
        return mFlushInterval;
    }

    /**
     * 设置两次数据发送的最小时间间隔
     *
     * @param flushInterval 时间间隔，单位毫秒
     */
    @Override
    public void setFlushInterval(int flushInterval) {
        mFlushInterval = Math.max( 5 * 1000, flushInterval );
    }

    /**
     * 返回本地缓存日志的最大条目数
     * <p>
     * 默认值为100条
     * 在每次调用track、signUp以及profileSet等接口的时候，都会检查如下条件，以判断是否向服务器上传数据:
     * <p>
     * 1. 是否是WIFI/3G/4G网络条件
     * 2. 是否满足发送条件之一:
     * 1) 与上次发送的时间间隔是否大于 flushInterval
     * 2) 本地缓存日志数目是否大于 flushBulkSize
     * <p>
     * 如果满足这两个条件，则向服务器发送一次数据；如果不满足，则把数据加入到队列中，等待下次检查时把整个队列的内
     * 容一并发送。需要注意的是，为了避免占用过多存储，队列最多只缓存32MB数据。
     *
     * @return 返回本地缓存日志的最大条目数
     */
    @Override
    public int getFlushBulkSize() {
        return mFlushBulkSize;
    }

    /**
     * 设置本地缓存日志的最大条目数
     *
     * @param flushBulkSize 缓存数目
     */
    @Override
    public void setFlushBulkSize(int flushBulkSize) {
        mFlushBulkSize = Math.max( 50, flushBulkSize );
    }

    /**
     * 设置 App 切换到后台与下次事件的事件间隔
     * <p>
     * 默认值为 30*1000 毫秒
     * <p>
     * 若 App 在后台超过设定事件，则认为当前 Session 结束，发送 _AppEnd 事件
     */
    @Override
    public void setSessionIntervalTime(int sessionIntervalTime) {
        if (DbAdapter.getInstance() == null) {
            BetaDataLog.i( BetaDataConstant.BT_TAG, "The static method sharedInstance(context, serverURL, debugMode) should be called before calling sharedInstance()" );
            return;
        }

        if (sessionIntervalTime < 10 * 1000 || sessionIntervalTime > 5 * 60 * 1000) {
            BetaDataLog.i( BetaDataConstant.BT_TAG, "SessionIntervalTime:" + sessionIntervalTime + " is invalid, session interval time is between 10s and 300s." );
            return;
        }

        DbAdapter.getInstance().commitSessionIntervalTime( sessionIntervalTime );
    }

    /**
     * 设置 App 切换到后台与下次事件的事件间隔
     * <p>
     * 默认值为 30*1000 毫秒
     * <p>
     * 若 App 在后台超过设定事件，则认为当前 Session 结束，发送 _AppEnd 事件
     *
     * @return 返回设置的 SessionIntervalTime ，默认是 30 * 1000 毫秒
     */
    @Override
    public int getSessionIntervalTime() {
        if (DbAdapter.getInstance() == null) {
            BetaDataLog.i( BetaDataConstant.BT_TAG, "The static method sharedInstance(context, serverURL, debugMode) should be called before calling sharedInstance()" );
            return 30 * 1000;
        }

        return DbAdapter.getInstance().getSessionIntervalTime();
    }

    /**
     * 更新 GPS 位置信息
     *
     * @param latitude  纬度
     * @param longitude 经度
     */
    @Override
    public void setGPSLocation(double latitude, double longitude) {
        try {
            if (mGPSLocation == null) {
                mGPSLocation = new BetaDataGPSLocation();
            }

            mGPSLocation.setLatitude( (long) (latitude * Math.pow( 10, 6 )) );
            mGPSLocation.setLongitude( (long) (longitude * Math.pow( 10, 6 )) );
        } catch (Exception e) {
            BetaDataLog.printStackTrace( e );
        }
    }


    /**
     * 更新 GPS 位置信息及地理位置
     *
     * @param latitude       纬度
     * @param longitude      经度
     * @param country        国家
     * @param province       省份
     * @param city           城市
     * @param housing_estate 详细地址
     */
    public void setGPSLocation(double latitude, double longitude, String country, String province, String city, String housing_estate) {
        try {
            if (mGPSLocation == null) {
                mGPSLocation = new BetaDataGPSLocation();
            }

            mGPSLocation.setLatitude( latitude );
            mGPSLocation.setLongitude( longitude );

            mGPSLocation.setCountry( country );
            mGPSLocation.setProvince( province );
            mGPSLocation.setCity( city );
            mGPSLocation.setHousing_estate( housing_estate );

        } catch (Exception e) {
            BetaDataLog.printStackTrace( e );
        }
    }

    /**
     * 清楚 GPS 位置信息
     */
    @Override
    public void clearGPSLocation() {
        mGPSLocation = null;
    }

    @Override
    public void enableTrackScreenOrientation(boolean enable) {
        try {
            if (enable) {
                if (mOrientationDetector == null) {
                    mOrientationDetector = new BetaDataScreenOrientationDetector( mContext, SensorManager.SENSOR_DELAY_NORMAL );
                }
                mOrientationDetector.enable();
            } else {
                if (mOrientationDetector != null) {
                    mOrientationDetector.disable();
                    mOrientationDetector = null;
                }
            }
        } catch (Exception e) {
            BetaDataLog.printStackTrace( e );
        }
    }

    @Override
    public void resumeTrackScreenOrientation() {
        try {
            if (mOrientationDetector != null) {
                mOrientationDetector.enable();
            }
        } catch (Exception e) {
            BetaDataLog.printStackTrace( e );
        }
    }

    @Override
    public void stopTrackScreenOrientation() {
        try {
            if (mOrientationDetector != null) {
                mOrientationDetector.disable();
            }
        } catch (Exception e) {
            BetaDataLog.printStackTrace( e );
        }
    }

    @Override
    public String getScreenOrientation() {
        try {
            if (mOrientationDetector != null) {
                return mOrientationDetector.getOrientation();
            }
        } catch (Exception e) {
            BetaDataLog.printStackTrace( e );
        }
        return null;
    }

    @Override
    public void setCookie(String cookie, boolean encode) {
        try {
            if (encode) {
                this.mCookie = URLEncoder.encode( cookie, "UTF-8" );
            } else {
                this.mCookie = cookie;
            }
        } catch (Exception e) {
            BetaDataLog.printStackTrace( e );
        }
    }

    @Override
    public String getCookie(boolean decode) {
        try {
            if (decode) {
                return URLDecoder.decode( this.mCookie, "UTF-8" );
            } else {
                return this.mCookie;
            }
        } catch (Exception e) {
            BetaDataLog.printStackTrace( e );
            return null;
        }

    }

    /**
     * 打开 SDK 自动追踪
     * <p>
     * 该功能自动追踪 App 的一些行为，例如 SDK 初始化、App 启动（_AppStart） / 关闭（_AppEnd）、
     * 进入页面（$AppViewScreen）等等，具体信息请参考文档:
     * https://sensorsdata.cn/manual/android_sdk.html
     * <p>
     * 该功能仅在 API 14 及以上版本中生效，默认关闭
     */
    @Deprecated
    @Override
    public void enableAutoTrack() {
        List<AutoTrackEventType> eventTypeList = new ArrayList<>();
        eventTypeList.add( AutoTrackEventType.APP_START );
        eventTypeList.add( AutoTrackEventType.APP_END );
        eventTypeList.add( AutoTrackEventType.APP_VIEW_SCREEN );
        enableAutoTrack( eventTypeList );
    }

    /**
     * 打开 SDK 自动追踪
     * <p>
     * 该功能自动追踪 App 的一些行为，指定哪些 AutoTrack 事件被追踪，具体信息请参考文档:
     * https://sensorsdata.cn/manual/android_sdk.html
     * <p>
     * 该功能仅在 API 14 及以上版本中生效，默认关闭
     *
     * @param eventTypeList 开启 AutoTrack 的事件列表
     */
    @Override
    public void enableAutoTrack(List<AutoTrackEventType> eventTypeList) {
        try {
            mIsOpenAutoTrack = true;
            if (eventTypeList == null) {
                eventTypeList = new ArrayList<>();
            }

            mAutoTrackEventTypeList.addAll( eventTypeList );
        } catch (Exception e) {
            BetaDataLog.printStackTrace( e );
        }
    }

    /**
     * 关闭 AutoTrack 中的部分事件
     *
     * @param eventTypeList AutoTrackEventType 类型 List
     */
    @Override
    public void disableAutoTrack(List<AutoTrackEventType> eventTypeList) {
        if (eventTypeList == null || eventTypeList.size() == 0) {
            return;
        }

        if (mAutoTrackEventTypeList == null) {
            return;
        }
        try {
            mAutoTrackEventTypeList.removeAll( eventTypeList );
        } catch (Exception e) {
            BetaDataLog.printStackTrace( e );
        }

        if (mAutoTrackEventTypeList.size() == 0) {
            mIsOpenAutoTrack = false;
        }
    }

    /**
     * 关闭 AutoTrack 中的某个事件
     *
     * @param autoTrackEventType AutoTrackEventType 类型
     */
    @Override
    public void disableAutoTrack(AutoTrackEventType autoTrackEventType) {
        if (autoTrackEventType == null) {
            return;
        }

        if (mAutoTrackEventTypeList == null) {
            return;
        }

        try {
            if (mAutoTrackEventTypeList.contains( autoTrackEventType )) {
                mAutoTrackEventTypeList.remove( autoTrackEventType );
            }
        } catch (Exception e) {
            BetaDataLog.printStackTrace( e );
        }

        if (mAutoTrackEventTypeList.size() == 0) {
            mIsOpenAutoTrack = false;
        }
    }

    /**
     * 自动收集 App Crash 日志，该功能默认是关闭的
     */
    @Override
    public void trackAppCrash(Class clz) {
        BetaDataExceptionHandler.init(mContext,clz);
    }

    /**
     * 是否开启自动采集，默认是开启
     *
     * @return
     */
    @Override
    public boolean isOpenAutoTrack() {
        return mIsOpenAutoTrack;
    }


    // Package-level access. Used (at least) by GCMReceiver
    // when OS-level events occur.
    /* package */ interface InstanceProcessor {
        public void process(BetaDataAPI m);
    }

    /* package */
    static void allInstances(InstanceProcessor processor) {
        synchronized (sInstanceMap) {
            for (final BetaDataAPI instance : sInstanceMap.values()) {
                processor.process( instance );
            }
        }
    }


    /**
     * 是否开启自动追踪 Fragment 的 $AppViewScreen 事件
     * 默认不开启
     */
    @Override
    public void trackFragmentAppViewScreen() {
        this.mTrackFragmentAppViewScreen = true;
    }

    @Override
    public boolean isTrackFragmentAppViewScreenEnabled() {
        return this.mTrackFragmentAppViewScreen;
    }

    /**
     * 开启 AutoTrack 支持 React Native
     */
    @Override
    public void enableReactNativeAutoTrack() {
        this.mEnableReactNativeAutoTrack = true;
    }

    @Override
    public boolean isReactNativeAutoTrackEnabled() {
        return this.mEnableReactNativeAutoTrack;
    }

    /**
     * 向WebView注入本地方法, 将distinctId传递给当前的WebView
     *
     * @param webView            当前WebView
     * @param isSupportJellyBean 是否支持API level 16及以下的版本。
     *                           因为API level 16及以下的版本, addJavascriptInterface有安全漏洞,请谨慎使用
     */
    @SuppressLint(value = {"SetJavaScriptEnabled", "addJavascriptInterface"})
    @Override
    public void showUpWebView(WebView webView, boolean isSupportJellyBean) {
        showUpWebView( webView, isSupportJellyBean, null );
    }

    @SuppressLint(value = {"SetJavaScriptEnabled", "addJavascriptInterface"})
    @Override
    public void showUpWebView(WebView webView, boolean isSupportJellyBean, boolean enableVerify) {
        showUpWebView( webView, null, isSupportJellyBean, enableVerify );
    }

    /**
     * 此方法谨慎修改
     * 插件配置 disableJsInterface 会修改此方法
     */
    @SuppressLint(value = {"SetJavaScriptEnabled", "addJavascriptInterface"})
    @Override
    public void showUpWebView(WebView webView, JSONObject properties, boolean isSupportJellyBean, boolean enableVerify) {
        if (Build.VERSION.SDK_INT < 17 && !isSupportJellyBean) {
            BetaDataLog.d( BetaDataConstant.BT_TAG, "For applications targeted to API level JELLY_BEAN or below, this feature NOT SUPPORTED" );
            return;
        }

        if (webView != null) {
            webView.getSettings().setJavaScriptEnabled( true );
            webView.addJavascriptInterface( new AppWebViewInterface( mContext, properties, enableVerify ), "BetaData_APP_JS_Bridge" );
        }
    }

    /**
     * 向WebView注入本地方法, 将distinctId传递给当前的WebView
     *
     * @param webView            当前WebView
     * @param isSupportJellyBean 是否支持API level 16及以下的版本。
     *                           因为API level 16及以下的版本, addJavascriptInterface有安全漏洞,请谨慎使用
     * @param properties         用户自定义属性
     */
    @SuppressLint(value = {"SetJavaScriptEnabled", "addJavascriptInterface"})
    @Override
    public void showUpWebView(WebView webView, boolean isSupportJellyBean, JSONObject properties) {
        showUpWebView( webView, properties, isSupportJellyBean, false );
    }

    /**
     * 此方法谨慎修改
     * 插件配置 disableJsInterface 会修改此方法
     */
    @Override
    public void showUpX5WebView(Object x5WebView, JSONObject properties, boolean isSupportJellyBean, boolean enableVerify) {
        try {
            if (Build.VERSION.SDK_INT < 17 && !isSupportJellyBean) {
                BetaDataLog.d( BetaDataConstant.BT_TAG, "For applications targeted to API level JELLY_BEAN or below, this feature NOT SUPPORTED" );
                return;
            }

            if (x5WebView == null) {
                return;
            }

            Class<?> clazz = x5WebView.getClass();
            Method addJavascriptInterface = clazz.getMethod( "addJavascriptInterface", Object.class, String.class );
            if (addJavascriptInterface == null) {
                return;
            }

            addJavascriptInterface.invoke( x5WebView, new AppWebViewInterface( mContext, properties, enableVerify ), "BetaData_APP_JS_Bridge" );
        } catch (Exception e) {
            BetaDataLog.printStackTrace( e );
        }
    }

    /**
     * 此方法谨慎修改
     * 插件配置 disableJsInterface 会修改此方法
     */
    @Override
    public void showUpX5WebView(Object x5WebView, boolean enableVerify) {
        try {
            if (x5WebView == null) {
                return;
            }

            Class<?> clazz = x5WebView.getClass();
            Method addJavascriptInterface = clazz.getMethod( "addJavascriptInterface", Object.class, String.class );
            if (addJavascriptInterface == null) {
                return;
            }

            addJavascriptInterface.invoke( x5WebView, new AppWebViewInterface( mContext, null, enableVerify ), "BetaData_APP_JS_Bridge" );
        } catch (Exception e) {
            BetaDataLog.printStackTrace( e );
        }
    }

    @Override
    public void showUpX5WebView(Object x5WebView) {
        showUpX5WebView( x5WebView, false );
    }

    /**
     * 指定哪些 activity 不被AutoTrack
     * <p>
     * 指定activity的格式为：activity.getClass().getCanonicalName()
     *
     * @param activitiesList activity列表
     */
    @Override
    public void ignoreAutoTrackActivities(List<Class<?>> activitiesList) {
        if (activitiesList == null || activitiesList.size() == 0) {
            return;
        }

        if (mAutoTrackIgnoredActivities == null) {
            mAutoTrackIgnoredActivities = new ArrayList<>();
        }

        int hashCode;
        for (Class<?> activity : activitiesList) {
            if (activity != null) {
                hashCode = activity.hashCode();
                if (!mAutoTrackIgnoredActivities.contains( hashCode )) {
                    mAutoTrackIgnoredActivities.add( hashCode );
                }
            }
        }
    }

    /**
     * 恢复不被 AutoTrack 的 activity
     *
     * @param activitiesList List
     */
    @Override
    public void resumeAutoTrackActivities(List<Class<?>> activitiesList) {
        if (activitiesList == null || activitiesList.size() == 0) {
            return;
        }

        if (mAutoTrackIgnoredActivities == null) {
            mAutoTrackIgnoredActivities = new ArrayList<>();
        }

        try {
            int hashCode;
            for (Class activity : activitiesList) {
                if (activity != null) {
                    hashCode = activity.hashCode();
                    if (mAutoTrackIgnoredActivities.contains( hashCode )) {
                        mAutoTrackIgnoredActivities.remove( Integer.valueOf( hashCode ) );
                    }
                }
            }
        } catch (Exception e) {
            BetaDataLog.printStackTrace( e );
        }
    }

    /**
     * 指定某个 activity 不被 AutoTrack
     *
     * @param activity Activity
     */
    @Override
    public void ignoreAutoTrackActivity(Class<?> activity) {
        if (activity == null) {
            return;
        }

        if (mAutoTrackIgnoredActivities == null) {
            mAutoTrackIgnoredActivities = new ArrayList<>();
        }

        try {
            int hashCode = activity.hashCode();
            if (!mAutoTrackIgnoredActivities.contains( hashCode )) {
                mAutoTrackIgnoredActivities.add( hashCode );
            }
        } catch (Exception e) {
            BetaDataLog.printStackTrace( e );
        }
    }

    /**
     * 恢复不被 AutoTrack 的 activity
     *
     * @param activity Class
     */
    @Override
    public void resumeAutoTrackActivity(Class<?> activity) {
        if (activity == null) {
            return;
        }

        if (mAutoTrackIgnoredActivities == null) {
            mAutoTrackIgnoredActivities = new ArrayList<>();
        }

        try {
            int hashCode = activity.hashCode();
            if (mAutoTrackIgnoredActivities.contains( hashCode )) {
                mAutoTrackIgnoredActivities.remove( Integer.valueOf( hashCode ) );
            }
        } catch (Exception e) {
            BetaDataLog.printStackTrace( e );
        }
    }

    /**
     * 指定 fragment 被 AutoTrack 采集
     *
     * @param fragment Fragment
     */
    @Override
    public void enableAutoTrackFragment(Class<?> fragment) {
        if (fragment == null) {
            return;
        }

        if (mAutoTrackFragments == null) {
            mAutoTrackFragments = new CopyOnWriteArraySet<>();
        }

        try {
            mAutoTrackFragments.add( fragment.hashCode() );
            mAutoTrackFragments.add( fragment.getCanonicalName().hashCode() );
        } catch (Exception e) {
            BetaDataLog.printStackTrace( e );
        }
    }

    /**
     * 指定 fragments 被 AutoTrack 采集
     *
     * @param fragmentsList Fragment 集合
     */
    @Override
    public void enableAutoTrackFragments(List<Class<?>> fragmentsList) {
        if (fragmentsList == null || fragmentsList.size() == 0) {
            return;
        }

        if (mAutoTrackFragments == null) {
            mAutoTrackFragments = new CopyOnWriteArraySet<>();
        }

        try {
            for (Class fragment : fragmentsList) {
                mAutoTrackFragments.add( fragment.hashCode() );
                mAutoTrackFragments.add( fragment.getCanonicalName().hashCode() );
            }
        } catch (Exception e) {
            BetaDataLog.printStackTrace( e );
        }
    }

    /**
     * 指定 fragment 被 AutoTrack 采集
     *
     * @param fragmentName,Fragment 名称，使用 包名 + 类名，建议直接通过 Class.getCanonicalName 获取
     */
    @Override
    public void enableAutoTrackFragment(String fragmentName) {
        if (TextUtils.isEmpty( fragmentName )) {
            return;
        }

        if (mAutoTrackFragments == null) {
            mAutoTrackFragments = new CopyOnWriteArraySet<>();
        }

        try {
            mAutoTrackFragments.add( fragmentName.hashCode() );
        } catch (Exception e) {
            BetaDataLog.printStackTrace( e );
        }
    }

    /**
     * 判断 AutoTrack 时，某个 Activity 的 $AppViewScreen 是否被过滤
     * 如果过滤的话，会过滤掉 Activity 的 $AppViewScreen 事件
     *
     * @param activity Activity
     * @return Activity 是否被过滤
     */
    @Override
    public boolean isActivityAutoTrackAppViewScreenIgnored(Class<?> activity) {
        if (activity == null) {
            return false;
        }
        if (mAutoTrackIgnoredActivities != null &&
                mAutoTrackIgnoredActivities.contains( activity.hashCode() )) {
            return true;
        }

        if (activity.getAnnotation( BetaDataIgnoreTrackAppViewScreenAndAppClick.class ) != null) {
            return true;
        }

        if (activity.getAnnotation( BetaDataIgnoreTrackAppViewScreen.class ) != null) {
            return true;
        }

        return false;
    }

    /**
     * 判断 AutoTrack 时，某个 Activity 的 $AppViewScreen 是否被采集
     *
     * @param fragment Fragment
     * @return 某个 Activity 的 $AppViewScreen 是否被采集
     */
    @Override
    public boolean isFragmentAutoTrackAppViewScreen(Class<?> fragment) {
        if (fragment == null) {
            return false;
        }
        try {
            if (mAutoTrackFragments != null && mAutoTrackFragments.size() > 0) {
                if (mAutoTrackFragments.contains( fragment.hashCode() )
                        || mAutoTrackFragments.contains( fragment.getCanonicalName().hashCode() )) {
                    return true;
                } else {
                    return false;
                }
            }

            if (fragment.getClass().getAnnotation( BetaDataIgnoreTrackAppViewScreen.class ) != null) {
                return false;
            }
        } catch (Exception e) {
            BetaDataLog.printStackTrace( e );
        }

        return true;
    }

    /**
     * 判断 AutoTrack 时，某个 Activity 的 $AppClick 是否被过滤
     * 如果过滤的话，会过滤掉 Activity 的 $AppClick 事件
     *
     * @param activity Activity
     * @return Activity 是否被过滤
     */
    @Override
    public boolean isActivityAutoTrackAppClickIgnored(Class<?> activity) {
        if (activity == null) {
            return false;
        }
        if (mAutoTrackIgnoredActivities != null &&
                mAutoTrackIgnoredActivities.contains( activity.hashCode() )) {
            return true;
        }

        if (activity.getAnnotation( BetaDataIgnoreTrackAppViewScreenAndAppClick.class ) != null) {
            return true;
        }

        if (activity.getAnnotation( BetaDataIgnoreTrackAppClick.class ) != null) {
            return true;
        }

        return false;
    }

    private Set<AutoTrackEventType> mAutoTrackEventTypeList;

    /**
     * 过滤掉 AutoTrack 的某个事件类型
     *
     * @param autoTrackEventType AutoTrackEventType
     */
    @Deprecated
    @Override
    public void ignoreAutoTrackEventType(AutoTrackEventType autoTrackEventType) {
        if (autoTrackEventType == null) {
            return;
        }

        if (mAutoTrackEventTypeList.contains( autoTrackEventType )) {
            mAutoTrackEventTypeList.remove( autoTrackEventType );
        }
    }

    /**
     * 过滤掉 AutoTrack 的某些事件类型
     *
     * @param eventTypeList AutoTrackEventType List
     */
    @Deprecated
    @Override
    public void ignoreAutoTrackEventType(List<AutoTrackEventType> eventTypeList) {
        if (eventTypeList == null) {
            return;
        }

        for (AutoTrackEventType eventType : eventTypeList) {
            if (eventType != null && mAutoTrackEventTypeList.contains( eventType )) {
                mAutoTrackEventTypeList.remove( eventType );
            }
        }
    }

    /**
     * 判断 某个 AutoTrackEventType 是否被忽略
     *
     * @param eventType AutoTrackEventType
     * @return true 被忽略; false 没有被忽略
     */
    @Override
    public boolean isAutoTrackEventTypeIgnored(AutoTrackEventType eventType) {

        if (eventType != null && !mAutoTrackEventTypeList.contains( eventType )) {
            return true;
        }
        return false;
    }

    /**
     * 设置界面元素ID
     *
     * @param view   要设置的View
     * @param viewID String 给这个View的ID
     */
    @Override
    public void setViewID(View view, String viewID) {
        if (view != null && !TextUtils.isEmpty( viewID )) {
            view.setTag( R.id.beta_analytics_tag_view_id, viewID );
        }
    }

    /**
     * 设置界面元素ID
     *
     * @param view   要设置的View
     * @param viewID String 给这个View的ID
     */
    @Override
    public void setViewID(android.app.Dialog view, String viewID) {
        try {
            if (view != null && !TextUtils.isEmpty( viewID )) {
                if (view.getWindow() != null) {
                    view.getWindow().getDecorView().setTag( R.id.beta_analytics_tag_view_id, viewID );
                }
            }
        } catch (Exception e) {
            BetaDataLog.printStackTrace( e );
        }
    }

    /**
     * 设置界面元素ID
     *
     * @param alertDialog 要设置的View
     * @param viewID      String 给这个View的ID
     */
    @Override
    public void setViewID(Object alertDialog, String viewID) {
        try {
            if (alertDialog == null) {
                return;

            }

            Class<?> supportAlertDialogClass = null;
            Class<?> androidXAlertDialogClass = null;
            Class<?> currentAlertDialogClass = null;
            try {
                supportAlertDialogClass = Class.forName( "android.support.v7.app.AlertDialog" );
            } catch (Exception e) {
                //ignored
            }

            try {
                androidXAlertDialogClass = Class.forName( "androidx.appcompat.app.AlertDialog" );
            } catch (Exception e) {
                //ignored
            }

            if (supportAlertDialogClass != null) {
                currentAlertDialogClass = supportAlertDialogClass;
            } else {
                currentAlertDialogClass = androidXAlertDialogClass;
            }

            if (currentAlertDialogClass == null) {
                return;
            }

            if (!currentAlertDialogClass.isInstance( alertDialog )) {
                return;
            }

            if (!TextUtils.isEmpty( viewID )) {
                Method getWindowMethod = alertDialog.getClass().getMethod( "getWindow" );
                if (getWindowMethod == null) {
                    return;
                }

                Window window = (Window) getWindowMethod.invoke( alertDialog );
                if (window != null) {
                    window.getDecorView().setTag( R.id.beta_analytics_tag_view_id, viewID );
                }
            }
        } catch (Exception e) {
            BetaDataLog.printStackTrace( e );
        }
    }

    /**
     * 设置 View 所属 Activity
     *
     * @param view     要设置的View
     * @param activity Activity View 所属 Activity
     */
    @Override
    public void setViewActivity(View view, Activity activity) {
        try {
            if (view == null || activity == null) {
                return;
            }
            view.setTag( R.id.beta_analytics_tag_view_activity, activity );
        } catch (Exception e) {
            BetaDataLog.printStackTrace( e );
        }
    }

    /**
     * 设置 View 所属 Fragment 名称
     *
     * @param view         要设置的View
     * @param fragmentName String View 所属 Fragment 名称
     */
    @Override
    public void setViewFragmentName(View view, String fragmentName) {
        try {
            if (view == null || TextUtils.isEmpty( fragmentName )) {
                return;
            }
            view.setTag( R.id.beta_analytics_tag_view_fragment_name2, fragmentName );
        } catch (Exception e) {
            BetaDataLog.printStackTrace( e );
        }
    }

    /**
     * 忽略View
     *
     * @param view 要忽略的View
     */
    @Override
    public void ignoreView(View view) {
        if (view != null) {
            view.setTag( R.id.beta_analytics_tag_view_ignored, "1" );
        }
    }

    @Override
    public void ignoreView(View view, boolean ignore) {
        if (view != null) {
            view.setTag( R.id.beta_analytics_tag_view_ignored, ignore ? "1" : "0" );
        }
    }

    /**
     * 设置View属性
     *
     * @param view       要设置的View
     * @param properties 要设置的View的属性
     */
    @Override
    public void setViewProperties(View view, JSONObject properties) {
        if (view == null || properties == null) {
            return;
        }

        view.setTag( R.id.beta_analytics_tag_view_properties, properties );
    }

    private List<Class> mIgnoredViewTypeList = new ArrayList<>();

    @Override
    public List<Class> getIgnoredViewTypeList() {
        if (mIgnoredViewTypeList == null) {
            mIgnoredViewTypeList = new ArrayList<>();
        }

        return mIgnoredViewTypeList;
    }

    /**
     * 返回设置 AutoTrack 的 Fragments 集合，如果没有设置则返回 null.
     *
     * @return Set
     */
    @Override
    public Set<Integer> getAutoTrackFragments() {
        return mAutoTrackFragments;
    }

    /**
     * 忽略某一类型的 View
     *
     * @param viewType Class
     */
    @Override
    public void ignoreViewType(Class viewType) {
        if (viewType == null) {
            return;
        }

        if (mIgnoredViewTypeList == null) {
            mIgnoredViewTypeList = new ArrayList<>();
        }

        if (!mIgnoredViewTypeList.contains( viewType )) {
            mIgnoredViewTypeList.add( viewType );
        }
    }


    /**
     * 获取当前用户的distinctId
     * <p>
     * 若调用前未调用 {@link #identify(String)} 设置用户的 distinctId，SDK 会调用 {@link UUID} 随机生成
     * UUID，作为用户的 distinctId
     * <p>
     * 该方法已不推荐使用，请参考 {@link #getAnonymousId()}
     *
     * @return 当前用户的distinctId
     */
    @Deprecated
    @Override
    public String getDistinctId() {
        synchronized (mDistinctId) {
            return mDistinctId.get();
        }
    }

    /**
     * 获取当前用户的匿名id
     * <p>
     * 若调用前未调用 {@link #identify(String)} 设置用户的匿名id，SDK 会调用 {@link UUID} 随机生成
     * UUID，作为用户的匿名id
     *
     * @return 当前用户的匿名id
     */
    @Override
    public String getAnonymousId() {
        synchronized (mDistinctId) {
            return mDistinctId.get();
        }
    }

    /**
     * 重置默认匿名id
     */
    @Override
    public void resetAnonymousId() {
        synchronized (mDistinctId) {
            if (BetaDataUtils.isValidAndroidId( mAndroidId )) {
                mDistinctId.commit( mAndroidId );
                return;
            }
            mDistinctId.commit( UUID.randomUUID().toString() );
        }
    }

    /**
     * 获取当前用户的 loginId
     * <p>
     * 若调用前未调用 {@link #login(String)} 设置用户的 loginId，会返回null
     *
     * @return 当前用户的 loginId
     */
    @Override
    public String getLoginId() {
        synchronized (mPersistentLoginId) {
            return mPersistentLoginId.get();
        }
    }

    /**
     * 获取当前用户的 ID
     *
     * @return 优先返回登录 ID ，登录 ID 为空时，返回匿名 ID
     */
    public String getCurrentDistinctId() {
        String mLoginId = getLoginId();
        if (!TextUtils.isEmpty( mLoginId )) {
            return mLoginId;
        } else {
            return null;
        }
    }

    /**
     * 设置当前用户的distinctId。一般情况下，如果是一个注册用户，则应该使用注册系统内
     * 的user_id，如果是个未注册用户，则可以选择一个不会重复的匿名ID，如设备ID等，如果
     * 客户没有调用identify，则使用SDK自动生成的匿名ID
     *
     * @param distinctId 当前用户的distinctId，仅接受数字、下划线和大小写字母
     */
    @Override
    public void identify(final String distinctId) {
        try {
            assertDistinctId( distinctId );
        } catch (Exception e) {
            BetaDataLog.printStackTrace( e );
            return;
        }
        mTrackTaskManager.addTrackEventTask( new Runnable() {
            @Override
            public void run() {
                try {
                    synchronized (mDistinctId) {
                        mDistinctId.commit( distinctId );
                    }
                } catch (Exception e) {
                    BetaDataLog.printStackTrace( e );
                }
            }
        } );
    }

    /**
     * 登录，设置当前用户的 loginId
     *
     * @param loginId 当前用户的 loginId，不能为空，且长度不能大于255
     */
    public void setLoginId(final String loginId) {
        if (loginId == null || "".equals( loginId )) {
            return;
        }
        synchronized (mPersistentLoginId) {
            if (!loginId.equals( mPersistentLoginId.get() )) {
                mPersistentLoginId.commit( loginId );
            }

        }
    }

    /**
     * 登录，设置当前用户的 loginId
     *
     * @param loginId 当前用户的 loginId，不能为空，且长度不能大于255
     */
    @Override
    public void login(final String loginId) {
        login( loginId, null );
    }

    /**
     * 登录，设置当前用户的 loginId
     *
     * @param loginId    当前用户的 loginId，不能为空，且长度不能大于255
     * @param properties 用户登录属性
     */
    @Override
    public void login(final String loginId, final JSONObject properties) {
        try {
            assertDistinctId( loginId );
        } catch (Exception e) {
            BetaDataLog.printStackTrace( e );
            return;
        }
        mTrackTaskManager.addTrackEventTask( new Runnable() {
            @Override
            public void run() {
                try {
                    synchronized (mPersistentLoginId) {
                        String id = mPersistentLoginId.get();
                        if (!loginId.equals( mPersistentLoginId.get() )) {
                            mPersistentLoginId.commit( loginId );
                            long eventTime = System.currentTimeMillis();
                            JSONObject userProperties = new JSONObject();
                            JSONObject eventProperties = new JSONObject();

                            userProperties.put( AopConstants.BETA_LAST_TIME, eventTime );// 上次登录时间
                            trackEvent( EventType.TRACK_SIGNUP, AopConstants.BETA_APP_LOGIN, eventProperties, userProperties, loginId );
                        }

                    }
                } catch (Exception e) {
                    BetaDataLog.printStackTrace( e );
                }
            }
        } );
    }

    /**
     * 注销，清空当前用户的 loginId
     */
    @Override
    public void logout() {
        mTrackTaskManager.addTrackEventTask( new Runnable() {
            @Override
            public void run() {
                try {
                    flush();
                    synchronized (mPersistentLoginId) {
                        mPersistentLoginId.commit( null );
                    }
                } catch (Exception e) {
                    BetaDataLog.printStackTrace( e );
                }
            }
        } );
    }

    /**
     * 记录第一次登录行为
     * <p>
     * 这个接口是一个较为复杂的功能，请在使用前先阅读相关说明:
     * http://www.sensorsdata.cn/manual/track_signup.html
     * 并在必要时联系我们的技术支持人员。
     * 该方法已不推荐使用，可以具体参考 {@link #login(String)} 方法
     *
     * @param newDistinctId 用户完成注册后生成的注册ID
     * @param properties    事件的属性
     */
    @Deprecated
    @Override
    public void trackSignUp(final String newDistinctId, final JSONObject properties) {
        mTrackTaskManager.addTrackEventTask( new Runnable() {
            @Override
            public void run() {
                try {
                    String originalDistinctId = getDistinctId();

                    synchronized (mDistinctId) {
                        mDistinctId.commit( newDistinctId );
                    }

                    trackEvent( EventType.TRACK_SIGNUP, AopConstants.BETA_SIGN_UP, properties, null, originalDistinctId );
                } catch (Exception e) {
                    BetaDataLog.printStackTrace( e );
                }
            }
        } );
    }

    /**
     * 与 {@link #trackSignUp(String, JSONObject)} 类似，无事件属性
     * <p>
     * 这个接口是一个较为复杂的功能，请在使用前先阅读相关说明:
     * http://www.sensorsdata.cn/manual/track_signup.html，
     * 并在必要时联系我们的技术支持人员。
     * 该方法已不推荐使用，可以具体参考 {@link #login(String)} 方法
     *
     * @param newDistinctId 用户完成注册后生成的注册ID
     */
    @Deprecated
    @Override
    public void trackSignUp(final String newDistinctId) {
        mTrackTaskManager.addTrackEventTask( new Runnable() {
            @Override
            public void run() {
                try {
                    String originalDistinctId = getDistinctId();
                    synchronized (mDistinctId) {
                        mDistinctId.commit( newDistinctId );
                    }

                    trackEvent( EventType.TRACK_SIGNUP, AopConstants.BETA_SIGN_UP, null, null, originalDistinctId );
                } catch (Exception e) {
                    BetaDataLog.printStackTrace( e );
                }
            }
        } );
    }

    /**
     * 用于在 App 首次启动时追踪渠道来源，并设置追踪渠道事件的属性。
     * <p>
     * 这是 Sensors Analytics 进阶功能，请参考文档 https://sensorsdata.cn/manual/track_installation.html
     *
     * @param eventName       渠道追踪事件的名称
     * @param properties      渠道追踪事件的属性
     * @param disableCallback 是否关闭这次渠道匹配的回调请求
     */
    @Override
    public void trackInstallation(final String eventName, final JSONObject properties, final boolean disableCallback) {
//        //只在主进程触发 trackInstallation
//        final JSONObject _properties;
//        if (properties != null) {
//            _properties = properties;
//        } else {
//            _properties = new JSONObject();
//        }
//
//        mTrackTaskManager.addTrackEventTask( new Runnable() {
//            @Override
//            public void run() {
//                try {
//                    if (!mIsMainProcess) {
//                        return;
//                    }
//                } catch (Exception e) {
//                    BetaDataLog.printStackTrace( e );
//                }
//
//                try {
//                    boolean firstTrackInstallation;
//                    if (disableCallback) {
//                        firstTrackInstallation = mFirstTrackInstallationWithCallback.get();
//                    } else {
//                        firstTrackInstallation = mFirstTrackInstallation.get();
//                    }
//                    if (firstTrackInstallation) {
//                        try {
//                            if (!BetaDataUtils.hasUtmProperties( _properties )) {
//                                Map<String, String> utmMap = new HashMap<>();
////                                utmMap.put("SENSORS_ANALYTICS_UTM_SOURCE", "$utm_source");
////                                utmMap.put("SENSORS_ANALYTICS_UTM_MEDIUM", "$utm_medium");
////                                utmMap.put("SENSORS_ANALYTICS_UTM_TERM", "$utm_term");
////                                utmMap.put("SENSORS_ANALYTICS_UTM_CONTENT", "$utm_content");
////                                utmMap.put("SENSORS_ANALYTICS_UTM_CAMPAIGN", "$utm_campaign");
//
//                                for (Map.Entry<String, String> entry : utmMap.entrySet()) {
//                                    if (entry != null) {
//                                        String utmValue = BetaDataUtils.getApplicationMetaData( mContext, entry.getKey() );
//                                        if (!TextUtils.isEmpty( utmValue )) {
//                                            _properties.put( entry.getValue(), utmValue );
//                                        }
//                                    }
//                                }
//                            }
//
//                            if (!BetaDataUtils.hasUtmProperties( _properties )) {
//                                String installSource = String.format( "android_id=%s##imei=%s##mac=%s",
//                                        mAndroidId,
//                                        BetaDataUtils.getIMEI( mContext ),
//                                        BetaDataUtils.getMacAddress( mContext ) );
////                                _properties.put("$ios_install_source", installSource);
//                            }
//
//                            if (disableCallback) {
////                                _properties.put("$ios_install_disable_callback", disableCallback);
//                            }
//                        } catch (Exception e) {
//                            BetaDataLog.printStackTrace( e );
//                        }
//
//                        // 先发送 track
//                        trackEvent( EventType.TRACK, eventName, _properties, null, null );
//
//                        // 再发送 profile_set_once
//                        JSONObject profileProperties = new JSONObject();
//                        if (_properties != null) {
//                            profileProperties = new JSONObject( _properties.toString() );
//                        }
////                        profileProperties.put("$first_visit_time", new Date());
//                        trackEvent( EventType.PROFILE_SET_ONCE, null, profileProperties, null, null );
//
//                        if (disableCallback) {
//                            mFirstTrackInstallationWithCallback.commit( false );
//                        } else {
//                            mFirstTrackInstallation.commit( false );
//                        }
//                    }
//                    flush();
//                } catch (Exception e) {
//                    BetaDataLog.printStackTrace( e );
//                }
//            }
//        } );
    }

    @Override
    public void trackInstallation(String eventName, JSONObject properties) {

    }

    @Override
    public void trackInstallation(String eventName) {

    }

//    /**
//     * 用于在 App 首次启动时追踪渠道来源，并设置追踪渠道事件的属性。
//     * <p>
//     * 这是 Sensors Analytics 进阶功能，请参考文档 https://sensorsdata.cn/manual/track_installation.html
//     *
//     * @param eventName  渠道追踪事件的名称
//     * @param properties 渠道追踪事件的属性
//     */
//    @Override
//    public void trackInstallation(String eventName, JSONObject properties) {
//        trackInstallation( eventName, properties, false );
//    }
//
//    /**
//     * 用于在 App 首次启动时追踪渠道来源，并设置追踪渠道事件的属性。
//     * <p>
//     * 这是 Sensors Analytics 进阶功能，请参考文档 https://sensorsdata.cn/manual/track_installation.html
//     *
//     * @param eventName 渠道追踪事件的名称
//     */
//    @Override
//    public void trackInstallation(String eventName) {
//        trackInstallation( eventName, null, false );
//    }

    /**
     * 调用track接口，追踪一个带有属性的事件
     *
     * @param eventName  事件的名称
     * @param properties 事件的属性
     */
    @Override
    public void track(final String eventName, final JSONObject properties) {
        mTrackTaskManager.addTrackEventTask( new Runnable() {
            @Override
            public void run() {
                try {


                    trackEvent( EventType.TRACK, eventName, properties, null, null );

                } catch (Exception e) {
                    Log.e("BT.SendMessage",e.getCause().getMessage());
                    BetaDataLog.printStackTrace( e );
                }

            }
        } );
    }

    /**
     * 调用track_user_info 更新用户属性接口，追踪一个带有属性的事件
     *
     * @param eventName  事件的名称
     * @param properties 事件的属性
     */
    public void track_user_info(final String eventName, final JSONObject properties) {
        mTrackTaskManager.addTrackEventTask( new Runnable() {
            @Override
            public void run() {
                try {
                    trackEvent( EventType.TRACK_USER_INFO, eventName, properties, null, null );
                } catch (Exception e) {
                    BetaDataLog.printStackTrace( e );
                }
            }
        } );
    }

    /**
     * 与 {@link #track(String, JSONObject)} 类似，无事件属性
     *
     * @param eventName 事件的名称
     */
    @Override
    public void track(final String eventName) {
        mTrackTaskManager.addTrackEventTask( new Runnable() {
            @Override
            public void run() {
                try {
                    trackEvent( EventType.TRACK, eventName, null, null, null );
                } catch (Exception e) {
                    BetaDataLog.printStackTrace( e );
                }
            }
        } );
    }

    /**
     * 初始化事件的计时器，默认计时单位为毫秒。
     * <p>
     * 详细用法请参考 trackTimer(String, TimeUnit)
     *
     * @param eventName 事件的名称
     */
    @Deprecated
    @Override
    public void trackTimer(final String eventName) {
        trackTimer( eventName, TimeUnit.MILLISECONDS );
    }

    /**
     * 初始化事件的计时器。
     * <p>
     * 若需要统计某个事件的持续时间，先在事件开始时调用 trackTimer("Event") 记录事件开始时间，该方法并不会真正发
     * 送事件；随后在事件结束时，调用 track("Event", properties)，SDK 会追踪 "Event" 事件，并自动将事件持续时
     * 间记录在事件属性 "event_duration" 中。
     * <p>
     * 多次调用 trackTimer("Event") 时，事件 "Event" 的开始时间以最后一次调用时为准。
     *
     * @param eventName 事件的名称
     * @param timeUnit  计时结果的时间单位
     */
    @Deprecated
    @Override
    public void trackTimer(final String eventName, final TimeUnit timeUnit) {
        final long startTime = SystemClock.elapsedRealtime();

        mTrackTaskManager.addTrackEventTask( new Runnable() {
            @Override
            public void run() {
                try {
//                    assertKey( eventName );
                    synchronized (mTrackTimer) {
                        mTrackTimer.put(eventName, new EventTimer(timeUnit, startTime));
                    }
                } catch (Exception e) {
                    BetaDataLog.printStackTrace( e );
                }
            }
        } );
    }


    /**
     * 初始化事件的计时器。
     * <p>
     * 若需要统计某个事件的持续时间，先在事件开始时调用 trackTimer("Event") 记录事件开始时间，该方法并不会真正发
     * 送事件；随后在事件结束时，调用 track("Event", properties)，SDK 会追踪 "Event" 事件，并自动将事件持续时
     * 间记录在事件属性 "event_duration" 中。
     * <p>
     * 多次调用 trackTimer("Event") 时，事件 "Event" 的开始时间以最后一次调用时为准。
     *
     * @param eventName  事件的名称
     * @param eventTimer 自定义事件计时器
     */
    @Override
    public void trackTimer(final String eventName, final EventTimer eventTimer) {
        mTrackTaskManager.addTrackEventTask( new Runnable() {
            @Override
            public void run() {
                try {
//                    assertKey( eventName );
                    synchronized (mTrackTimer) {
                        mTrackTimer.put( eventName, eventTimer );
                    }
                } catch (Exception ex) {
                    BetaDataLog.printStackTrace( ex );
                }
            }
        } );
    }

    /**
     * 删除指定时间的计时器
     *
     * @param eventName 事件名称
     */
    @Override
    public void removeTimer(final String eventName) {
        mTrackTaskManager.addTrackEventTask( new Runnable() {
            @Override
            public void run() {
                try {
//                    assertKey( eventName );
                    synchronized (mTrackTimer) {
                        mTrackTimer.remove( eventName );
                    }
                } catch (Exception e) {
                    BetaDataLog.printStackTrace( e );
                }
            }
        } );
    }


    /**
     * 初始化事件的计时器，计时单位为秒。
     *
     * @param eventName 事件的名称
     */
    @Override
    public void trackTimerStart(String eventName) {
        trackTimerBegin( eventName, TimeUnit.MILLISECONDS );
    }

    /**
     * 初始化事件的计时器，默认计时单位为毫秒。
     * <p>
     * 详细用法请参考 trackTimerBegin(String, TimeUnit)
     *
     * @param eventName 事件的名称
     */
    @Override
    @Deprecated
    public void trackTimerBegin(final String eventName) {
        trackTimer( eventName );
    }

    /**
     * 初始化事件的计时器。
     * <p>
     * 若需要统计某个事件的持续时间，先在事件开始时调用 trackTimerBegin("Event") 记录事件开始时间，该方法并不会真正发
     * 送事件；随后在事件结束时，调用 track("Event", properties)，SDK 会追踪 "Event" 事件，并自动将事件持续时
     * 间记录在事件属性 "event_duration" 中。
     * <p>
     * 多次调用 trackTimerBegin("Event") 时，事件 "Event" 的开始时间以最后一次调用时为准。
     *
     * @param eventName 事件的名称
     * @param timeUnit  计时结果的时间单位
     */
    @Override
    @Deprecated
    public void trackTimerBegin(final String eventName, final TimeUnit timeUnit) {
        trackTimer( eventName, timeUnit );
    }

    /**
     * 停止事件计时器
     *
     * @param eventName  事件的名称
     * @param properties 事件的属性
     */
    @Override
    public void trackTimerEnd(final String eventName, final JSONObject properties) {
        long endTime = SystemClock.elapsedRealtime();
        if (eventName != null) {
            synchronized (mTrackTimer) {
                EventTimer eventTimer = mTrackTimer.get(eventName);
                if (eventTimer != null) {
                    eventTimer.setEndTime(endTime);
                }
            }
        }
        mTrackTaskManager.addTrackEventTask( new Runnable() {
            @Override
            public void run() {
                try {
                    trackEvent( EventType.TRACK, eventName, properties, null, null );
                } catch (Exception e) {
                    BetaDataLog.printStackTrace( e );
                }
            }
        } );
    }

    /**
     * 停止事件计时器
     *
     * @param eventName 事件的名称
     */
    @Override
    public void trackTimerEnd(final String eventName) {
        long endTime = SystemClock.elapsedRealtime();
        if (eventName != null) {
            synchronized (mTrackTimer) {
                EventTimer eventTimer = mTrackTimer.get(eventName);
                if (eventTimer != null) {
                    eventTimer.setEndTime(endTime);
                }
            }
        }
        mTrackTaskManager.addTrackEventTask( new Runnable() {
            @Override
            public void run() {
                try {
                    trackEvent( EventType.TRACK, eventName, null, null, null );
                } catch (Exception e) {
                    BetaDataLog.printStackTrace( e );
                }
            }
        } );
    }

    /**
     * 清除所有事件计时器
     */
    @Override
    public void clearTrackTimer() {
        mTrackTaskManager.addTrackEventTask( new Runnable() {
            @Override
            public void run() {
                try {
                    synchronized (mTrackTimer) {
                        mTrackTimer.clear();
                    }
                } catch (Exception e) {
                    BetaDataLog.printStackTrace( e );
                }
            }
        } );
    }

    /**
     * 获取LastScreenUrl
     *
     * @return String
     */
    @Override
    public String getLastScreenUrl() {
        return mLastScreenUrl;
    }

    /**
     * App 退出或进到后台时清空 referrer，默认情况下不清空
     */
    @Override
    public void clearReferrerWhenAppEnd() {
        mClearReferrerWhenAppEnd = true;
    }

    @Override
    public void clearLastScreenUrl() {
        if (mClearReferrerWhenAppEnd) {
            mLastScreenUrl = null;
        }
    }

    @Override
    @Deprecated
    public String getMainProcessName() {
        return mMainProcessName;
    }

    /**
     * 获取LastScreenTrackProperties
     *
     * @return JSONObject
     */
    @Override
    public JSONObject getLastScreenTrackProperties() {
        return mLastScreenTrackProperties;
    }

    /**
     * Track 进入页面事件 ($AppViewScreen)
     *
     * @param url        String
     * @param properties JSONObject
     */
    @Override
    public void trackViewScreen(final String url, final JSONObject properties) {
        mTrackTaskManager.addTrackEventTask( new Runnable() {
            @Override
            public void run() {
                try {
                    if (!TextUtils.isEmpty( url ) || properties != null) {
                        JSONObject trackProperties = new JSONObject();
                        mLastScreenTrackProperties = properties;

                        if (!TextUtils.isEmpty( mLastScreenUrl )) {
                            trackProperties.put( AopConstants.BETA_SCREEN_NAME, mLastScreenUrl );
                        }

                        trackProperties.put( AopConstants.BETA_URL, url );
                        mLastScreenUrl = url;
                        if (properties != null) {
                            BetaDataUtils.mergeJSONObject( properties, trackProperties );
                        }
                        track( AopConstants.BETA_APP_PAGEVIEW, trackProperties );
                    }
                } catch (Exception e) {
                    BetaDataLog.printStackTrace( e );
                }
            }
        } );
    }

    /**
     * Track Activity 进入页面事件($AppViewScreen)
     *
     * @param activity activity Activity，当前 Activity
     */
    @Override
    public void trackViewScreen(final Activity activity) {
        mTrackTaskManager.addTrackEventTask( new Runnable() {
            @Override
            public void run() {
                try {
                    if (activity == null) {
                        return;
                    }

                    JSONObject properties = new JSONObject();
                    properties.put( AopConstants.BETA_SCREEN_NAME, activity.getClass().getSimpleName() );
                    BetaDataUtils.getScreenNameAndTitleFromActivity( properties, activity );

                    if (activity instanceof ScreenAutoTracker) {
                        ScreenAutoTracker screenAutoTracker = (ScreenAutoTracker) activity;

                        String screenUrl = screenAutoTracker.getScreenUrl();
                        JSONObject otherProperties = screenAutoTracker.getTrackProperties();
                        if (otherProperties != null) {
                            BetaDataUtils.mergeJSONObject( otherProperties, properties );
                        }

                        trackViewScreen( screenUrl, properties );
                    } else {
                        track( "_app_pageview", properties );
                    }
                } catch (Exception e) {
                    BetaDataLog.printStackTrace( e );
                }
            }
        } );
    }

    @Override
    public void trackViewScreen(final Object fragment) {
        if (fragment == null) {
            return;
        }

        Class<?> supportFragmentClass = null;
        Class<?> appFragmentClass = null;
        Class<?> androidXFragmentClass = null;

        try {
            try {
                supportFragmentClass = Class.forName( "android.support.v4.app.Fragment" );
            } catch (Exception e) {
                //ignored
            }

            try {
                appFragmentClass = Class.forName( "android.app.Fragment" );
            } catch (Exception e) {
                //ignored
            }

            try {
                androidXFragmentClass = Class.forName( "androidx.fragment.app.Fragment" );
            } catch (Exception e) {
                //ignored
            }
        } catch (Exception e) {
            //ignored
        }

        if (!(supportFragmentClass != null && supportFragmentClass.isInstance( fragment )) &&
                !(appFragmentClass != null && appFragmentClass.isInstance( fragment )) &&
                !(androidXFragmentClass != null && androidXFragmentClass.isInstance( fragment ))) {
            return;
        }

        mTrackTaskManager.addTrackEventTask( new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject properties = new JSONObject();
                    String screenName = fragment.getClass().getSimpleName();

                    String title = null;

                    if (fragment.getClass().isAnnotationPresent( BetaDataFragmentTitle.class )) {
                        BetaDataFragmentTitle betaDataFragmentTitle = fragment.getClass().getAnnotation( BetaDataFragmentTitle.class );
                        if (betaDataFragmentTitle != null) {
                            title = betaDataFragmentTitle.title();
                        }
                    }

                    if (Build.VERSION.SDK_INT >= 11) {
                        Activity activity = null;
                        try {
                            Method getActivityMethod = fragment.getClass().getMethod( "getActivity" );
                            if (getActivityMethod != null) {
                                activity = (Activity) getActivityMethod.invoke( fragment );
                            }
                        } catch (Exception e) {
                            //ignored
                        }
                        if (activity != null) {
                            if (TextUtils.isEmpty( title )) {
                                title = BetaDataUtils.getActivityTitle( activity );

                            }
                            screenName = String.format( Locale.CHINA, "%s|%s", activity.getClass().getCanonicalName(), screenName );
                        }
                    }

                    if (!TextUtils.isEmpty( title )) {
                        properties.put( AopConstants.BETA_TITLE, title );
                    }

                    properties.put( AopConstants.BETA_SCREEN_NAME, screenName );
                    track( AopConstants.BETA_APP_PAGEVIEW, properties );
                } catch (Exception e) {
                    BetaDataLog.printStackTrace( e );
                }
            }
        } );
    }

    /**
     * app进入后台
     * 遍历mTrackTimer
     * eventAccumulatedDuration = eventAccumulatedDuration + System.currentTimeMillis() - startTime
     */
    protected void appEnterBackground() {
        synchronized (mTrackTimer) {
            try {
                Iterator iter = mTrackTimer.entrySet().iterator();
                while (iter.hasNext()) {
                    Map.Entry entry = (Map.Entry) iter.next();
                    if (entry != null) {
                        if (AopConstants.BETA_APP_END.equals( entry.getKey().toString() )) {
                            continue;
                        }
                        EventTimer eventTimer = (EventTimer) entry.getValue();
                        if (eventTimer != null) {
                            long eventAccumulatedDuration = eventTimer.getEventAccumulatedDuration() + SystemClock.elapsedRealtime() - eventTimer.getStartTime();
                            eventTimer.setEventAccumulatedDuration( eventAccumulatedDuration );
                            eventTimer.setStartTime( SystemClock.elapsedRealtime() );
                        }
                    }
                }
            } catch (Exception e) {
                BetaDataLog.i( BetaDataConstant.BT_TAG, "appEnterBackground error:" + e.getMessage() );
            }
        }
    }

    /**
     * app从后台恢复
     * 遍历mTrackTimer
     * SystemClock.elapsedRealtime 从设备开机到现在的时间，单位毫秒，含系统深度睡眠时间
     * startTime = System.currentTimeMillis()
     */
    protected void appBecomeActive() {
        synchronized (mTrackTimer) {
            try {
                Iterator iter = mTrackTimer.entrySet().iterator();
                while (iter.hasNext()) {
                    Map.Entry entry = (Map.Entry) iter.next();
                    if (entry != null) {
                        EventTimer eventTimer = (EventTimer) entry.getValue();
                        if (eventTimer != null) {
                            eventTimer.setStartTime( SystemClock.elapsedRealtime() );
                        }
                    }
                }
            } catch (Exception e) {
                BetaDataLog.i( BetaDataConstant.BT_TAG, "appBecomeActive error:" + e.getMessage() );
            }
        }
    }

    /**
     * 将所有本地缓存的日志发送到 Sensors Analytics.
     */
    @Override
    public void flush() {
        mMessages.flush();
    }

    /**
     * 延迟指定毫秒数将所有本地缓存的日志发送到 Sensors Analytics.
     *
     * @param timeDelayMills 延迟毫秒数
     */
    public void flush(long timeDelayMills) {
        mMessages.flush( timeDelayMills );
    }

    /**
     * 以阻塞形式将所有本地缓存的日志发送到 Sensors Analytics，该方法不能在 UI 线程调用。
     */
    @Override
    public void flushSync() {
        mMessages.sendData();
    }

    /**
     * 以阻塞形式入库数据
     */
    void flushDataSync() {
        mTrackTaskManager.addEventDBTask( new Runnable() {
            @Override
            public void run() {
                mMessages.flushDataSync();
            }
        } );
    }

    /**
     * 注册事件动态公共属性
     *
     * @param dynamicSuperProperties 事件动态公共属性回调接口
     */
    @Override
    public void registerDynamicSuperProperties(BetaDataDynamicSuperProperties dynamicSuperProperties) {
        mDynamicSuperProperties = dynamicSuperProperties;
    }

    /**
     * 设置 track 事件回调
     *
     * @param trackEventCallBack track 事件回调接口
     */
    @Override
    public void setTrackEventCallBack(BetaDataTrackEventCallBack trackEventCallBack) {
        mTrackEventCallBack = trackEventCallBack;
    }

    /**
     * 删除本地缓存的全部事件
     */
    @Override
    public void deleteAll() {
        mMessages.deleteAll();
    }

    /**
     * 获取事件公共属性
     *
     * @return 当前所有Super属性
     */
    @Override
    public JSONObject getSuperProperties() {
        synchronized (mSuperProperties) {
            return mSuperProperties.get();
        }
    }

    /**
     * 注册所有事件都有的公共属性
     *
     * @param superProperties 事件公共属性
     */
    @Override
    public void registerSuperProperties(JSONObject superProperties) {
        try {
            if (superProperties == null) {
                return;
            }
            assertPropertyTypes( superProperties );
            synchronized (mSuperProperties) {
                JSONObject properties = mSuperProperties.get();
                BetaDataUtils.mergeSuperJSONObject( superProperties, properties );
                mSuperProperties.commit( properties );
            }
        } catch (Exception e) {
            BetaDataLog.printStackTrace( e );
        }
    }

    /**
     * 删除事件公共属性
     *
     * @param superPropertyName 事件属性名称
     */
    @Override
    public void unregisterSuperProperty(String superPropertyName) {
        try {
            synchronized (mSuperProperties) {
                JSONObject superProperties = mSuperProperties.get();
                superProperties.remove( superPropertyName );
                mSuperProperties.commit( superProperties );
            }
        } catch (Exception e) {
            BetaDataLog.printStackTrace( e );
        }
    }

    /**
     * 删除所有事件公共属性
     */
    @Override
    public void clearSuperProperties() {
        synchronized (mSuperProperties) {
            mSuperProperties.commit( new JSONObject() );
        }
    }

    /**
     * 设置用户的一个或多个Profile。
     * Profile如果存在，则覆盖；否则，新创建。
     */
    public void signupSet(final String loginId, final JSONObject eventProperties, final JSONObject userProperties) {
        // 初始化登录
        try {
            assertDistinctId( loginId );
        } catch (Exception e) {
            BetaDataLog.printStackTrace( e );
            return;
        }
        mTrackTaskManager.addTrackEventTask( new Runnable() {
            @Override
            public void run() {
                try {
                    synchronized (mPersistentLoginId) {
                        String id = mPersistentLoginId.get();
                        if (!loginId.equals( mPersistentLoginId.get() )) {
                            mPersistentLoginId.commit( loginId );
                        }
                        trackEvent( EventType.TRACK_SIGNUP, AopConstants.BETA_APP_REGISTER, eventProperties, userProperties, null );
                    }
                } catch (Exception e) {
                    BetaDataLog.printStackTrace( e );
                }
            }
        } );
    }

    @Override
    public void profileSet(JSONObject properties) {

    }

    /**
     * 设置用户的一个或多个Profile。
     * Profile如果存在，则覆盖；否则，新创建。
     *
     * @param properties 属性列表
     */
    @Override
    public void profileSet(final JSONObject properties, final JSONObject userProperties) {
        mTrackTaskManager.addTrackEventTask( new Runnable() {
            @Override
            public void run() {
                try {
                    trackEvent( EventType.PROFILE_SET, AopConstants.BETA_APP_PROFILE, properties, userProperties, null );
                } catch (Exception e) {
                    BetaDataLog.printStackTrace( e );
                }
            }
        } );
    }

    /**
     * 设置用户的一个Profile，如果之前存在，则覆盖，否则，新创建
     *
     * @param property 属性名称
     * @param value    属性的值，值的类型只允许为
     *                 {@link String}, {@link Number}, {@link Date}, {@link List}
     */
    @Override
    public void profileSet(final String property, final Object value) {
        mTrackTaskManager.addTrackEventTask( new Runnable() {
            @Override
            public void run() {
                try {
                    trackEvent( EventType.PROFILE_SET, null, new JSONObject().put( property, value ), null, null );
                } catch (Exception e) {
                    BetaDataLog.printStackTrace( e );
                }
            }
        } );
    }

    /**
     * 首次设置用户的一个或多个Profile。
     * 与profileSet接口不同的是，如果之前存在，则忽略，否则，新创建
     *
     * @param properties 属性列表
     */
    @Override
    public void profileSetOnce(final JSONObject properties) {
        mTrackTaskManager.addTrackEventTask( new Runnable() {
            @Override
            public void run() {
                try {
                    trackEvent( EventType.PROFILE_SET_ONCE, null, properties, null, null );
                } catch (Exception e) {
                    BetaDataLog.printStackTrace( e );
                }
            }
        } );
    }

    /**
     * 首次设置用户的一个Profile
     * 与profileSet接口不同的是，如果之前存在，则忽略，否则，新创建
     *
     * @param property 属性名称
     * @param value    属性的值，值的类型只允许为
     *                 {@link String}, {@link Number}, {@link Date}, {@link List}
     */
    @Override
    public void profileSetOnce(final String property, final Object value) {
        mTrackTaskManager.addTrackEventTask( new Runnable() {
            @Override
            public void run() {
                try {
                    trackEvent( EventType.PROFILE_SET_ONCE, null, new JSONObject().put( property, value ), null, null );
                } catch (Exception e) {
                    BetaDataLog.printStackTrace( e );
                }
            }
        } );
    }

    /**
     * 给一个或多个数值类型的Profile增加一个数值。只能对数值型属性进行操作，若该属性
     * 未设置，则添加属性并设置默认值为0
     *
     * @param properties 一个或多个属性集合
     */
    @Override
    public void profileIncrement(final Map<String, ? extends Number> properties) {
        mTrackTaskManager.addTrackEventTask( new Runnable() {
            @Override
            public void run() {
                try {
                    trackEvent( EventType.PROFILE_INCREMENT, null, new JSONObject( properties ), null, null );
                } catch (Exception e) {
                    BetaDataLog.printStackTrace( e );
                }
            }
        } );
    }

    /**
     * 给一个数值类型的Profile增加一个数值。只能对数值型属性进行操作，若该属性
     * 未设置，则添加属性并设置默认值为0
     *
     * @param property 属性名称
     * @param value    属性的值，值的类型只允许为 {@link Number}
     */
    @Override
    public void profileIncrement(final String property, final Number value) {
        mTrackTaskManager.addTrackEventTask( new Runnable() {
            @Override
            public void run() {
                try {
                    trackEvent( EventType.PROFILE_INCREMENT, null, new JSONObject().put( property, value ), null, null );
                } catch (Exception e) {
                    BetaDataLog.printStackTrace( e );
                }
            }
        } );
    }

    /**
     * 给一个列表类型的Profile增加一个元素
     *
     * @param property 属性名称
     * @param value    新增的元素
     */
    @Override
    public void profileAppend(final String property, final String value) {
        mTrackTaskManager.addTrackEventTask( new Runnable() {
            @Override
            public void run() {
                try {
                    final JSONArray append_values = new JSONArray();
                    append_values.put( value );
                    final JSONObject properties = new JSONObject();
                    properties.put( property, append_values );
                    trackEvent( EventType.PROFILE_APPEND, null, properties, null, null );
                } catch (Exception e) {
                    BetaDataLog.printStackTrace( e );
                }
            }
        } );
    }

    /**
     * 给一个列表类型的Profile增加一个或多个元素
     *
     * @param property 属性名称
     * @param values   新增的元素集合
     */
    @Override
    public void profileAppend(final String property, final Set<String> values) {
        mTrackTaskManager.addTrackEventTask( new Runnable() {
            @Override
            public void run() {
                try {
                    final JSONArray append_values = new JSONArray();
                    for (String value : values) {
                        append_values.put( value );
                    }
                    final JSONObject properties = new JSONObject();
                    properties.put( property, append_values );
                    trackEvent( EventType.PROFILE_APPEND, null, properties, null, null );
                } catch (Exception e) {
                    BetaDataLog.printStackTrace( e );
                }
            }
        } );
    }

    /**
     * 删除用户的一个Profile
     *
     * @param property 属性名称
     */
    @Override
    public void profileUnset(final String property) {
        mTrackTaskManager.addTrackEventTask( new Runnable() {
            @Override
            public void run() {
                try {
                    trackEvent( EventType.PROFILE_UNSET, null, new JSONObject().put( property, true ), null, null );
                } catch (Exception e) {
                    BetaDataLog.printStackTrace( e );
                }
            }
        } );
    }

    /**
     * 删除用户所有Profile
     */
    @Override
    public void profileDelete() {
        mTrackTaskManager.addTrackEventTask( new Runnable() {
            @Override
            public void run() {
                try {
                    trackEvent( EventType.PROFILE_DELETE, null, null, null, null );
                } catch (Exception e) {
                    BetaDataLog.printStackTrace( e );
                }
            }
        } );
    }

    @Override
    public boolean isDebugMode() {
        return mDebugMode.isDebugMode();
    }

    @Override
    public boolean isFlushInBackground() {
        return mFlushInBackground;
    }

    @Override
    public void setFlushInBackground(boolean Flush) {
        mFlushInBackground = Flush;
    }


    String getServerUrl() {
        return mServerUrl;
    }


    @Override
    public void trackEventFromH5(String eventInfo, boolean enableVerify) {
//        try {
//            if (TextUtils.isEmpty( eventInfo )) {
//                return;
//            }
//
//            JSONObject eventObject = new JSONObject( eventInfo );
//            if (enableVerify) {
//                String serverUrl = eventObject.optString( "server_url" );
//                if (!TextUtils.isEmpty( serverUrl )) {
//                    if (!(new ServerUrl( serverUrl ).check( new ServerUrl( mServerUrl ) ))) {
//                        return;
//                    }
//                } else {
//                    //防止 H5 集成的 JS SDK 版本太老，没有发 server_url
//                    return;
//                }
//            }
//            trackEventFromH5( eventInfo );
//        } catch (Exception e) {
//            BetaDataLog.printStackTrace( e );
//        }
    }

    protected boolean _trackEventFromH5(String eventInfo) {
//        try {
//            if (TextUtils.isEmpty( eventInfo )) {
//                return false;
//            }
//            JSONObject eventObject = new JSONObject( eventInfo );
//
//            String serverUrl = eventObject.optString( "server_url" );
//            if (!TextUtils.isEmpty( serverUrl )) {
//                if (!(new ServerUrl( serverUrl ).check( new ServerUrl( mServerUrl ) ))) {
//                    return false;
//                }
//                trackEventFromH5( eventInfo );
//                return true;
//            }
//        } catch (Exception e) {
//            BetaDataLog.printStackTrace( e );
//        }
        return false;

    }

    @Override
    public void trackEventFromH5(String eventInfo) {
//        try {
//            if (TextUtils.isEmpty( eventInfo )) {
//                return;
//            }
//
//            JSONObject eventObject = new JSONObject( eventInfo );
//            String type = eventObject.getString( "type" );
//            EventType eventType = EventType.valueOf( type.toUpperCase() );
//
//            String distinctIdKey = "distinct_id";
//            if (eventType == EventType.TRACK_SIGNUP) {
//                eventObject.put( "original_id", getAnonymousId() );
//            } else if (!TextUtils.isEmpty( getLoginId() )) {
//                eventObject.put( distinctIdKey, getLoginId() );
//            } else {
//                eventObject.put( distinctIdKey, getAnonymousId() );
//            }
//
//            long eventTime = System.currentTimeMillis();
//            eventObject.put( "time", eventTime );
//
//            try {
//                SecureRandom secureRandom = new SecureRandom();
//                eventObject.put( "_track_id", secureRandom.nextInt() );
//            } catch (Exception e) {
//                //ignore
//            }
//
//            JSONObject propertiesObject = eventObject.optJSONObject( "properties" );
//            if (propertiesObject == null) {
//                propertiesObject = new JSONObject();
//            }
//
//            JSONObject libObject = eventObject.optJSONObject( "lib" );
//            if (libObject != null) {
//                if (mBaseEventInfo.containsKey( AopConstants.BETA_APP_VERSION )) {
//                    libObject.put( AopConstants.BETA_APP_VERSION, mBaseEventInfo.get( AopConstants.BETA_APP_VERSION ) );
//                }
//
//                //update lib $app_version from super properties
//                JSONObject superProperties = mSuperProperties.get();
//                if (superProperties != null) {
//                    if (superProperties.has( AopConstants.BETA_APP_VERSION )) {
//                        libObject.put( AopConstants.BETA_APP_VERSION, superProperties.get( AopConstants.BETA_APP_VERSION ) );
//                    }
//                }
//            }
//
//            if (eventType.isTrack()) {
//                if (mBaseEventInfo != null) {
//                    for (Map.Entry<String, Object> entry : mBaseEventInfo.entrySet()) {
//                        String key = entry.getKey();
//                        if (!TextUtils.isEmpty( key )) {
////                            if ("$lib".equals(key) || "$lib_version".equals(key)) {
////                                continue;
////                            }
//                            propertiesObject.put( entry.getKey(), entry.getValue() );
//                        }
//                    }
//                }
//
//                // 当前网络状况
//                String networkType = BetaDataUtils.networkType( mContext );
//                propertiesObject.put( AopConstants.BETA_WIFI, networkType.equals( AopConstants.BETA_WIFI ) );
//                propertiesObject.put( AopConstants.BETA_NETWORK_TYPE, networkType );
//
//                // SuperProperties
//                synchronized (mSuperProperties) {
//                    JSONObject superProperties = mSuperProperties.get();
//                    BetaDataUtils.mergeJSONObject( superProperties, propertiesObject );
//                }
//
//                try {
//                    if (mDynamicSuperProperties != null) {
//                        JSONObject dynamicSuperProperties = mDynamicSuperProperties.getDynamicSuperProperties();
//                        if (dynamicSuperProperties != null) {
//                            BetaDataUtils.mergeJSONObject( dynamicSuperProperties, propertiesObject );
//                        }
//                    }
//                } catch (Exception e) {
//                    BetaDataLog.printStackTrace( e );
//                }

//                //是否首日访问
//                if (eventType.isTrack()) {
//                    propertiesObject.put(AopConstants.BETA_IS_FIRST_DAY, isFirstDay(eventTime));
//                }
//            }

//            if (eventObject.has("_nocache")) {
//                eventObject.remove("_nocache");
//            }
//            if (eventObject.has("server_url")) {
//                eventObject.remove("server_url");
//            }
//
//            if (propertiesObject.has("$project")) {
//                eventObject.put("project", propertiesObject.optString("$project"));
//                propertiesObject.remove("$project");
//            }
//
//            if (propertiesObject.has("$token")) {
//                eventObject.put("token", propertiesObject.optString("$token"));
//                propertiesObject.remove("$token");
//            }

//            String eventName = eventObject.optString( "event" );
//            boolean enterDb = isEnterDb( eventName, propertiesObject );
//            if (!enterDb) {
//                BetaDataLog.d( TAG, eventName + " event can not enter database" );
//                return;
//            }
//            eventObject.put( "properties", propertiesObject );
//
//            if (eventType == EventType.TRACK_SIGNUP) {
//                String loginId = eventObject.getString( "distinct_id" );
//                synchronized (mPersistentLoginId) {
//                    if (!loginId.equals( mPersistentLoginId.get() )) {
//                        mPersistentLoginId.commit( loginId );
//                        if (!loginId.equals( getAnonymousId() )) {
//                            mMessages.enqueueEventMessage( type, eventObject );
//                        }
//                    }
//                }
//            } else {
//                mMessages.enqueueEventMessage( type, eventObject );
//            }
//            BetaDataLog.i( TAG, "track event:\n" + JSONUtils.formatJson( eventObject.toString() ) );
//        } catch (Exception e) {
//            //ignore
//            BetaDataLog.printStackTrace( e );
//        }
    }

    /**
     * @param eventName       事件名
     * @param eventProperties 事件属性
     * @return 该事件是否入库
     */
    private boolean isEnterDb(String eventName, JSONObject eventProperties) {
        boolean enterDb = true;
        if (mTrackEventCallBack != null) {
            BetaDataLog.d( BetaDataConstant.BT_TAG, "SDK have set trackEvent callBack" );
            try {
                JSONObject properties = new JSONObject();
                Iterator<String> iterator = eventProperties.keys();
                ArrayList<String> keys = new ArrayList<>();
                while (iterator.hasNext()) {
                    String key = iterator.next();
                    if (key.startsWith( "_" ) && !TextUtils.equals( key, AopConstants.BETA_DEVICE_ID )) {
                        continue;
                    }
                    Object value = eventProperties.opt( key );
                    properties.put( key, value );
                    keys.add( key );
                }
                enterDb = mTrackEventCallBack.onTrackEvent( eventName, properties );
                if (enterDb) {
                    for (String key : keys) {
                        eventProperties.remove( key );
                    }
                    Iterator<String> it = properties.keys();
                    while (it.hasNext()) {
                        String key = it.next();
//                        try {
//                            assertKey( key );
//                        } catch (Exception e) {
//                            BetaDataLog.printStackTrace( e );
//                            return false;
//                        }
                        Object value = properties.opt( key );
                        if (!(value instanceof String || value instanceof Number || value
                                instanceof JSONArray || value instanceof Boolean || value instanceof Date)) {
                            BetaDataLog.d( BetaDataConstant.BT_TAG, "The property value must be an instance of "
                                    + "String/Number/Boolean/JSONArray. [key='" + key + "', value='" + value.toString()
                                    + "']" );
                            return false;
                        }

                        if (AopConstants.BETA_CRASH_REASON.equals( key )) {
                            if (value instanceof String && ((String) value).length() > 8191 * 2) {
                                BetaDataLog.d( BetaDataConstant.BT_TAG, "The property value is too long. [key='" + key
                                        + "', value='" + value.toString() + "']" );
                                value = ((String) value).substring( 0, 8191 * 2 ) + "$";
                            }
                        } else {
                            if (value instanceof String && ((String) value).length() > 8191) {
                                BetaDataLog.d( BetaDataConstant.BT_TAG, "The property value is too long. [key='" + key
                                        + "', value='" + value.toString() + "']" );
                                value = ((String) value).substring( 0, 8191 ) + "$";
                            }
                        }
                        eventProperties.put( key, value );
                    }
                }

            } catch (Exception e) {
                BetaDataLog.printStackTrace( e );
            }
        }
        return enterDb;
    }

    /**
     * 初始化运营商参数
     * 检验运营商是否获取到，如果没有,则再次获取
     *
     * @param sendProperties
     */
    private void init_carrier(JSONObject sendProperties) {
        try {
            if (TextUtils.isEmpty( sendProperties.optString( "_carrier" ) )) {
                String carrier = BetaDataUtils.getCarrier( mContext );
                if (!TextUtils.isEmpty( carrier )) {
                    sendProperties.put( "_carrier", carrier );
                }
            }
        } catch (Exception e) {
            BetaDataLog.printStackTrace( e );
        }
    }


    public void stopTrackTaskThread() {
        mTrackTaskManager.addTrackEventTask( new Runnable() {
            @Override
            public void run() {
                mTrackTaskManagerThread.setStop( true );
                mTrackDBTaskManagerThread.setStop( true );
            }
        } );
    }

    public void resumeTrackTaskThread() {
        mTrackTaskManagerThread = new TrackTaskManagerThread();
        mTrackDBTaskManagerThread = new TrackDBTaskManagerThread();
        betaDataThreadPool = BetaDataThreadPool.getInstance();
        betaDataThreadPool.execute( mTrackTaskManagerThread );
        betaDataThreadPool.execute( mTrackDBTaskManagerThread );
    }


    private void assertPropertyTypes(JSONObject properties) throws
            InvalidDataException {
        if (properties == null) {
            return;
        }

        for (Iterator iterator = properties.keys(); iterator.hasNext(); ) {
            String key = (String) iterator.next();

            try {
                Object value = properties.get( key );

                if (!(value instanceof String || value instanceof Number || value
                        instanceof JSONArray || value instanceof Boolean || value instanceof Date)) {
                    throw new InvalidDataException( "The property value must be an instance of "
                            + "String/Number/Boolean/JSONArray. [key='" + key + "', value='" + value.toString()
                            + "']" );
                }

                if (AopConstants.BETA_CRASH_REASON.equals( key )) {
                    if (value instanceof String && ((String) value).length() > 8191 * 2) {
                        properties.put( key, ((String) value).substring( 0, 8191 * 2 ) + "$" );
                        BetaDataLog.d( BetaDataConstant.BT_TAG, "The property value is too long. [key='" + key
                                + "', value='" + value.toString() + "']" );
                    }
                } else {
                    if (value instanceof String && ((String) value).length() > 8191) {
                        properties.put( key, ((String) value).substring( 0, 8191 ) + "$" );
                        BetaDataLog.d( BetaDataConstant.BT_TAG, "The property value is too long. [key='" + key
                                + "', value='" + value.toString() + "']" );
                    }
                }
            } catch (JSONException e) {
                throw new InvalidDataException( "Unexpected property key. [key='" + key + "']" );
            }
        }
    }


    private void assertDistinctId(String key) throws InvalidDataException {
        if (key == null || key.length() < 1) {
            throw new InvalidDataException( "The distinct_id or original_id or login_id is empty." );
        }
        if (key.length() > 255) {
            throw new InvalidDataException( "The max length of distinct_id or original_id or login_id is 255." );
        }
    }


    /**
     * 是否是第一天
     *
     * @return
     */
    private boolean isFirstDay() {
        String firstDay = mFirstDay.get();
        if (firstDay == null) {
            return true;
        }
        try {
            if (mIsFirstDayDateFormat == null) {
                mIsFirstDayDateFormat = new SimpleDateFormat( "yyyy-MM-dd", Locale.getDefault() );
            }
            String current = mIsFirstDayDateFormat.format( System.currentTimeMillis() );
            return firstDay.equals( current );
        } catch (Exception e) {
            BetaDataLog.printStackTrace( e );
        }
        return true;
    }

    private boolean isFirstDay(long eventTime) {
        String firstDay = mFirstDay.get();
        if (firstDay == null) {
            return true;
        }
        try {
            if (mIsFirstDayDateFormat == null) {
                mIsFirstDayDateFormat = new SimpleDateFormat( "yyyy-MM-dd", Locale.getDefault() );
            }
            String current = mIsFirstDayDateFormat.format( eventTime );
            return firstDay.equals( current );
        } catch (Exception e) {
            BetaDataLog.printStackTrace( e );
        }
        return true;
    }



    /**
     * 设置手机唯一标识 imei
     */
    public void setPhoneImei(String imei) {
       if(imei!=null)
       {
           BetaPhoneInfoManager.getInstance().setImei(imei);
       }
    }

    /**
     * 设置手机唯一标识 oaid
     */
    public void setPhoneOaid(String oaid) {
        if(oaid!=null)
        {
            BetaPhoneInfoManager.getInstance().setOaid(oaid);
        }
    }


}
