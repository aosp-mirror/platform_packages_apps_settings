/*
 * Copyright (C) 2018 The Android Open Source Project
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
import android.provider.Settings;

import androidx.preference.Preference;
import androidx.preference.TwoStatePreference;

import com.android.settings.core.BasePreferenceController;
import com.android.settings.overlay.FeatureFactory;

/** Controller to change and update the auto restriction toggle */
public class AutoRestrictionPreferenceController extends BasePreferenceController
        implements Preference.OnPreferenceChangeListener {
    private static final String KEY_SMART_BATTERY = "auto_restriction";
    private static final int ON = 1;
    private static final int OFF = 0;
    private final PowerUsageFeatureProvider mPowerUsageFeatureProvider;

    public AutoRestrictionPreferenceController(Context context) {
        super(context, KEY_SMART_BATTERY);
        mPowerUsageFeatureProvider =
                FeatureFactory.getFeatureFactory().getPowerUsageFeatureProvider();
    }

    @Override
    public int getAvailabilityStatus() {
        return mPowerUsageFeatureProvider.isSmartBatterySupported()
                ? UNSUPPORTED_ON_DEVICE
                : AVAILABLE;
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        final boolean smartBatteryOn =
                Settings.Global.getInt(
                                mContext.getContentResolver(),
                                Settings.Global.APP_AUTO_RESTRICTION_ENABLED,
                                ON)
                        == ON;
        ((TwoStatePreference) preference).setChecked(smartBatteryOn);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final boolean smartBatteryOn = (Boolean) newValue;
        Settings.Global.putInt(
                mContext.getContentResolver(),
                Settings.Global.APP_AUTO_RESTRICTION_ENABLED,
                smartBatteryOn ? ON : OFF);
        return true;
    }
}
