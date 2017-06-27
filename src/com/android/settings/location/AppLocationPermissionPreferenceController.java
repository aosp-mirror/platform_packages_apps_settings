package com.android.settings.location;

import android.content.Context;
import android.provider.Settings;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;

public class AppLocationPermissionPreferenceController extends
        AbstractPreferenceController implements PreferenceControllerMixin {

    private static final String KEY_APP_LEVEL_PERMISSIONS = "app_level_permissions";
    private Preference mPreference;

    public AppLocationPermissionPreferenceController(Context context) {
        super(context);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        if (isAvailable()) {
            mPreference = screen.findPreference(KEY_APP_LEVEL_PERMISSIONS);
        }
    }

    @Override
    public String getPreferenceKey() {
        return KEY_APP_LEVEL_PERMISSIONS;
    }

    @Override
    public boolean isAvailable() {
        return Settings.Global.getInt(mContext.getContentResolver(),
                android.provider.Settings.Global.LOCATION_SETTINGS_LINK_TO_PERMISSIONS_ENABLED, 1)
                == 1;
    }
}
