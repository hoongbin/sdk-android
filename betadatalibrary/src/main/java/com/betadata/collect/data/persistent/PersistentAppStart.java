
 
package com.betadata.collect.data.persistent;

import android.content.SharedPreferences;


import com.betadata.collect.data.DbParams;

import java.util.concurrent.Future;

public class PersistentAppStart extends PersistentIdentity<Boolean> {
    public PersistentAppStart(Future<SharedPreferences> loadStoredPreferences) {
        super(loadStoredPreferences, DbParams.TABLE_APPSTARTED, new PersistentSerializer<Boolean>() {
            @Override
            public Boolean load(String value) {
                return Boolean.valueOf(value);
            }

            @Override
            public String save(Boolean item) {
                return String.valueOf(item);
            }

            @Override
            public Boolean create() {
                return true;
            }
        });
    }
}
