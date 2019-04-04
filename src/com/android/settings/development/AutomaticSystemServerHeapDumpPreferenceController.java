/*
 * Copyright (C) 2019 The Android Open Source Project
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
import android.os.Build;
import android.os.UserManager;
import android.provider.Settings;

import androidx.preference.Preference;
import androidx.preference.SwitchPreference;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

public class AutomaticSystemServerHeapDumpPreferenceController extends
        DeveloperOptionsPreferenceController implements Preference.OnPreferenceChangeListener,
        PreferenceControllerMixin {

    private static final String KEY_AUTOMATIC_SYSTEM_SERVER_HEAP_DUMPS =
            "automatic_system_server_heap_dumps";

    private static final int SETTING_VALUE_OFF = 0;
    private static final int SETTING_VALUE_ON = 1;

    private final UserManager mUserManager;
    private final boolean mIsConfigEnabled;

    public AutomaticSystemServerHeapDumpPreferenceController(Context context) {
        super(context);
        mIsConfigEnabled = context.getResources().getBoolean(
                com.android.internal.R.bool.config_debugEnableAutomaticSystemServerHeapDumps);
        mUserManager = context.getSystemService(UserManager.class);
    }

    @Override
    public boolean isAvailable() {
        return Build.IS_DEBUGGABLE && mIsConfigEnabled
                && !mUserManager.hasUserRestriction(UserManager.DISALLOW_DEBUGGING_FEATURES);
    }

    @Override
    public String getPreferenceKey() {
        return KEY_AUTOMATIC_SYSTEM_SERVER_HEAP_DUMPS;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final boolean isEnabled = (Boolean) newValue;
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Global.ENABLE_AUTOMATIC_SYSTEM_SERVER_HEAP_DUMPS,
                isEnabled ? SETTING_VALUE_ON : SETTING_VALUE_OFF);
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        final int mode = Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Global.ENABLE_AUTOMATIC_SYSTEM_SERVER_HEAP_DUMPS, SETTING_VALUE_ON);
        ((SwitchPreference) mPreference).setChecked(mode != SETTING_VALUE_OFF);
    }

    @Override
    protected void onDeveloperOptionsSwitchDisabled() {
        super.onDeveloperOptionsSwitchDisabled();
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Global.ENABLE_AUTOMATIC_SYSTEM_SERVER_HEAP_DUMPS, SETTING_VALUE_OFF);
        ((SwitchPreference) mPreference).setChecked(false);
    }
}
