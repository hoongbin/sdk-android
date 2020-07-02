
 
package com.betadata.collect.data.persistent;

import android.content.SharedPreferences;

import com.betadata.collect.data.DbParams;

import java.util.concurrent.Future;

public class PersistentAppPaused extends PersistentIdentity<Long> {
    public PersistentAppPaused(Future<SharedPreferences> loadStoredPreferences) {
        super(loadStoredPreferences, DbParams.TABLE_APPPAUSEDTIME, new PersistentSerializer<Long>() {
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
