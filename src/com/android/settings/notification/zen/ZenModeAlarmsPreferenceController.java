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

package com.android.settings.notification.zen;

import android.app.NotificationManager.Policy;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.provider.Settings;
import android.util.Log;

import androidx.preference.Preference;
import androidx.preference.TwoStatePreference;

import com.android.settingslib.core.lifecycle.Lifecycle;

public class ZenModeAlarmsPreferenceController extends
        AbstractZenModePreferenceController implements Preference.OnPreferenceChangeListener {

    private final String KEY;

    public ZenModeAlarmsPreferenceController(Context context, Lifecycle lifecycle, String key) {
        super(context, key, lifecycle);
        KEY = key;
    }

    @Override
    public String getPreferenceKey() {
        return KEY;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);

        TwoStatePreference pref = (TwoStatePreference) preference;
        switch (getZenMode()) {
            case Settings.Global.ZEN_MODE_NO_INTERRUPTIONS:
                pref.setEnabled(false);
                pref.setChecked(false);
                break;
            case Settings.Global.ZEN_MODE_ALARMS:
                pref.setEnabled(false);
                pref.setChecked(true);
                break;
            default:
                pref.setEnabled(true);
                pref.setChecked(mBackend.isPriorityCategoryEnabled(
                        Policy.PRIORITY_CATEGORY_ALARMS));
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final boolean allowAlarms = (Boolean) newValue;
        if (ZenModeSettingsBase.DEBUG) {
            Log.d(TAG, "onPrefChange allowAlarms=" + allowAlarms);
        }

        mMetricsFeatureProvider.action(mContext, SettingsEnums.ACTION_ZEN_ALLOW_ALARMS,
                allowAlarms);
        mBackend.saveSoundPolicy(Policy.PRIORITY_CATEGORY_ALARMS, allowAlarms);

        return true;
    }
}
