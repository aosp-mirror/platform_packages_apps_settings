/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.development;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Handler;
import android.provider.Settings;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedLockUtilsInternal;
import com.android.settingslib.RestrictedSwitchPreference;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;


public class StayAwakePreferenceController extends DeveloperOptionsPreferenceController
        implements Preference.OnPreferenceChangeListener, LifecycleObserver, OnResume, OnPause,
        PreferenceControllerMixin {

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

        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }
    }

    @Override
    public String getPreferenceKey() {
        return PREFERENCE_KEY;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
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
        if (mPreference == null) {
            return;
        }
        if (mSettingsObserver == null) {
            mSettingsObserver = new SettingsObserver();
        }
        mSettingsObserver.register(true /* register */);
    }

    @Override
    public void onPause() {
        if (mPreference == null || mSettingsObserver == null) {
            return;
        }
        mSettingsObserver.register(false /* unregister */);
    }

    @Override
    protected void onDeveloperOptionsSwitchDisabled() {
        super.onDeveloperOptionsSwitchDisabled();
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.STAY_ON_WHILE_PLUGGED_IN, SETTING_VALUE_OFF);
        mPreference.setChecked(false);
    }

    @VisibleForTesting
    RestrictedLockUtils.EnforcedAdmin checkIfMaximumTimeToLockSetByAdmin() {
        // A DeviceAdmin has specified a maximum time until the device
        // will lock...  in this case we can't allow the user to turn
        // on "stay awake when plugged in" because that would defeat the
        // restriction.
        return RestrictedLockUtilsInternal.checkIfMaximumTimeToLockIsSet(mContext);
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
