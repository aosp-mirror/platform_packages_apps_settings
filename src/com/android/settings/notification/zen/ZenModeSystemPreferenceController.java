/*
 * Copyright (C) 2018 The Android Open Source Project
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

public class ZenModeSystemPreferenceController extends
        AbstractZenModePreferenceController implements Preference.OnPreferenceChangeListener {

    protected static final String KEY = "zen_mode_system";

    public ZenModeSystemPreferenceController(Context context, Lifecycle lifecycle) {
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
                pref.setEnabled(false);
                pref.setChecked(false);
                break;
            case Settings.Global.ZEN_MODE_ALARMS:
                pref.setEnabled(false);
                pref.setChecked(false);
                break;
            default:
                pref.setEnabled(true);
                pref.setChecked(mBackend.isPriorityCategoryEnabled(
                        Policy.PRIORITY_CATEGORY_SYSTEM));
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final boolean allowSystem = (Boolean) newValue;
        if (ZenModeSettingsBase.DEBUG) {
            Log.d(TAG, "onPrefChange allowSystem=" + allowSystem);
        }

        mMetricsFeatureProvider.action(mContext, SettingsEnums.ACTION_ZEN_ALLOW_SYSTEM,
                allowSystem);
        mBackend.saveSoundPolicy(Policy.PRIORITY_CATEGORY_SYSTEM, allowSystem);
        return true;
    }
}
