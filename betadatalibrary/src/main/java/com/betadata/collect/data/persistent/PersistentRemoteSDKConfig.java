
package com.betadata.collect.data.persistent;

import android.content.SharedPreferences;

import java.util.concurrent.Future;

public class PersistentRemoteSDKConfig extends PersistentIdentity<String> {
    public PersistentRemoteSDKConfig(Future<SharedPreferences> loadStoredPreferences) {
        super(loadStoredPreferences, "sensorsdata_sdk_configuration", new PersistentSerializer<String>() {
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
                return null;
            }
        });
    }
}
