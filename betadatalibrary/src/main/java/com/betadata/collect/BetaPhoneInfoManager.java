package com.betadata.collect;

/**
 * Author: 李巷阳
 * Date: 2020-02-18
 * Version: V2.0.0
 * Part:    手机信息管理器
 * Description:
 */
public class BetaPhoneInfoManager {

    private volatile static BetaPhoneInfoManager mSingleton = null;
    private String imei="";
    private String oaid="";

    public String getImei() {
        return imei;
    }

    public void setImei(String imei) {
        this.imei = imei;
    }

    public String getOaid() {
        return oaid;
    }

    public void setOaid(String oaid) {
        this.oaid = oaid;
    }

    private BetaPhoneInfoManager() {
    }

    public static BetaPhoneInfoManager getInstance() {
        if (mSingleton == null) {
            synchronized (BetaPhoneInfoManager.class) {
                if (mSingleton == null) {
                    mSingleton = new BetaPhoneInfoManager();
                }
            }
        }
        return mSingleton;

    }

}
