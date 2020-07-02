
 
package com.betadata.collect.data.persistent;

import android.content.SharedPreferences;


import com.betadata.collect.data.DbParams;

import java.util.concurrent.Future;

public class PersistentAppStartTime extends PersistentIdentity<Long> {
    public PersistentAppStartTime(Future<SharedPreferences> loadStoredPreferences) {
        super(loadStoredPreferences, DbParams.TABLE_APPSTARTTIME, new PersistentSerializer<Long>() {
            @Override
            public Long load(String value) {
                return Long.valueOf(value);
            }

            @Override
            public String save(Long item) {
                return String.valueOf(item);
            }

            @Override
            public Long create() {
                return Long.valueOf(0);
            }
        });
    }
}
