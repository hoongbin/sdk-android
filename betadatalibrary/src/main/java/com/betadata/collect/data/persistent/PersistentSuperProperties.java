
package com.betadata.collect.data.persistent;

import android.content.SharedPreferences;


import com.betadata.collect.BetaDataLog;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.Future;

public class PersistentSuperProperties extends PersistentIdentity<JSONObject> {
    public PersistentSuperProperties(Future<SharedPreferences> loadStoredPreferences) {
        super(loadStoredPreferences, "super_properties", new PersistentSerializer<JSONObject>() {
            @Override
            public JSONObject load(String value) {
                try {
                    return new JSONObject(value);
                } catch (JSONException e) {
                    BetaDataLog.d("Persistent", "failed to load SuperProperties from SharedPreferences.", e);
                    return null;
                }
            }

            @Override
            public String save(JSONObject item) {
                return item.toString();
            }

            @Override
            public JSONObject create() {
                return new JSONObject();
            }
        });
    }
}
