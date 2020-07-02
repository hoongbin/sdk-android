
package com.betadata.collect;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.inputmethod.InputMethodManager;

import com.betadata.collect.bean.SendReturnMessageBean;
import com.betadata.collect.common.AopConstants;
import com.betadata.collect.common.BetaDataConstant;
import com.betadata.collect.data.DbAdapter;
import com.betadata.collect.data.DbParams;
import com.betadata.collect.exceptions.ConnectErrorException;
import com.betadata.collect.exceptions.DebugModeException;
import com.betadata.collect.exceptions.InvalidDataException;
import com.betadata.collect.exceptions.ResponseErrorException;
import com.betadata.collect.util.Base64Coder;
import com.betadata.collect.util.BetaDataUtils;
import com.betadata.collect.util.HmacSha256;
import com.betadata.collect.util.JSONUtils;
import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.zip.GZIPOutputStream;


/**
 * Author: 李巷阳
 * Date: 2019/8/3
 * Version: V2.1.0
 * Part:管理与内部数据库和传感器数据服务器之间的事件通信。
 * 该类跨越用户线程和逻辑传感器数据线程之间的线程边界
 * Description:
 */
class AnalyticsMessages {


    private long currentTimestamp = 0;// 发送时间戳
    private String sign = "";// 验签sign
    private final SharedPreferences sp;

    private final Worker mWorker;
    private final Context mContext;
    private final DbAdapter mDbAdapter;
    private List<JSONObject> mEventsList = new CopyOnWriteArrayList<>();
    private static int mFlushSize;// 内存中缓存数量，默认是5.
    private static final int SEND_QUEUE = 1001;// 发送数据
    private static final int DELETE_ALL = 1002;// 删除数据
    private static final String CHARSET_UTF8 = "UTF-8";    /* 指定默认编码 */
    private static final String TAG = "BT.AnalyticsMessages";
    private static String mbefore_id = null;


    private static final Map<Context, AnalyticsMessages> sInstances = new HashMap<>();

    /**
     * 应该调用AnalyticsMessages.getInstance()
     */
    AnalyticsMessages(final Context context) {
        mContext = context;
        mDbAdapter = DbAdapter.getInstance();
        mWorker = new Worker();
        sp = BetaDataUtils.getSharedPreferences(mContext);
    }

    /**
     * 管理与内部数据库和传感器数据服务器之间的事件通信
     *
     * @param messageContext 上下文
     * @param flushCacheSize 内存中缓存数量，默认是5.
     * @param appid
     * @param token
     * @return
     */
    public static AnalyticsMessages getInstance(final Context messageContext, int flushCacheSize, String appid, String token) {
        synchronized (sInstances) {
            AopConstants.App_ID = appid;
            AopConstants.App_Secret = token;
            final Context appContext = messageContext.getApplicationContext();
            mFlushSize = flushCacheSize;
            final AnalyticsMessages ret;
            if (!sInstances.containsKey(appContext)) {
                ret = new AnalyticsMessages(appContext);
                sInstances.put(appContext, ret);
            } else {
                ret = sInstances.get(appContext);
            }
            return ret;
        }
    }

