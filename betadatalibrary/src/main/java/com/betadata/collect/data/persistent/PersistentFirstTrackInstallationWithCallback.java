
package com.betadata.collect.data.persistent;

import android.content.SharedPreferences;

import java.util.concurrent.Future;

public class PersistentFirstTrackInstallationWithCallback extends PersistentIdentity<Boolean> {
    public PersistentFirstTrackInstallationWithCallback(Future<SharedPreferences> loadStoredPreferences) {
        super(loadStoredPreferences, "first_track_installation_with_callback", new PersistentSerializer<Boolean>() {
            @Override
            public Boolean load(String value) {
                return false;
            }

            @Override
            public String save(Boolean item) {
                return String.valueOf(true);
            }

            @Override
            public Boolean create() {
                return true;
            }
        });
    }
}
