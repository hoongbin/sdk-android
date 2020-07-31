
package com.betadata.collect.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.betadata.collect.data.persistent.PersistentAppEndData;
import com.betadata.collect.data.persistent.PersistentAppEndEventState;
import com.betadata.collect.data.persistent.PersistentAppPaused;
import com.betadata.collect.data.persistent.PersistentAppStart;
import com.betadata.collect.data.persistent.PersistentAppStartTime;
import com.betadata.collect.data.persistent.PersistentDistinctId;
import com.betadata.collect.data.persistent.PersistentFirstDay;
import com.betadata.collect.data.persistent.PersistentFirstStart;
import com.betadata.collect.data.persistent.PersistentFirstTrackInstallation;
import com.betadata.collect.data.persistent.PersistentFirstTrackInstallationWithCallback;
import com.betadata.collect.data.persistent.PersistentIdentity;
import com.betadata.collect.data.persistent.PersistentLoginId;
import com.betadata.collect.data.persistent.PersistentRemoteSDKConfig;
import com.betadata.collect.data.persistent.PersistentSessionIntervalTime;
import com.betadata.collect.data.persistent.PersistentSuperProperties;

import java.util.concurrent.Future;

public class PersistentLoader {

    private static volatile PersistentLoader instance;
    private static Context context;
    private static Future<SharedPreferences> storedPreferences;

    private PersistentLoader(Context context) {
        this.context = context;
        final SharedPreferencesLoader sPrefsLoader = new SharedPreferencesLoader();
        final String prefsName = "com.betadata.analytics.android.sdk.BetaDataAPI";
        final SharedPreferencesLoader.OnPrefsLoadedListener listener =
                        new SharedPreferencesLoader.OnPrefsLoadedListener() {
                            @Override
                            public void onPrefsLoaded(SharedPreferences preferences) {
                            }
                        };
        // 创建名字为prefsName的SharedPreferences。
        storedPreferences = sPrefsLoader.loadPreferences(context, prefsName, listener);
    }

    public static PersistentLoader initLoader(Context context) {
        if (instance == null) {
            instance = new PersistentLoader(context);
        }
        return instance;
    }

    public static PersistentIdentity loadPersistent(String persistentKey) {
        switch (persistentKey) {
            case PersistentName.APP_END_DATA:
                return new PersistentAppEndData(storedPreferences);
            case PersistentName.APP_END_STATE:
                return new PersistentAppEndEventState(storedPreferences);
            case PersistentName.APP_PAUSED_TIME:
                return new PersistentAppPaused(storedPreferences);
            case PersistentName.APP_SESSION_TIME:
                return new PersistentSessionIntervalTime(storedPreferences);
            case PersistentName.APP_START_STATE:
                return new PersistentAppStart(storedPreferences);
            case PersistentName.APP_START_TIME:
                return new PersistentAppStartTime(storedPreferences);
            case PersistentName.DISTINCT_ID:
                return new PersistentDistinctId(storedPreferences, context);
            case PersistentName.FIRST_DAY:
                return new PersistentFirstDay(storedPreferences);
            case PersistentName.FIRST_INSTALL:
                return new PersistentFirstTrackInstallation(storedPreferences);
            case PersistentName.FIRST_INSTALL_CALLBACK:
                return new PersistentFirstTrackInstallationWithCallback(storedPreferences);
            case PersistentName.FIRST_START:
                return new PersistentFirstStart(storedPreferences);
            case PersistentName.LOGIN_ID:
                return new PersistentLoginId(storedPreferences);
            case PersistentName.REMOTE_CONFIG:
                return new PersistentRemoteSDKConfig(storedPreferences);
            case PersistentName.SUPER_PROPERTIES:
                return new PersistentSuperProperties(storedPreferences);
        }
        return null;
    }

    public static class PersistentName {
        static final String APP_END_DATA = DbParams.TABLE_APPENDDATA;
        static final String APP_END_STATE = DbParams.TABLE_APPENDSTATE;
        static final String APP_PAUSED_TIME = DbParams.TABLE_APPPAUSEDTIME;
        static final String APP_START_STATE = DbParams.TABLE_APPSTARTED;
        static final String APP_START_TIME = DbParams.TABLE_APPSTARTTIME;
        static final String APP_SESSION_TIME = DbParams.TABLE_SESSIONINTERVALTIME;
        public static final String DISTINCT_ID = "events_distinct_id";
        public static final String FIRST_DAY = "first_day";
        public static final String FIRST_START = "first_start";
        public static final String FIRST_INSTALL = "first_track_installation";
        public static final String FIRST_INSTALL_CALLBACK = "first_track_installation_with_callback";
        public static final String LOGIN_ID = "events_login_id";
        public static final String REMOTE_CONFIG = "sensorsdata_sdk_configuration";
        public static final String SUPER_PROPERTIES = "super_properties";
    }
}
