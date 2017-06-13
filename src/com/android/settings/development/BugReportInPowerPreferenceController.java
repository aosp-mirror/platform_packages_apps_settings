/*
 * Copyright (C) 2016 The Android Open Source Project
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
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;

public class BugReportInPowerPreferenceController extends AbstractPreferenceController
        implements PreferenceControllerMixin {

    private static final String KEY_BUGREPORT_IN_POWER = "bugreport_in_power";

    private UserManager mUserManager;
    private SwitchPreference mPreference;

    public BugReportInPowerPreferenceController(Context context) {
        super(context);
        mUserManager = (UserManager) context.getSystemService(Context.USER_SERVICE);
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (KEY_BUGREPORT_IN_POWER.equals(preference.getKey())) {
            final SwitchPreference switchPreference = (SwitchPreference) preference;
            Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Global.BUGREPORT_IN_POWER_MENU,
                switchPreference.isChecked() ? 1 : 0);
            setBugreportStorageProviderStatus();
            return true;
        }
        return false;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        if (isAvailable()) {
            mPreference = (SwitchPreference) screen.findPreference(KEY_BUGREPORT_IN_POWER);
        }
    }

    @Override
    public String getPreferenceKey() {
        return KEY_BUGREPORT_IN_POWER;
    }

    @Override
    public boolean isAvailable() {
        return !mUserManager.hasUserRestriction(UserManager.DISALLOW_DEBUGGING_FEATURES);
    }

    @Override
    public void updateState(Preference preference) {
        updatePreference();
    }

    public void enablePreference(boolean enabled) {
        if (isAvailable()) {
            mPreference.setEnabled(enabled);
        }
    }

    public void resetPreference() {
        if (mPreference.isChecked()) {
            mPreference.setChecked(false);
            handlePreferenceTreeClick(mPreference);
        }
    }

    public boolean updatePreference() {
        if (!isAvailable()) {
            return false;
        }
        final boolean enabled = Settings.Secure.getInt(
            mContext.getContentResolver(), Settings.Global.BUGREPORT_IN_POWER_MENU, 0) != 0;
        mPreference.setChecked(enabled);
        return enabled;
    }

    public void updateBugreportOptions() {
        if (!isAvailable()) {
            return;
        }
        mPreference.setEnabled(true);
        setBugreportStorageProviderStatus();
    }

    private void setBugreportStorageProviderStatus() {
        final ComponentName componentName = new ComponentName("com.android.shell",
            "com.android.shell.BugreportStorageProvider");
        final boolean enabled = mPreference.isChecked();
        mContext.getPackageManager().setComponentEnabledSetting(componentName,
            enabled ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                : PackageManager.COMPONENT_ENABLED_STATE_DEFAULT,
            0);
    }

}