    public void enqueueEventMessage(final String eventName, final JSONObject eventJson) {
        try {
            // 同步锁
            synchronized (mDbAdapter) {
                TrackTaskManager.getInstance().addEventDBTask(new Runnable() {
                    @Override
                    public void run() {
                        int ret;
//                        boolean isDebugMode = BetaDataAPI.sharedInstance( mContext ).isDebugMode();
                        // 如果如果是debug 或是注册  或者安装事件  则添加到本地数据库
                        if (AopConstants.BETA_APP_REGISTER.equals(eventName)
                                || AopConstants.BETA_APP_CRASH.equals(eventName)
                                || AopConstants.BETA_APP_LOGIN.equals(eventName)
                                || AopConstants.BETA_APP_INSTALL.equals(eventName)) {
                            ret = mDbAdapter.addJSON(eventJson);
                        } else {
                            // 添加至事件内存集合中。
                            mEventsList.add(eventJson);
                            // 当前缓存集合数量小于 设置的内存缓存数量  并且可见 则return。
                            boolean mAppResumeStart = mDbAdapter.getAppResumeStart();

                            if (mEventsList.size() < mFlushSize && mAppResumeStart)
                                return;
                            // 把内存事件集合添加到本地数据库
                            // 返回表中行数，如果大于0表示添加成功，清除缓存
                            ret = mDbAdapter.addJSON(mEventsList);
                            if (ret >= 0) {
                                mEventsList.clear();
                            }
                        }
                        // 如果小于0 ，就是有bug
                        if (ret < 0) {
                            String error = "Failed to enqueue the event: " + eventJson;

                            BetaDataLog.i(TAG, error);
                        }

                        final Message m = Message.obtain();
                        // 发送消息
                        m.what = SEND_QUEUE;
                        // 如果是debug 或者 是数据库数量超过上限  直接发送
                        if (ret == DbParams.DB_OUT_OF_MEMORY_ERROR) {
                            mWorker.runMessage(m);
                        } else {
                            // 如果是注册 或者安装事件   行数大于本地配置缓存日志的最大条目数 则发送
                            int mFlushBulkSize = BetaDataAPI.sharedInstance(mContext).getFlushBulkSize();
                            if (AopConstants.BETA_APP_REGISTER.equals(eventName)
                                    || AopConstants.BETA_APP_CRASH.equals(eventName)
                                    || AopConstants.BETA_APP_LOGIN.equals(eventName)
                                    || AopConstants.BETA_APP_INSTALL.equals(eventName) || ret > mFlushBulkSize) {
                                mWorker.runMessage(m);
                            } else {
                                // 获取设置两次数据发送的最小时间间隔，开启定时器，进行发送。
                                int mFlushInterval = BetaDataAPI.sharedInstance(mContext).getFlushInterval();
                                mWorker.runMessageOnce(m, mFlushInterval);
                            }
                        }
                    }
                });
            }
        } catch (Exception e) {
            BetaDataLog.i(TAG, "enqueueEventMessage error:" + e);
        }
    }

    public void flush() {
        final Message m = Message.obtain();
        m.what = SEND_QUEUE;

        mWorker.runMessage(m);
    }

    public void flush(long timeDelayMills) {
        final Message m = Message.obtain();
        m.what = SEND_QUEUE;

        mWorker.runMessageOnce(m, timeDelayMills);
    }

    /**
     * 刷新数据同步
     */
    public void flushDataSync() {
        try {
            if (mEventsList.size() > 0) {
                if (mDbAdapter.addJSON(mEventsList) >= 0) {
                    mEventsList.clear();

                }
            }
        } catch (Exception e) {
            BetaDataLog.printStackTrace(e);
        }
    }

    public void deleteAll() {
        final Message m = Message.obtain();
        m.what = DELETE_ALL;

        mWorker.runMessage(m);
    }

    public static byte[] slurp(final InputStream inputStream)
            throws IOException {
        final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        int nRead;
        byte[] data = new byte[8192];

        while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }

