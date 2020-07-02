
package com.betadata.collect;

import org.json.JSONException;
import org.json.JSONObject;

public interface BetaExpandableListViewItemTrackProperties {
    /**
     * 点击 groupPosition、childPosition 处 item 的扩展属性
     * @param groupPosition int
     * @param childPosition int
     * @return JSONObject
     * @throws JSONException JSONException
     */
    JSONObject getBetaChildItemTrackProperties(int groupPosition, int childPosition) throws JSONException;

    /**
     * 点击 groupPosition 处 item 的扩展属性
     * @param groupPosition int
     * @return JSONObject
     * @throws JSONException JSONException
     */
    JSONObject getBetaGroupItemTrackProperties(int groupPosition) throws JSONException;
}
