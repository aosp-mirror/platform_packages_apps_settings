/*
 * Copyright (C) 2021 The Android Open Source Project
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


package com.android.settings.fuelgauge;

import android.content.Context;
import android.util.Log;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.widget.SelectorWithWidgetPreference;

public class RestrictedPreferenceController extends AbstractPreferenceController
        implements PreferenceControllerMixin {

    private static final String TAG = "RESTRICTED_PREF";

    @VisibleForTesting String KEY_RESTRICTED_PREF = "restricted_pref";
    @VisibleForTesting BatteryOptimizeUtils mBatteryOptimizeUtils;

    public RestrictedPreferenceController(Context context, int uid, String packageName) {
        super(context);
        mBatteryOptimizeUtils = new BatteryOptimizeUtils(context, uid, packageName);
    }

    @Override
    public void updateState(Preference preference) {

        if (!mBatteryOptimizeUtils.isValidPackageName()) {
            Log.d(TAG, "invalid package name, disable pref");
            preference.setEnabled(false);
            return;
        } else {
            preference.setEnabled(true);
        }

        if (mBatteryOptimizeUtils.isSystemOrDefaultApp()) {
            Log.d(TAG, "is system or default app, disable pref");
            ((SelectorWithWidgetPreference) preference).setChecked(false);
            preference.setEnabled(false);
        } else if (mBatteryOptimizeUtils.getAppOptimizationMode()
                == BatteryOptimizeUtils.MODE_RESTRICTED) {
            Log.d(TAG, "is restricted states");
            ((SelectorWithWidgetPreference) preference).setChecked(true);
        } else {
            ((SelectorWithWidgetPreference) preference).setChecked(false);
        }
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getPreferenceKey() {
        return KEY_RESTRICTED_PREF;
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        return getPreferenceKey().equals(preference.getKey());
    }
}
