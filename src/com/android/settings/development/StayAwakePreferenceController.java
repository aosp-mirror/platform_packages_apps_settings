package com.android.settings.development;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.VisibleForTesting;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedSwitchPreference;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;


public class StayAwakePreferenceController extends AbstractPreferenceController implements
        PreferenceControllerMixin, Preference.OnPreferenceChangeListener, LifecycleObserver,
        OnResume, OnPause {

    private static final String TAG = "StayAwakeCtrl";
    private static final String PREFERENCE_KEY = "keep_screen_on";
    @VisibleForTesting
    static final int SETTING_VALUE_OFF = 0;
    @VisibleForTesting
    static final int SETTING_VALUE_ON =
            BatteryManager.BATTERY_PLUGGED_AC | BatteryManager.BATTERY_PLUGGED_USB
                    | BatteryManager.BATTERY_PLUGGED_WIRELESS;
    @VisibleForTesting
    SettingsObserver mSettingsObserver;

    private RestrictedSwitchPreference mPreference;

    public StayAwakePreferenceController(Context context, Lifecycle lifecycle) {
        super(context);
        mSettingsObserver = new SettingsObserver();

        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getPreferenceKey() {
        return PREFERENCE_KEY;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = (RestrictedSwitchPreference) screen.findPreference(getPreferenceKey());
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final boolean stayAwake = (Boolean) newValue;
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.STAY_ON_WHILE_PLUGGED_IN,
                stayAwake ? SETTING_VALUE_ON : SETTING_VALUE_OFF);
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        final RestrictedLockUtils.EnforcedAdmin admin = checkIfMaximumTimeToLockSetByAdmin();
        if (admin != null) {
            mPreference.setDisabledByAdmin(admin);
            return;
        }

        final int stayAwakeMode = Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.STAY_ON_WHILE_PLUGGED_IN,
                SETTING_VALUE_OFF);
        mPreference.setChecked(stayAwakeMode != SETTING_VALUE_OFF);
    }

    @Override
    public void onResume() {
        if (mPreference != null) {
            mSettingsObserver.register(true /* register */);
        }
    }

    @Override
    public void onPause() {
        if (mPreference != null) {
            mSettingsObserver.register(false /* unregister */);
        }
    }

    @VisibleForTesting
    RestrictedLockUtils.EnforcedAdmin checkIfMaximumTimeToLockSetByAdmin() {
        // A DeviceAdmin has specified a maximum time until the device
        // will lock...  in this case we can't allow the user to turn
        // on "stay awake when plugged in" because that would defeat the
        // restriction.
        return RestrictedLockUtils.checkIfMaximumTimeToLockIsSet(mContext);
    }

    @VisibleForTesting
    class SettingsObserver extends ContentObserver {
        private final Uri mStayAwakeUri = Settings.Global.getUriFor(
                Settings.Global.STAY_ON_WHILE_PLUGGED_IN);

        public SettingsObserver() {
            super(new Handler());
        }

        public void register(boolean register) {
            final ContentResolver cr = mContext.getContentResolver();
            if (register) {
                cr.registerContentObserver(
                        mStayAwakeUri, false, this);
            } else {
                cr.unregisterContentObserver(this);
            }
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange, uri);
            if (mStayAwakeUri.equals(uri)) {
                updateState(mPreference);
            }
        }
    }
}