        buffer.flush();
        return buffer.toByteArray();
    }

    /**
     * 发送数据
     */
    public void sendData() {
        try {
            // 进入后台是否上传数据
            if (!BetaDataAPI.sharedInstance(mContext).isFlushInBackground()) {
                // 如果app不可见状态 ,则直接return.
                if (!mDbAdapter.getAppResumeStart()) {
                    return;
                }
            }
            // 判断serverUrl是否为空
            if (TextUtils.isEmpty(BetaDataAPI.sharedInstance(mContext).getServerUrl())) {
                return;
            }
            // 不是主进程
            if (!BetaDataAPI.mIsMainProcess) {
                return;
            }

            // 无网络
            if (!BetaDataUtils.isNetworkAvailable(mContext)) {
                return;
            }

            // 是否符合需要的网络策略
            String networkType = BetaDataUtils.networkType(mContext);
            if (!BetaDataAPI.sharedInstance(mContext).isShouldFlush(networkType)) {
                return;
            }
        } catch (Exception e) {
            BetaDataLog.printStackTrace(e);
        }
        int count = 100;// 如果已经删除,则count为0
        while (count > 0) {
            boolean deleteEvents = true;
            String[] eventsData;
            synchronized (mDbAdapter) {
                // 判断如果是debug,则获取第一条eventsData
                // 否则获取前50条
//                if (BetaDataAPI.sharedInstance( mContext ).isDebugMode()) {
                // 默认是每次取数据库前50条数据上传至服务端
                if (false) {
                    eventsData = mDbAdapter.generateDataString(DbParams.TABLE_EVENTS, 1);
                } else {
                    eventsData = mDbAdapter.generateDataString(DbParams.TABLE_EVENTS, 50);
                }
            }
            // 如果取出为null 则return
            if (eventsData == null) {
                return;
            }

            final String lastId = eventsData[0];// 上一次的数据ID
            String rawMessage = eventsData[1];// 50条json
            String errorMessage = null;
            // 拼装数据
            List<String> oblist = BetaDataUtils.getJsonStrToList(rawMessage);

            Map<String, Object> map = new HashMap<>();
            map.put(AopConstants.BETA_EVENTS, oblist);
            map.put(AopConstants.DEVICE_ID, BetaDataUtils.getAndroidID(mContext));
            map.put(AopConstants.BETA_BEFORE_ID, mbefore_id);
            map.put(AopConstants.TIME, BetaDataUtils.getCurrenTimestampMillis(mContext));


            // 初始化sdk属性
            JSONObject sdkProperties = new JSONObject();
            try {
                sdkProperties.put(AopConstants.SDK_VERSION, AopConstants.BETADATA_SDK_VERSION);
                sdkProperties.put(AopConstants.SDK, "Android");
                sdkProperties.put(AopConstants.APP_VERSION, BetaDataUtils.getVersionName(mContext));
                map.put(AopConstants.SDK, sdkProperties);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            JSONObject powerCurveJsonObj = new JSONObject(map);
            rawMessage = powerCurveJsonObj.toString();

            try {
                String data;
                try {
                    // 进行数据转码
                    data = encodeData(rawMessage);
                } catch (IOException e) {
                    // 格式错误，直接将数据删除
                    mDbAdapter.cleanupEvents(lastId);
                    throw new InvalidDataException(e);
                }
                currentTimestamp = BetaDataUtils.getCurrenTimestampMillis(mContext);
                sign = HmacSha256.sha256_HMAC((AopConstants.App_ID + data + currentTimestamp), AopConstants.App_Secret);
                sendHttpRequest(BetaDataAPI.sharedInstance(mContext).getServerUrl(), data, rawMessage, false);// 发送消息
            } catch (ConnectErrorException e) {
                deleteEvents = false;
                errorMessage = "Connection error: " + e.getMessage();
            } catch (InvalidDataException e) {
                deleteEvents = true;
                errorMessage = "Invalid data: " + e.getMessage();
            } catch (ResponseErrorException e) {
                deleteEvents = true;
                errorMessage = "ResponseErrorException: " + e.getMessage();
            } catch (Exception e) {
                deleteEvents = false;
                errorMessage = "Exception: " + e.getMessage();
            } finally {
                if (deleteEvents) {
                    count = mDbAdapter.cleanupEvents(lastId);
                    BetaDataLog.i(TAG, String.format(Locale.CHINA, "Events flushed. [left = %d]", count));
                } else {
                    count = 0;
                }

            }
        }
    }

    /**
     * 发送数据
     *
     * @param path
     * @param data
     * @param rawMessage
     * @param isRedirects
     * @throws ConnectErrorException
     * @throws ResponseErrorException
     * @throws JSONException
     */
    private void sendHttpRequest(String path, String data, String rawMessage, boolean isRedirects) throws ConnectErrorException, ResponseErrorException, JSONException {
        BetaDataLog.e(BetaDataConstant.BT_TAG_SEND_MESSAGE, "2.准备发送数据:" + rawMessage);
        BetaDataLog.e(BetaDataConstant.BT_TAG_SEND_MESSAGE, "3.准备发送数据加密:" + data);


        HttpURLConnection connection = null;
        InputStream in = null;
        OutputStream out = null;
        BufferedOutputStream bout = null;
        try {
            final URL url = new URL(path);
            connection = (HttpURLConnection) url.openConnection();
            connection.setInstanceFollowRedirects(false);
            try {
                String ua = BetaDataUtils.getUserAgent(mContext);
                if (TextUtils.isEmpty(ua)) {
                    ua = "Android";
                }
                connection.addRequestProperty(AopConstants.USER_AGENT, "Android " + ua);// 设置userAgent
            } catch (Exception e) {
                BetaDataLog.printStackTrace(e);
            }

            connection.setRequestProperty(AopConstants.COOKIE, BetaDataAPI.sharedInstance(mContext).getCookie(false));
            Uri.Builder builder = new Uri.Builder();

            builder.appendQueryParameter(AopConstants.APPID, AopConstants.App_ID);
            builder.appendQueryParameter(AopConstants.DATA, data);
            builder.appendQueryParameter(AopConstants.TIMESTAMP, currentTimestamp + "");
            builder.appendQueryParameter(AopConstants.SIGN, sign);
            builder.appendQueryParameter(AopConstants.PROJECT, "moego");


            String query = builder.build().getEncodedQuery();


            connection.setFixedLengthStreamingMode(query.getBytes().length);
            connection.setDoOutput(true);
            connection.setRequestMethod("POST");
            out = connection.getOutputStream();
            bout = new BufferedOutputStream(out);


            bout.write(query.getBytes());
            bout.flush();
            bout.close();

            int responseCode = connection.getResponseCode();
            BetaDataLog.i(TAG, "responseCode: " + responseCode);
            if (!isRedirects && BetaDataHttpURLConnectionHelper.needRedirects(responseCode)) {
                String location = BetaDataHttpURLConnectionHelper.getLocation(connection, path);
                if (!TextUtils.isEmpty(location)) {
                    closeStream(bout, out, null, connection);
                    sendHttpRequest(location, data, rawMessage, true);
                    return;
                }
            }
            try {
                in = connection.getInputStream();
            } catch (FileNotFoundException e) {
                in = connection.getErrorStream();
            }
            byte[] responseBody = slurp(in);
            in.close();
            in = null;
            String response = new String(responseBody, "UTF-8");
            if (responseCode == HttpURLConnection.HTTP_OK) {
                Gson gson = new Gson();
                SendReturnMessageBean mSendReturnBean = gson.fromJson(response, SendReturnMessageBean.class);
                mbefore_id = mSendReturnBean.getData().getTrack_id();
                BetaDataLog.e(BetaDataConstant.BT_TAG_SEND_MESSAGE, String.format("4.数据上报成功: \n%s", mSendReturnBean.toString()));

            } else {

                BetaDataLog.e(BetaDataConstant.BT_TAG_SEND_MESSAGE, String.format("5.数据上报失败: \n%s", JSONUtils.formatJson(rawMessage)));
            }
            if (responseCode < HttpURLConnection.HTTP_OK || responseCode >= HttpURLConnection.HTTP_MULT_CHOICE) {
                // 校验错误，直接将数据删除
                throw new ResponseErrorException(String.format("flush failure with response '%s'", response));
            }
        } catch (IOException e) {
            BetaDataLog.e(BetaDataConstant.BT_TAG_SEND_MESSAGE, String.format("6.数据上报失败"));
            throw new ConnectErrorException(e);
        } finally {
            closeStream(bout, out, in, connection);
        }
    }

    private void closeStream(BufferedOutputStream bout, OutputStream out, InputStream in, HttpURLConnection connection) {
        if (null != bout) {
            try {
                bout.close();
            } catch (Exception e) {
                BetaDataLog.i(TAG, e.getMessage());
            }
        }

        if (null != out) {
            try {
                out.close();
            } catch (Exception e) {
                BetaDataLog.i(TAG, e.getMessage());
            }
        }

        if (null != in) {
            try {
                in.close();
            } catch (Exception e) {
                BetaDataLog.i(TAG, e.getMessage());
            }
        }

        if (null != connection) {
            try {
                connection.disconnect();
            } catch (Exception e) {
                BetaDataLog.i(TAG, e.getMessage());
            }
        }
    }

    private String encodeData(final String rawMessage) throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream(rawMessage.getBytes(CHARSET_UTF8).length);
        GZIPOutputStream gos = new GZIPOutputStream(os);
        gos.write(rawMessage.getBytes(CHARSET_UTF8));
        gos.close();
        byte[] compressed = os.toByteArray();
        os.close();
        return new String(Base64Coder.encode(compressed));
    }

    //  Worker将管理(最多一个)与之关联的IO线程
    //  这个分析消息实例。
    //  XXX: Worker类是不必要的，应该只是HandlerThread的子类
    private class Worker {

        public Worker() {
            final HandlerThread thread = new HandlerThread("com.betadata.analytics.android.sdk.AnalyticsMessages.Worker", Thread.MIN_PRIORITY);
            thread.start();
            mHandler = new AnalyticsMessageHandler(thread.getLooper());
        }

        public void runMessage(Message msg) {
            synchronized (mHandlerLock) {
                if (mHandler == null) {
                    // We died under suspicious circumstances. Don't try to send any more events.
                    BetaDataLog.i(TAG, "Dead worker dropping a message: " + msg.what);
                } else {
                    mHandler.sendMessage(msg);
                }
            }
        }

        public void runMessageOnce(Message msg, long delay) {
            synchronized (mHandlerLock) {
                if (mHandler == null) {
                    // We died under suspicious circumstances. Don't try to send any more events.
                    BetaDataLog.e(TAG, "Dead worker dropping a message: " + msg.what);
                } else {
                    if (!mHandler.hasMessages(msg.what)) {
                        mHandler.sendMessageDelayed(msg, delay);

                    }
                }
            }
        }

        private class AnalyticsMessageHandler extends Handler {

            public AnalyticsMessageHandler(Looper looper) {
                super(looper);
            }

            @Override
            public void handleMessage(Message msg) {
                try {
                    // 发送数据
                    if (msg.what == SEND_QUEUE) {
                        sendData();
                    }
                    // 删除本地数据库数据
                    else if (msg.what == DELETE_ALL) {
                        if(mDbAdapter!=null)
                        {
                            try {
                                String[] eventsData = mDbAdapter.generateDataString(DbParams.TABLE_EVENTS,500);
                                if(eventsData!=null&&eventsData.length>0)
                                {
                                    final String lastId = eventsData[0];// 上一次的数据ID
                                    // 格式错误，直接将数据删除
                                    int count = mDbAdapter.cleanupEvents(lastId);
                                }else{
                                    Log.e("delectbetadata", "eventsData1=null");
                                }
                            } catch (Exception e) {
                                BetaDataLog.printStackTrace(e);
                            }
                        }
                    } else {
                        BetaDataLog.i(TAG, "Unexpected message received by BetaData worker: " + msg);
                    }
                } catch (final RuntimeException e) {
                    BetaDataLog.i(TAG, "Worker threw an unhandled exception", e);
                }
            }
        }

        private final Object mHandlerLock = new Object();
        private Handler mHandler;
    }


}
