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

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.widget.SelectorWithWidgetPreference;

public class UnrestrictedPreferenceController extends AbstractPreferenceController
        implements PreferenceControllerMixin {

    private static final String TAG = "UNRESTRICTED_PREF";

    @VisibleForTesting static final String KEY_UNRESTRICTED_PREF = "unrestricted_preference";

    @VisibleForTesting BatteryOptimizeUtils mBatteryOptimizeUtils;

    public UnrestrictedPreferenceController(Context context, int uid, String packageName) {
        super(context);
        mBatteryOptimizeUtils = new BatteryOptimizeUtils(context, uid, packageName);
    }

    @Override
    public void updateState(Preference preference) {
        preference.setEnabled(mBatteryOptimizeUtils.isSelectorPreferenceEnabled());

        final boolean isUnrestricted =
                mBatteryOptimizeUtils.getAppOptimizationMode()
                        == BatteryOptimizeUtils.MODE_UNRESTRICTED;
        ((SelectorWithWidgetPreference) preference).setChecked(isUnrestricted);
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getPreferenceKey() {
        return KEY_UNRESTRICTED_PREF;
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        return getPreferenceKey().equals(preference.getKey());
    }
}
