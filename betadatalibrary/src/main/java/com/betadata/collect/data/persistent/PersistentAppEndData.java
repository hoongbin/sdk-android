
package com.betadata.collect.data.persistent;

import android.content.SharedPreferences;

import com.betadata.collect.data.DbParams;

import java.util.concurrent.Future;

public class PersistentAppEndData extends PersistentIdentity<String> {
    public PersistentAppEndData(Future<SharedPreferences> loadStoredPreferences) {
        super(loadStoredPreferences, DbParams.TABLE_APPENDDATA, new PersistentSerializer<String>() {
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
                return "";
            }
        });
    }
}
