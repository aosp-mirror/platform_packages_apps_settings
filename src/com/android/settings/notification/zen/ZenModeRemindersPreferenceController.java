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

import android.app.NotificationManager;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.provider.Settings;
import android.util.Log;

import androidx.preference.Preference;
import androidx.preference.TwoStatePreference;

import com.android.settingslib.core.lifecycle.Lifecycle;

public class ZenModeRemindersPreferenceController extends AbstractZenModePreferenceController
        implements Preference.OnPreferenceChangeListener {

    protected static final String KEY = "zen_mode_reminders";

    public ZenModeRemindersPreferenceController(Context context, Lifecycle lifecycle) {
        super(context, KEY, lifecycle);
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
            case Settings.Global.ZEN_MODE_ALARMS:
                pref.setEnabled(false);
                pref.setChecked(false);
                break;
            default:
                pref.setEnabled(true);
                pref.setChecked(mBackend.isPriorityCategoryEnabled(
                        NotificationManager.Policy.PRIORITY_CATEGORY_REMINDERS));
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final boolean allowReminders = (Boolean) newValue;
        if (ZenModeSettingsBase.DEBUG) {
            Log.d(TAG, "onPrefChange allowReminders=" + allowReminders);
        }
        mMetricsFeatureProvider.action(mContext,
                SettingsEnums.ACTION_ZEN_ALLOW_REMINDERS, allowReminders);
        mBackend.saveSoundPolicy(NotificationManager.Policy.PRIORITY_CATEGORY_REMINDERS,
                allowReminders);
        return true;
    }
}
