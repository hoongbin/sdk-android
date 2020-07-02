
package com.betadata.collect.common;

public class AopConstants {


    public static String App_ID = "";// app id
    public static String App_Secret = "";// app secret


    public static final String BETADATA_SDK_VERSION = "1.2.7";
    public static final String ELEMENT_TYPE = "_element_selector";
    public static final String ELEMENT_CONTENT = "_element_content";
    public static final String BETA_TITLE = "_title";


    //------------------------------事件类型  start----------------------------------------------
    public static final String BETA_USERS_PROPERTIES = "user_properties";// 基础事件
    public static final String BETA_EVENTS_PROPERTIES = "event_properties";// 用户事件
    //------------------------------事件类型  end------------------------------------------------


    //------------------------------事件 基础属性 start----------------------------------------------
    public static final String BETA_DEVICE_ID = "_device_id";// 用户设备ID  必填：否
    public static final String BETA_SECOND_ID = "_second_id";// 用户唯一ID  必填：否
    public static final String BETA_TIME = "_time";// 时间  必填：是
    public static final String BETA_LBS = "_lbs";// 用户设备定位  必填：否
    public static final String BETA_COUNTRY = "_country";// 国家  必填：否
    public static final String BETA_PROVINCE = "_province";// 省份  必填：否
    public static final String BETA_CITY = "_city";// 城市  必填：否
    public static final String BETA_HOUSING_ESTATE = "_housing_estate";// 详细地址  必填：否
    public static final String BETA_SDK = "_sdk";// SDK类型  必填：是
    public static final String BETA_SDK_VERSION = "_sdk_version";// SDK版本  必填：是
    public static final String BETA_APP_VERSION = "_app_version";// 应用的版本 必填：是
    public static final String BETA_MANUFACTURER = "_manufacturer";// 设备制造商  必填：否
    public static final String BETA_MODEL = "_model";// 设备型号  必填：否
    public static final String BETA_OS = "_os";// 操作系统  必填：否
    public static final String BETA_OS_VERSION = "_os_version";// 操作系统版本  必填：否
    public static final String BETA_WIFI = "_wifi";// 是否使用wifi  必填：否
    public static final String BETA_CARRIER = "_carrier";// 运营商名称  必填：否
    public static final String BETA_NETWORK_TYPE = "_network_type";// 网络类型  必填：否
    public static final String BETA_SCREEN_HEIGHT = "_screen_height";// 屏幕高度  必填：否
    public static final String BETA_SCREEN_WIDTH = "_screen_width";// 屏幕宽度  必填：否
    public static final String BETA_EVENT = "_event";// 事件名  必填：是
    public static final String BETA_CHANNEL = "_channel";// 渠道追踪匹配模式  必填：否
    public static final String BETA_URL = "_url";// 页面地址  必填：否
    public static final String BETA_URL_PATH = "_url_path";// 页面路径  必填：否
    public static final String BETA_EVENTS = "events";
    public static final String BETA_BEFORE_ID = "before_id";// 服务端返回
    public static final String BETA_SCREEN_NAME = "_screen_name";
    public static final String BETA_CRASH_REASON = "_crash_reason";// 崩溃详情
    public static final String BETA_EVENT_DURATION = "_event_duration";// 统计时长
    public static final String BETA_LAST_TIME = "_last_time";// 上次登录时间
    public static final String BETA_IS_FIRST = "_is_first";// 是否第一次注册
    public static final String BETA_IMEI = "imei";// 获取手机imei
    public static final String BETA_OAID = "oaid";// 获取手机oaid
    public static final String BETA_ANDROID_ID = "android_id";// 获取手机Androidid
    //------------------------------事件 基础属性 end----------------------------------------------

    //------------------------------事件 基础属性外层属性 start----------------------------------------------
    public static final String DEVICE_ID = "device_id";// 用户设备ID
    public static final String TIME = "time";// 时间
    public static final String SDK_VERSION = "sdk_version";
    public static final String SDK = "sdk";
    public static final String APP_VERSION = "app_version";
    public static final String USER_AGENT = "user_agent";
    public static final String COOKIE = "Cookie";
    public static final String APPID = "app_id";
    public static final String DATA = "data";
    public static final String TIMESTAMP = "timestamp";
    public static final String SIGN = "sign";
    public static final String PROJECT = "project";
    //------------------------------事件 基础属性外层属性 end----------------------------------------------


    //------------------------------事件event  start----------------------------------------------
    public static final String BETA_APP_INSTALL = "_app_install";// 安装APP
    public static final String BETA_APP_START = "_app_start";// 开启APP
    public static final String BETA_APP_END = "_app_end";// 退出APP
    public static final String BETA_APP_PAGEVIEW = "_app_pageview";// 开启页面
    public static final String BETA_APP_CLICK = "_app_click";// 点击事件
    public static final String BETA_SIGN_UP = "_sign_up";
    public static final String BETA_APP_CRASH = "_app_crash";// 崩溃事件
    public static final String BETA_APP_LOGIN = "_app_login";// 登录事件
    public static final String BETA_APP_PROFILE = "_app_profile";// 更新用户属性
    public static final String BETA_APP_REGISTER = "_app_register";// 注册
    public static final String BETA_APP_PAYMENT = "_app_payment";// 会员购买买付款请求
    //------------------------------事件event  end----------------------------------------------


    //---------------------------------存储before_id的sharePre-----------------------------
    public final static String SP_BEFORE_ID_KEY = "sp_before_id_key";
    //------------------------------常量  注册方式----------------------------------------------
    public final static long REGISTER_METHOD_UNKNOWN = 0;// 未知
    public final static long REGISTER_METHOD_PHONE = 1;// 手机号
    public final static long REGISTER_METHOD_MAIL = 2;// 邮箱
    public final static long REGISTER_METHOD_WX = 3;// 微信
    public final static long REGISTER_METHOD_QQ = 4;// QQ
    public final static long REGISTER_METHOD_SINA = 5;// 微博

    //------------------------------常量  支付渠道----------------------------------------------
    public final static long PAYMENT_CHANNEL_UNKNOWN = 0;// 未知
    public final static long PAYMENT_CHANNEL_ALIPAY = 1;// 支付宝
    public final static long PAYMENT_CHANNEL_WECHATPAY = 2;// 微信
    public final static long PAYMENT_CHANNEL_DIAMOND = 4;// 钻石支付


    //------------------------------支付 购买类型----------------------------------------------
    public final static long BUY_DIAMOND = 1;// 1 购买钻石
    public final static long BUY_VIP = 2;// 2 购买会员
    //------------------------------分享 平台类型----------------------------------------------
    public final static int SHARE_UNKNOWN = 0;// 未知
    public final static int SHARE_QQ = 1;// QQ好友
    public final static int SHARE_QQ_ZONE  = 2;// QQ空间
    public final static int WEIXIN  = 3;// 微信好友
    public final static int SHARE_WEIXIN_CIRCLE  = 4;// 微信朋友圈
    public final static int SHARE_SINA = 5;// 微博


}
