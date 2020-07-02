
 
package com.betadata.collect;

import org.json.JSONObject;

public interface BetaDataTrackEventCallBack {
     /**
      *
      * @param eventName 事件名称
      * @param eventProperties 要修改的事件属性
      * @return true 表示事件将入库， false 表示事件将被抛弃
      */
     boolean onTrackEvent(String eventName, JSONObject eventProperties);

}
