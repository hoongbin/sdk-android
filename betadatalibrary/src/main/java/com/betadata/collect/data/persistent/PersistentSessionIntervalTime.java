
package com.betadata.collect.data.persistent;

import android.content.SharedPreferences;

import com.betadata.collect.common.BetaDataConfig;
import com.betadata.collect.data.DbParams;

import java.util.concurrent.Future;

public class PersistentSessionIntervalTime extends PersistentIdentity<Integer> {
    public PersistentSessionIntervalTime(Future<SharedPreferences> loadStoredPreferences) {
        super(loadStoredPreferences, DbParams.TABLE_SESSIONINTERVALTIME, new PersistentSerializer<Integer>() {
            @Override
            public Integer load(String value) {
                return Integer.valueOf(value);
            }

            @Override
            public String save(Integer item) {
                return item == null ? "" : item.toString();
            }

            @Override
            public Integer create() {
                return BetaDataConfig.mSessionTime;
            }
        });
    }
}
