
 
package com.betadata.collect;

import android.content.Context;
import android.text.TextUtils;
import android.webkit.JavascriptInterface;

import org.json.JSONException;
import org.json.JSONObject;

/* package */ class AppWebViewInterface {
    private static final String TAG = "BT.AppWebViewInterface";
    private Context mContext;
    private JSONObject properties;
    private boolean enableVerify;

    AppWebViewInterface(Context c, JSONObject p, boolean b) {
        this.mContext = c;
        this.properties = p;
        this.enableVerify = b;
    }

    @JavascriptInterface
    public String sensorsdata_call_app() {
        try {
            if (properties == null) {
                properties = new JSONObject();
            }
            properties.put("type", "Android");
            String loginId = BetaDataAPI.sharedInstance(mContext).getLoginId();
            if (!TextUtils.isEmpty(loginId)) {
                properties.put("distinct_id", loginId);
                properties.put("is_login", true);
            } else {
                properties.put("distinct_id", BetaDataAPI.sharedInstance(mContext).getAnonymousId());
                properties.put("is_login", false);
            }
            return properties.toString();
        } catch (JSONException e) {
            BetaDataLog.i(TAG, e.getMessage());
        }
        return null;
    }

    @JavascriptInterface
    public void sensorsdata_track(String event) {
        try {
            BetaDataAPI.sharedInstance(mContext).trackEventFromH5(event, enableVerify);
        } catch (Exception e) {
            BetaDataLog.printStackTrace(e);
        }
    }

    @JavascriptInterface
    public boolean sensorsdata_verify(String event){
        try {
            if(!enableVerify) {
                sensorsdata_track(event);
                return true;
            }
            return BetaDataAPI.sharedInstance(mContext)._trackEventFromH5(event);
        } catch (Exception e) {
            BetaDataLog.printStackTrace(e);
            return false;
        }
    }
}
