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


import android.content.Context;
import android.content.Intent;
import android.os.UserManager;
import android.provider.Settings;
import android.support.annotation.VisibleForTesting;
import android.support.v14.preference.SwitchPreference;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;

public class AdbPreferenceController extends DeveloperOptionsPreferenceController implements
        Preference.OnPreferenceChangeListener {

    public static final String ADB_STATE_CHANGED =
            "com.android.settings.development.AdbPreferenceController.ADB_STATE_CHANGED";
    public static final int ADB_SETTING_ON = 1;
    public static final int ADB_SETTING_OFF = 0;

    private static final String KEY_ENABLE_ADB = "enable_adb";

    private final DevelopmentSettingsDashboardFragment mFragment;
    private SwitchPreference mPreference;

    public AdbPreferenceController(Context context, DevelopmentSettingsDashboardFragment fragment) {
        super(context);
        mFragment = fragment;
    }

    @Override
    public boolean isAvailable() {
        return mContext.getSystemService(UserManager.class).isAdminUser();
    }

    @Override
    public String getPreferenceKey() {
        return KEY_ENABLE_ADB;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);

        mPreference = (SwitchPreference) screen.findPreference(getPreferenceKey());
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final boolean isAdbEnabled = (Boolean) newValue;
        if (isAdbEnabled) {
            EnableAdbWarningDialog.show(mFragment);
        } else {
            writeAdbSetting(isAdbEnabled);
            notifyStateChanged();
        }
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        final int adbMode = Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.ADB_ENABLED, 0 /* default */);
        mPreference.setChecked(adbMode != ADB_SETTING_OFF);
    }

    @Override
    protected void onDeveloperOptionsSwitchEnabled() {
        mPreference.setEnabled(true);
    }

    @Override
    protected void onDeveloperOptionsSwitchDisabled() {
        writeAdbSetting(false);
        notifyStateChanged();
        mPreference.setEnabled(false);
        mPreference.setChecked(false);
    }

    public void onAdbDialogConfirmed() {
        writeAdbSetting(true);
        notifyStateChanged();
    }

    public void onAdbDialogDismissed() {
        updateState(mPreference);
    }

    private void writeAdbSetting(boolean enabled) {
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.ADB_ENABLED, enabled ? ADB_SETTING_ON : ADB_SETTING_OFF);
    }

    @VisibleForTesting
    void notifyStateChanged() {
        LocalBroadcastManager.getInstance(mContext)
                .sendBroadcast(new Intent(ADB_STATE_CHANGED));
    }
}
