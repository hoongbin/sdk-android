package com.example.betadata_sdk_android;

import android.app.Application;

import com.betadata.collect.BetaDataAPI;

import java.util.ArrayList;
import java.util.List;

/**
 * Author: 李巷阳
 * Date: 2020/7/2
 * Part:
 * Description:
 */
public class MyApplication extends Application {
    /**
     * beta Analytics 采集数据的地址
     */
    private final static String SA_SERVER_URL = "";
    private final static String BETADATA_TEST_APPID = "";// betadata 测试 APPID
    private final static String BETADATA_TEST_APP_SECRET = "";// betadata 测试 TOKEN
    private MyApplication mContext;

    @Override
    public void onCreate() {
        super.onCreate();
        this.mContext = this;
        initBetaDataAPI();
    }


    /**
     * 初始化betadata sdk
     */
    private void initBetaDataAPI() {

        // 传入 Context
        BetaDataAPI.sharedInstance(this, BetaDataAPI.DebugMode.DEBUG_OPEN, SA_SERVER_URL, BETADATA_TEST_APPID, BETADATA_TEST_APP_SECRET);
        // 打开自动采集, 并指定追踪哪些 AutoTrack 事件
        List<BetaDataAPI.AutoTrackEventType> eventTypeList = new ArrayList<>();
        // _AppStart
        eventTypeList.add(BetaDataAPI.AutoTrackEventType.APP_START);
        // _AppEnd
        eventTypeList.add(BetaDataAPI.AutoTrackEventType.APP_END);
        // _AppViewScreen
        eventTypeList.add(BetaDataAPI.AutoTrackEventType.APP_VIEW_SCREEN);
        // _AppClick
        eventTypeList.add(BetaDataAPI.AutoTrackEventType.APP_CLICK);
        // 设置需要采集的参数
        BetaDataAPI.sharedInstance(mContext).enableAutoTrack(eventTypeList);

        BetaDataAPI.sharedInstance().trackFragmentAppViewScreen();


    }

    public Application getAppliaction() {
        return mContext;
    }
}
