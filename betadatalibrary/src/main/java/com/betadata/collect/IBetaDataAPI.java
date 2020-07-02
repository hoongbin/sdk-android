
package com.betadata.collect;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.view.View;
import android.webkit.WebView;

import org.json.JSONObject;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public interface IBetaDataAPI {
    /**
     * 返回预置属性
     *
     * @return JSONObject 预置属性
     */
    JSONObject getPresetProperties();

    /**
     * 设置当前 serverUrl
     *
     * @param serverUrl 当前 serverUrl
     */
    void setServerUrl(String serverUrl);

    /**
     * 设置是否开启 log
     *
     * @param enable boolean
     */
    void enableLog(boolean enable);

    /**
     * 获取本地缓存上限制
     *
     * @return 字节
     */
    long getMaxCacheSize();

    /**
     * 返回档期是否是开启 debug 模式
     *
     * @return true：是，false：不是
     */
    boolean isDebugMode();

    /**
     * 返回是否允许后台上传数据，默认是true
     *
     * @return 是否允许后台上传数据
     */
    boolean isFlushInBackground();

    /**
     * 设置是否允许后台上传数据，默认是 true
     *
     * @param isFlush boolean
     */
    void setFlushInBackground(boolean isFlush);

    /**
     * 设置本地缓存上限值，单位 byte，默认为 32MB：32 * 1024 * 1024
     *
     * @param maxCacheSize 单位 byte
     */
    void setMaxCacheSize(long maxCacheSize);

    /**
     * 设置 flush 时网络发送策略，默认 3G、4G、WI-FI 环境下都会尝试 flush
     *
     * @param networkType int 网络类型
     */
    void setFlushNetworkPolicy(int networkType);

    /**
     * 两次数据发送的最小时间间隔，单位毫秒
     *
     * 默认值为15 * 1000毫秒
     * 在每次调用track、signUp以及profileSet等接口的时候，都会检查如下条件，以判断是否向服务器上传数据:
     *
     * 1. 是否是WIFI/3G/4G网络条件
     * 2. 是否满足发送条件之一:
     * 1) 与上次发送的时间间隔是否大于 flushInterval
     * 2) 本地缓存日志数目是否大于 flushBulkSize
     *
     * 如果满足这两个条件，则向服务器发送一次数据；如果不满足，则把数据加入到队列中，等待下次检查时把整个队列的内
     * 容一并发送。需要注意的是，为了避免占用过多存储，队列最多只缓存20MB数据。
     *
     * @return 返回时间间隔，单位毫秒
     */
    int getFlushInterval();

    /**
     * 设置两次数据发送的最小时间间隔
     *
     * @param flushInterval 时间间隔，单位毫秒
     */
    void setFlushInterval(int flushInterval);

    /**
     * 返回本地缓存日志的最大条目数
     *
     * @return 条数
     */
    int getFlushBulkSize();

    /**
     * 设置本地缓存日志的最大条目数
     *
     * @param flushBulkSize 缓存数目
     */
    void setFlushBulkSize(int flushBulkSize);

    /**
     * 设置 App 切换到后台与下次事件的事件间隔
     *
     * 默认值为 30*1000 毫秒
     *
     * 若 App 在后台超过设定事件，则认为当前 Session 结束，发送 $AppEnd 事件
     *
     * @param sessionIntervalTime int
     */
    void setSessionIntervalTime(int sessionIntervalTime);

    /**
     * 设置 App 切换到后台与下次事件的事件间隔
     *
     * 默认值为 30*1000 毫秒
     *
     * 若 App 在后台超过设定事件，则认为当前 Session 结束，发送 $AppEnd 事件
     *
     * @return 返回设置的 SessionIntervalTime ，默认是 30s
     */
    int getSessionIntervalTime();

    /**
     * 打开 SDK 自动追踪
     *
     * 该功能自动追踪 App 的一些行为，例如 SDK 初始化、App 启动（$AppStart） / 关闭（$AppEnd）、
     * 进入页面（$AppViewScreen）等等，具体信息请参考文档:
     * https://sensorsdata.cn/manual/android_sdk.html
     *
     * 该功能仅在 API 14 及以上版本中生效，默认关闭
     */
    @Deprecated
    void enableAutoTrack();

    /**
     * 打开 SDK 自动追踪
     *
     * 该功能自动追踪 App 的一些行为，指定哪些 AutoTrack 事件被追踪，具体信息请参考文档:
     * https://sensorsdata.cn/manual/android_sdk.html
     *
     * 该功能仅在 API 14 及以上版本中生效，默认关闭
     *
     * @param eventTypeList 开启 AutoTrack 的事件列表
     */
    void enableAutoTrack(List<BetaDataAPI.AutoTrackEventType> eventTypeList);

    /**
     * 关闭 AutoTrack 中的部分事件
     *
     * @param eventTypeList AutoTrackEventType 类型 List
     */
    void disableAutoTrack(List<BetaDataAPI.AutoTrackEventType> eventTypeList);

    /**
     * 关闭 AutoTrack 中的某个事件
     *
     * @param autoTrackEventType AutoTrackEventType 类型
     */
    void disableAutoTrack(BetaDataAPI.AutoTrackEventType autoTrackEventType);

    /**
     * 自动收集 App Crash 日志，该功能默认是关闭的
     */
    void trackAppCrash(Class clz);

    /**
     * 是否开启 AutoTrack
     *
     * @return true: 开启 AutoTrack; false：没有开启 AutoTrack
     */
    boolean isOpenAutoTrack();

//    /**
//     * 是否开启了支持 Butterknife
//     *
//     * @return true：支持，false：不支持
//     */
//    boolean isButterknifeOnClickEnabled();

    /**
     * 是否开启自动追踪 Fragment 的 $AppViewScreen 事件
     * 默认不开启
     */
    void trackFragmentAppViewScreen();

    boolean isTrackFragmentAppViewScreenEnabled();

    /**
     * 开启 AutoTrack 支持 React Native
     */
    void enableReactNativeAutoTrack();

    boolean isReactNativeAutoTrackEnabled();

    /**
     * 向WebView注入本地方法, 将distinctId传递给当前的WebView
     *
     * @param webView 当前WebView
     * @param isSupportJellyBean 是否支持API level 16及以下的版本。
     * 因为API level 16及以下的版本, addJavascriptInterface有安全漏洞,请谨慎使用
     */
    @SuppressLint(value = {"SetJavaScriptEnabled", "addJavascriptInterface"})
    void showUpWebView(WebView webView, boolean isSupportJellyBean);

    @SuppressLint(value = {"SetJavaScriptEnabled", "addJavascriptInterface"})
    void showUpWebView(WebView webView, boolean isSupportJellyBean, boolean enableVerify);

    @SuppressLint(value = {"SetJavaScriptEnabled", "addJavascriptInterface"})
    void showUpWebView(WebView webView, JSONObject properties, boolean isSupportJellyBean, boolean enableVerify);

    /**
     * 向WebView注入本地方法, 将distinctId传递给当前的WebView
     *
     * @param webView 当前WebView
     * @param isSupportJellyBean 是否支持API level 16及以下的版本。
     * 因为API level 16及以下的版本, addJavascriptInterface有安全漏洞,请谨慎使用
     * @param properties 用户自定义属性
     */
    @SuppressLint(value = {"SetJavaScriptEnabled", "addJavascriptInterface"})
    void showUpWebView(WebView webView, boolean isSupportJellyBean, JSONObject properties);

    void showUpX5WebView(Object x5WebView, JSONObject properties, boolean isSupportJellyBean, boolean enableVerify);

    void showUpX5WebView(Object x5WebView, boolean enableVerify);

    void showUpX5WebView(Object x5WebView);

    /**
     * 指定哪些 activity 不被AutoTrack
     *
     * 指定activity的格式为：activity.getClass().getCanonicalName()
     *
     * @param activitiesList activity列表
     */
    void ignoreAutoTrackActivities(List<Class<?>> activitiesList);

    /**
     * 恢复不被 AutoTrack 的 activity
     *
     * @param activitiesList List
     */
    void resumeAutoTrackActivities(List<Class<?>> activitiesList);

    /**
     * 指定某个 activity 不被 AutoTrack
     *
     * @param activity Activity
     */
    void ignoreAutoTrackActivity(Class<?> activity);

    /**
     * 恢复不被 AutoTrack 的 activity
     *
     * @param activity Class
     */
    void resumeAutoTrackActivity(Class<?> activity);

    /**
     * 指定 fragment 被 AutoTrack 采集
     *
     * @param fragment Fragment
     */
    void enableAutoTrackFragment(Class<?> fragment);

    /**
     * 指定 fragments 被 AutoTrack 采集
     *
     * @param fragmentsList Fragment 集合
     */
    void enableAutoTrackFragments(List<Class<?>> fragmentsList);

    /**
     * 指定 fragment 被 AutoTrack 采集
     *
     * @param fragmentName String
     */
    void enableAutoTrackFragment(String fragmentName);

    /**
     * 判断 AutoTrack 时，某个 Activity 的 $AppViewScreen 是否被过滤
     * 如果过滤的话，会过滤掉 Activity 的 $AppViewScreen 事件
     *
     * @param activity Activity
     * @return Activity 是否被采集
     */
    boolean isActivityAutoTrackAppViewScreenIgnored(Class<?> activity);

    /**
     * 判断 AutoTrack 时，某个 Fragment 的 $AppViewScreen 是否被采集
     *
     * @param fragment Fragment
     * @return Fragment 是否被采集
     */
    boolean isFragmentAutoTrackAppViewScreen(Class<?> fragment);

    /**
     * 判断 AutoTrack 时，某个 Activity 的 $AppClick 是否被过滤
     * 如果过滤的话，会过滤掉 Activity 的 $AppClick 事件
     *
     * @param activity Activity
     * @return Activity 是否被过滤
     */
    boolean isActivityAutoTrackAppClickIgnored(Class<?> activity);

    /**
     * 过滤掉 AutoTrack 的某个事件类型
     *
     * @param autoTrackEventType AutoTrackEventType
     */
    @Deprecated
    void ignoreAutoTrackEventType(BetaDataAPI.AutoTrackEventType autoTrackEventType);

    /**
     * 过滤掉 AutoTrack 的某些事件类型
     *
     * @param eventTypeList AutoTrackEventType List
     */
    @Deprecated
    void ignoreAutoTrackEventType(List<BetaDataAPI.AutoTrackEventType> eventTypeList);

    /**
     * 判断 某个 AutoTrackEventType 是否被忽略
     *
     * @param eventType AutoTrackEventType
     * @return true 被忽略; false 没有被忽略
     */
    boolean isAutoTrackEventTypeIgnored(BetaDataAPI.AutoTrackEventType eventType);

    /**
     * 设置界面元素ID
     *
     * @param view 要设置的View
     * @param viewID String 给这个View的ID
     */
    void setViewID(View view, String viewID);

    /**
     * 设置界面元素ID
     *
     * @param view 要设置的View
     * @param viewID String 给这个View的ID
     */
    void setViewID(android.app.Dialog view, String viewID);

    /**
     * 设置界面元素ID
     *
     * @param view 要设置的View
     * @param viewID String 给这个View的ID
     */
    void setViewID(Object view, String viewID);

    /**
     * 设置 View 所属 Activity
     *
     * @param view 要设置的View
     * @param activity Activity View 所属 Activity
     */
    void setViewActivity(View view, Activity activity);

    /**
     * 设置 View 所属 Fragment 名称
     *
     * @param view 要设置的View
     * @param fragmentName String View 所属 Fragment 名称
     */
    void setViewFragmentName(View view, String fragmentName);

    /**
     * 忽略View
     *
     * @param view 要忽略的View
     */
    void ignoreView(View view);

    /**
     * 忽略View
     *
     * @param view View
     * @param ignore 是否忽略
     */
    void ignoreView(View view, boolean ignore);

    /**
     * 设置View属性
     *
     * @param view 要设置的View
     * @param properties 要设置的View的属性
     */
    void setViewProperties(View view, JSONObject properties);

    List<Class> getIgnoredViewTypeList();

    /**
     * 获取需要采集的 Fragment 集合
     *
     * @return Set
     */
    Set<Integer> getAutoTrackFragments();

    /**
     * 忽略某一类型的 View
     *
     * @param viewType Class
     */
    void ignoreViewType(Class viewType);

//    /**
//     * activity 是否开启了可视化全埋点
//     *
//     * @param activity activity 类的对象
//     * @return true 代表 activity 开启了可视化全埋点，false 代表 activity 关闭了可视化全埋点
//     */
//    boolean isVisualizedAutoTrackActivity(Class<?> activity);
//
//    /**
//     * 开启某个 activity 的可视化全埋点
//     *
//     * @param activity activity 类的对象
//     */
//    void addVisualizedAutoTrackActivity(Class<?> activity);
//
//    /**
//     * 开启多个 activity 的可视化全埋点
//     *
//     * @param activitiesList activity 类的对象集合
//     */
//    void addVisualizedAutoTrackActivities(List<Class<?>> activitiesList);
//
//    /**
//     * 是否开启可视化全埋点
//     *
//     * @return true 代表开启了可视化全埋点， false 代表关闭了可视化全埋点
//     */
//    boolean isVisualizedAutoTrackEnabled();
//
//    /**
//     * 是否开启可视化全埋点的提示框
//     *
//     * @param enable true 代表开启了可视化全埋点的提示框， false 代表关闭了可视化全埋点的提示框
//     */
//    void enableVisualizedAutoTrackConfirmDialog(boolean enable);
//
//    /**
//     * 开启可视化全埋点功能
//     */
//    void enableVisualizedAutoTrack();
//
//    /**
//     * activity 是否开启了点击图
//     *
//     * @param activity activity 类的对象
//     * @return true 代表 activity 开启了点击图， false 代表 activity 关闭了点击图
//     */
//    boolean isHeatMapActivity(Class<?> activity);
//
//    /**
//     * 开启某个 activity 的点击图
//     *
//     * @param activity activity 类的对象
//     */
//    void addHeatMapActivity(Class<?> activity);
//
//    /**
//     * 开启多个 activity 的点击图
//     *
//     * @param activitiesList activity 类的对象集合
//     */
//    void addHeatMapActivities(List<Class<?>> activitiesList);
//
//    /**
//     * 是否开启点击图
//     *
//     * @return true 代表开启了点击图，false 代表关闭了点击图
//     */
//    boolean isHeatMapEnabled();
//
//    /**
//     * 是否开启点击图的提示框
//     *
//     * @param enable true 代表开启， false 代表关闭
//     */
//    void enableAppHeatMapConfirmDialog(boolean enable);
//
//    /**
//     * 开启点击图，$AppClick 事件将会采集控件的 viewPath
//     */
//    void enableHeatMap();

    /**
     * 获取当前用户的distinctId
     *
     * 若调用前未调用 {@link #identify(String)} 设置用户的 distinctId，SDK 会调用 {@link java.util.UUID} 随机生成
     * UUID，作为用户的 distinctId
     *
     * 该方法已不推荐使用，请参考 {@link #getAnonymousId()}
     *
     * @return 当前用户的distinctId
     */
    @Deprecated
    String getDistinctId();

    /**
     * 获取当前用户的匿名id
     *
     * 若调用前未调用 {@link #identify(String)} 设置用户的匿名id，SDK 会调用 {@link java.util.UUID} 随机生成
     * UUID，作为用户的匿名id
     *
     * @return 当前用户的匿名id
     */
    String getAnonymousId();

    /**
     * 重置默认匿名id
     */
    void resetAnonymousId();

    /**
     * 获取当前用户的 loginId
     *
     * 若调用前未调用 {@link #login(String)} 设置用户的 loginId，会返回null
     *
     * @return 当前用户的 loginId
     */
    String getLoginId();

    /**
     * 设置当前用户的distinctId。一般情况下，如果是一个注册用户，则应该使用注册系统内
     * 的user_id，如果是个未注册用户，则可以选择一个不会重复的匿名ID，如设备ID等，如果
     * 客户没有调用identify，则使用SDK自动生成的匿名ID
     *
     * @param distinctId 当前用户的distinctId，仅接受数字、下划线和大小写字母
     */
    void identify(String distinctId);

    /**
     * 登录，设置当前用户的 loginId
     *
     * @param loginId 当前用户的 loginId，不能为空，且长度不能大于255
     */
    void login(String loginId);

    /**
     * 登录，设置当前用户的 loginId
     *
     * @param loginId 当前用户的 loginId，不能为空，且长度不能大于255
     * @param properties 用户登录属性
     */
    void login(final String loginId, final JSONObject properties);

    /**
     * 注销，清空当前用户的 loginId
     */
    void logout();

    /**
     * 记录第一次登录行为
     *
     * 这个接口是一个较为复杂的功能，请在使用前先阅读相关说明:
     * http://www.sensorsdata.cn/manual/track_signup.html
     * 并在必要时联系我们的技术支持人员。
     * 该方法已不推荐使用，可以具体参考 {@link #login(String)} 方法
     *
     * @param newDistinctId 用户完成注册后生成的注册ID
     * @param properties 事件的属性
     */
    @Deprecated
    void trackSignUp(String newDistinctId, JSONObject properties);

    /**
     * 与 {@link #trackSignUp(String, JSONObject)} 类似，无事件属性
     *
     * 这个接口是一个较为复杂的功能，请在使用前先阅读相关说明:
     * http://www.sensorsdata.cn/manual/track_signup.html，
     * 并在必要时联系我们的技术支持人员。
     * 该方法已不推荐使用，可以具体参考 {@link #login(String)} 方法
     *
     * @param newDistinctId 用户完成注册后生成的注册ID
     */
    @Deprecated
    void trackSignUp(String newDistinctId);

    /**
     * 用于在 App 首次启动时追踪渠道来源，并设置追踪渠道事件的属性。
     *
     * 这是 Sensors Analytics 进阶功能，请参考文档 https://sensorsdata.cn/manual/track_installation.html
     *
     * @param eventName 渠道追踪事件的名称
     * @param properties 渠道追踪事件的属性
     * @param disableCallback 是否关闭这次渠道匹配的回调请求
     */
    void trackInstallation(String eventName, JSONObject properties, boolean disableCallback);

    /**
     * 用于在 App 首次启动时追踪渠道来源，并设置追踪渠道事件的属性。
     *
     * 这是 Sensors Analytics 进阶功能，请参考文档 https://sensorsdata.cn/manual/track_installation.html
     *
     * @param eventName 渠道追踪事件的名称
     * @param properties 渠道追踪事件的属性
     */
    void trackInstallation(String eventName, JSONObject properties);

    /**
     * 用于在 App 首次启动时追踪渠道来源，并设置追踪渠道事件的属性。
     *
     * 这是 Sensors Analytics 进阶功能，请参考文档 https://sensorsdata.cn/manual/track_installation.html
     *
     * @param eventName 渠道追踪事件的名称
     */
    void trackInstallation(String eventName);

    /**
     * 调用track接口，追踪一个带有属性的事件
     *
     * @param eventName 事件的名称
     * @param properties 事件的属性
     */
    void track(String eventName, JSONObject properties);

    /**
     * 与 {@link #track(String, JSONObject)} 类似，无事件属性
     *
     * @param eventName 事件的名称
     */
    void track(String eventName);

    /**
     * 初始化事件的计时器，默认计时单位为毫秒。
     *
     * 详细用法请参考 trackTimer(String, TimeUnit)
     *
     * @param eventName 事件的名称
     */
    @Deprecated
    void trackTimer(final String eventName);

    /**
     * 初始化事件的计时器。
     *
     * 若需要统计某个事件的持续时间，先在事件开始时调用 trackTimer("Event") 记录事件开始时间，该方法并不会真正发
     * 送事件；随后在事件结束时，调用 track("Event", properties)，SDK 会追踪 "Event" 事件，并自动将事件持续时
     * 间记录在事件属性 "event_duration" 中。
     *
     * 多次调用 trackTimer("Event") 时，事件 "Event" 的开始时间以最后一次调用时为准。
     *
     * @param eventName 事件的名称
     * @param timeUnit 计时结果的时间单位
     */
    @Deprecated
    void trackTimer(final String eventName, final TimeUnit timeUnit);

    /**
     * 初始化事件的计时器。
     *
     * 若需要统计某个事件的持续时间，先在事件开始时调用 trackTimer("Event") 记录事件开始时间，该方法并不会真正发
     * 送事件；随后在事件结束时，调用 track("Event", properties)，SDK 会追踪 "Event" 事件，并自动将事件持续时
     * 间记录在事件属性 "event_duration" 中。
     *
     * 多次调用 trackTimer("Event") 时，事件 "Event" 的开始时间以最后一次调用时为准。
     *
     * @param eventName 事件的名称
     * @param eventTimer 事件的自定义 EventTimer
     */
    @Deprecated
    void trackTimer(final String eventName, final EventTimer eventTimer);

    /**
     * 删除事件的计时器
     *
     * @param eventName 事件名称
     */
    void removeTimer(final String eventName);

    /**
     * 初始化事件的计时器，默认计时单位为毫秒。
     *
     * 详细用法请参考 trackTimerBegin(String, TimeUnit)
     *
     * @param eventName 事件的名称
     */
    void trackTimerBegin(final String eventName);

    /**
     * 初始化事件的计时器。
     *
     * 若需要统计某个事件的持续时间，先在事件开始时调用 trackTimerBegin("Event") 记录事件开始时间，该方法并不会真正发
     * 送事件；随后在事件结束时，调用 track("Event", properties)，SDK 会追踪 "Event" 事件，并自动将事件持续时
     * 间记录在事件属性 "event_duration" 中。
     *
     * 多次调用 trackTimerBegin("Event") 时，事件 "Event" 的开始时间以最后一次调用时为准。
     *
     * @param eventName 事件的名称
     * @param timeUnit 计时结果的时间单位
     */
    void trackTimerBegin(final String eventName, final TimeUnit timeUnit);

    /**
     * 停止事件计时器
     *
     * @param eventName 事件的名称
     * @param properties 事件的属性
     */
    void trackTimerEnd(final String eventName, JSONObject properties);

    /**
     * 停止事件计时器
     *
     * @param eventName 事件的名称
     */
    void trackTimerEnd(final String eventName);

    /**
     * 清除所有事件计时器
     */
    void clearTrackTimer();

    /**
     * 获取LastScreenUrl
     *
     * @return String
     */
    String getLastScreenUrl();

    /**
     * App 退出或进到后台时清空 referrer，默认情况下不清空
     */
    void clearReferrerWhenAppEnd();

    void clearLastScreenUrl();

    String getMainProcessName();

    /**
     * 获取LastScreenTrackProperties
     *
     * @return JSONObject
     */
    JSONObject getLastScreenTrackProperties();

    /**
     * Track 进入页面事件 ($AppViewScreen)
     *
     * @param url String
     * @param properties JSONObject
     */
    void trackViewScreen(String url, JSONObject properties);

    /**
     * Track Activity 进入页面事件($AppViewScreen)
     *
     * @param activity activity Activity，当前 Activity
     */
    void trackViewScreen(Activity activity);

    void trackViewScreen(Object fragment);

    /**
     * 将所有本地缓存的日志发送到 Sensors Analytics.
     */
    void flush();

    /**
     * 以阻塞形式将所有本地缓存的日志发送到 Sensors Analytics，该方法不能在 UI 线程调用。
     */
    void flushSync();

    /**
     * 注册事件动态公共属性
     *
     * @param dynamicSuperProperties 事件动态公共属性回调接口
     */
    void registerDynamicSuperProperties(BetaDataDynamicSuperProperties dynamicSuperProperties);

    /**
     * 设置 track 事件回调
     *
     * @param trackEventCallBack track 事件回调接口
     */
    void setTrackEventCallBack(BetaDataTrackEventCallBack trackEventCallBack);

    /**
     * 获取事件公共属性
     *
     * @return 当前所有Super属性
     */
    JSONObject getSuperProperties();

    /**
     * 注册所有事件都有的公共属性
     *
     * @param superProperties 事件公共属性
     */
    void registerSuperProperties(JSONObject superProperties);

    /**
     * 删除事件公共属性
     *
     * @param superPropertyName 事件属性名称
     */
    void unregisterSuperProperty(String superPropertyName);

    /**
     * 删除所有事件公共属性
     */
    void clearSuperProperties();

    /**
     * 设置用户的一个或多个Profile。
     * Profile如果存在，则覆盖；否则，新创建。
     *
     * @param properties 属性列表
     */
    void profileSet(JSONObject properties);
    /**
     * 设置用户的一个或多个Profile。
     * Profile如果存在，则覆盖；否则，新创建。
     * UserProperties 用户
     * @param properties 属性列表
     */
    void profileSet(JSONObject properties, JSONObject userProperties);
    /**
     * 设置用户的一个Profile，如果之前存在，则覆盖，否则，新创建
     *
     * @param property 属性名称
     * @param value 属性的值，值的类型只允许为
     * {@link String}, {@link Number}, {@link java.util.Date}, {@link List}
     */
    void profileSet(String property, Object value);

    /**
     * 首次设置用户的一个或多个Profile。
     * 与profileSet接口不同的是，如果之前存在，则忽略，否则，新创建
     *
     * @param properties 属性列表
     */
    void profileSetOnce(JSONObject properties);

    /**
     * 首次设置用户的一个Profile
     * 与profileSet接口不同的是，如果之前存在，则忽略，否则，新创建
     *
     * @param property 属性名称
     * @param value 属性的值，值的类型只允许为
     * {@link String}, {@link Number}, {@link java.util.Date}, {@link List}
     */
    void profileSetOnce(String property, Object value);

    /**
     * 给一个或多个数值类型的Profile增加一个数值。只能对数值型属性进行操作，若该属性
     * 未设置，则添加属性并设置默认值为0
     *
     * @param properties 一个或多个属性集合
     */
    void profileIncrement(Map<String, ? extends Number> properties);

    /**
     * 给一个数值类型的Profile增加一个数值。只能对数值型属性进行操作，若该属性
     * 未设置，则添加属性并设置默认值为0
     *
     * @param property 属性名称
     * @param value 属性的值，值的类型只允许为 {@link Number}
     */
    void profileIncrement(String property, Number value);

    /**
     * 给一个列表类型的Profile增加一个元素
     *
     * @param property 属性名称
     * @param value 新增的元素
     */
    void profileAppend(String property, String value);

    /**
     * 给一个列表类型的Profile增加一个或多个元素
     *
     * @param property 属性名称
     * @param values 新增的元素集合
     */
    void profileAppend(String property, Set<String> values);

    /**
     * 删除用户的一个Profile
     *
     * @param property 属性名称
     */
    void profileUnset(String property);

    /**
     * 删除用户所有Profile
     */
    void profileDelete();

    void trackEventFromH5(String eventInfo, boolean enableVerify);

    void trackEventFromH5(String eventInfo);

    /**
     * 更新 GPS 位置信息
     *
     * @param latitude 纬度
     * @param longitude 经度
     */
    void setGPSLocation(double latitude, double longitude);

    /**
     * 清楚 GPS 位置信息
     */
    void clearGPSLocation();

    /**
     * 开启/关闭采集屏幕方向
     *
     * @param enable true：开启 false：关闭
     */
    void enableTrackScreenOrientation(boolean enable);

    /**
     * 恢复采集屏幕方向
     */
    void resumeTrackScreenOrientation();

    /**
     * 暂停采集屏幕方向
     */
    void stopTrackScreenOrientation();

    /**
     * 获取当前屏幕方向
     *
     * @return portrait:竖屏 landscape:横屏
     */
    String getScreenOrientation();

    /**
     * 初始化事件的计时器，计时单位为秒。
     *
     * @param eventName 事件的名称
     */
    void trackTimerStart(final String eventName);

    /**
     * 设置 Cookie，flush 的时候会设置 HTTP 的 cookie
     * 内部会 URLEncoder.encode(cookie, "UTF-8")
     *
     * @param cookie String cookie
     * @param encode boolean 是否 encode
     */
    void setCookie(final String cookie, boolean encode);

    /**
     * 获取已设置的 Cookie
     * URLDecoder.decode(Cookie, "UTF-8")
     *
     * @param decode String
     * @return String cookie
     */
    String getCookie(boolean decode);

    /**
     * 删除本地缓存的全部事件
     */
    void deleteAll();

//    /**
//     * 保存用户推送 ID 到用户表
//     *
//     * @param propertyKey 属性名称（例如 jgId）
//     * @param pushId 推送 ID
//     * 使用 profilePushId("jgId",JPushInterface.getRegistrationID(this))
//     */
//
//    void profilePushId(String propertyKey, String pushId);
}
