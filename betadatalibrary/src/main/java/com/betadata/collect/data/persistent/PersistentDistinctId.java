
package com.betadata.collect.data.persistent;

import android.content.Context;
import android.content.SharedPreferences;

import com.betadata.collect.util.BetaDataUtils;

import java.util.UUID;
import java.util.concurrent.Future;

public class PersistentDistinctId extends PersistentIdentity<String> {
    public PersistentDistinctId(Future<SharedPreferences> loadStoredPreferences, final Context context) {
        super(loadStoredPreferences, "events_distinct_id", new PersistentSerializer<String>() {
            @Override
            public String load(String value) {
                return value;
            }

            @Override
            public String save(String item) {
                return item;
            }

            @Override
            public String create() {
                String androidId = BetaDataUtils.getAndroidID(context);
                if (BetaDataUtils.isValidAndroidId(androidId)) {
                    return androidId;
                }
                return UUID.randomUUID().toString();
            }
        });
    }
}
