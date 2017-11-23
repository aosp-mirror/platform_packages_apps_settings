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

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.UserManager;
import android.provider.Settings;
import android.support.annotation.VisibleForTesting;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;
import com.android.settingslib.wrapper.PackageManagerWrapper;

public class BugReportInPowerPreferenceController extends
        DeveloperOptionsPreferenceController implements Preference.OnPreferenceChangeListener,
        PreferenceControllerMixin {

    private static final String KEY_BUGREPORT_IN_POWER = "bugreport_in_power";

    @VisibleForTesting
    static final ComponentName COMPONENT_NAME = new ComponentName("com.android.shell",
            "com.android.shell.BugreportStorageProvider");
    @VisibleForTesting
    static int SETTING_VALUE_ON = 1;
    @VisibleForTesting
    static int SETTING_VALUE_OFF = 0;

    private final PackageManagerWrapper mPackageManager;
    private final UserManager mUserManager;
    private SwitchPreference mPreference;

    public BugReportInPowerPreferenceController(Context context) {
        super(context);
        mUserManager = (UserManager) context.getSystemService(Context.USER_SERVICE);
        mPackageManager = new PackageManagerWrapper(context.getPackageManager());
    }

    @Override
    public boolean isAvailable() {
        return !mUserManager.hasUserRestriction(UserManager.DISALLOW_DEBUGGING_FEATURES);
    }

    @Override
    public String getPreferenceKey() {
        return KEY_BUGREPORT_IN_POWER;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);

        mPreference = (SwitchPreference) screen.findPreference(KEY_BUGREPORT_IN_POWER);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final boolean isEnabled = (Boolean) newValue;
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Global.BUGREPORT_IN_POWER_MENU,
                isEnabled ? SETTING_VALUE_ON : SETTING_VALUE_OFF);
        setBugreportStorageProviderStatus(isEnabled);
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        final int mode = Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Global.BUGREPORT_IN_POWER_MENU, SETTING_VALUE_OFF);
        mPreference.setChecked(mode != SETTING_VALUE_OFF);
    }

    @Override
    protected void onDeveloperOptionsSwitchEnabled() {
        // no-op because this preference can never be disabled
    }

    @Override
    protected void onDeveloperOptionsSwitchDisabled() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Global.BUGREPORT_IN_POWER_MENU, SETTING_VALUE_OFF);
        setBugreportStorageProviderStatus(false);
        mPreference.setChecked(false);
    }

    private void setBugreportStorageProviderStatus(boolean isEnabled) {
        mPackageManager.setComponentEnabledSetting(COMPONENT_NAME,
                isEnabled ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                        : PackageManager.COMPONENT_ENABLED_STATE_DEFAULT, 0 /* flags */);
    }
}
