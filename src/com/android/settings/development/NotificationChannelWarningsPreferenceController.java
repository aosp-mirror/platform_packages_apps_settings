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
import android.os.Build;
import android.provider.Settings;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.SwitchPreference;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

public class NotificationChannelWarningsPreferenceController extends
        DeveloperOptionsPreferenceController implements Preference.OnPreferenceChangeListener,
        PreferenceControllerMixin {

    private static final String SHOW_NOTIFICATION_CHANNEL_WARNINGS_KEY =
            "show_notification_channel_warnings";

    @VisibleForTesting
    final static int SETTING_VALUE_ON = 1;
    @VisibleForTesting
    final static int SETTING_VALUE_OFF = 0;
    @VisibleForTesting
    final static int DEBUGGING_ENABLED = 1;
    @VisibleForTesting
    final static int DEBUGGING_DISABLED = 0;

    public NotificationChannelWarningsPreferenceController(Context context) {
        super(context);
    }


    @Override
    public String getPreferenceKey() {
        return SHOW_NOTIFICATION_CHANNEL_WARNINGS_KEY;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final boolean isEnabled = (Boolean) newValue;
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.SHOW_NOTIFICATION_CHANNEL_WARNINGS,
                isEnabled ? SETTING_VALUE_ON : SETTING_VALUE_OFF);
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        final int defaultWarningEnabled = isDebuggable() ? DEBUGGING_ENABLED : DEBUGGING_DISABLED;
        final int mode = Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.SHOW_NOTIFICATION_CHANNEL_WARNINGS, defaultWarningEnabled);
        ((SwitchPreference) mPreference).setChecked(mode != SETTING_VALUE_OFF);
    }

    @Override
    protected void onDeveloperOptionsSwitchDisabled() {
        super.onDeveloperOptionsSwitchDisabled();
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.SHOW_NOTIFICATION_CHANNEL_WARNINGS, SETTING_VALUE_OFF);
        ((SwitchPreference) mPreference).setChecked(false);
    }

    @VisibleForTesting
    boolean isDebuggable() {
        return Build.TYPE.equals("eng");
    }
}
