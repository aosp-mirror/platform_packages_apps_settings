/*
 * Copyright (C) 2023 The Android Open Source Project
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
import com.android.settingslib.PrimarySwitchPreference;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.widget.MainSwitchPreference;

/** Controller to update the app background usage state */
public class AllowBackgroundPreferenceController extends AbstractPreferenceController
        implements PreferenceControllerMixin {

    private static final String TAG = "AllowBackgroundPreferenceController";

    @VisibleForTesting static final String KEY_ALLOW_BACKGROUND_USAGE = "allow_background_usage";

    @VisibleForTesting BatteryOptimizeUtils mBatteryOptimizeUtils;

    public AllowBackgroundPreferenceController(Context context, int uid, String packageName) {
        super(context);
        mBatteryOptimizeUtils = new BatteryOptimizeUtils(context, uid, packageName);
    }

    private void setChecked(Preference preference, boolean checked) {
        if (preference instanceof PrimarySwitchPreference) {
            ((PrimarySwitchPreference) preference).setChecked(checked);
        } else if (preference instanceof MainSwitchPreference) {
            ((MainSwitchPreference) preference).setChecked(checked);
        }
    }

    private void setEnabled(Preference preference, boolean enabled) {
        if (preference instanceof PrimarySwitchPreference) {
            ((PrimarySwitchPreference) preference).setEnabled(enabled);
            ((PrimarySwitchPreference) preference).setSwitchEnabled(enabled);
        } else if (preference instanceof MainSwitchPreference) {
            ((MainSwitchPreference) preference).setEnabled(enabled);
        }
    }

    @Override
    public void updateState(Preference preference) {
        setEnabled(preference, mBatteryOptimizeUtils.isOptimizeModeMutable());

        final boolean isAllowBackground =
                mBatteryOptimizeUtils.getAppOptimizationMode()
                        != BatteryOptimizeUtils.MODE_RESTRICTED;
        setChecked(preference, isAllowBackground);
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getPreferenceKey() {
        return KEY_ALLOW_BACKGROUND_USAGE;
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        return getPreferenceKey().equals(preference.getKey());
    }
}
