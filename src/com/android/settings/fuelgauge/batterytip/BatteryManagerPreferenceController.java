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

package com.android.settings.fuelgauge.batterytip;

import android.app.AppOpsManager;
import android.content.Context;
import android.os.UserManager;
import android.provider.Settings;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.fuelgauge.PowerUsageFeatureProvider;
import com.android.settings.overlay.FeatureFactory;

/**
 * Preference controller to control the battery manager
 */
public class BatteryManagerPreferenceController extends BasePreferenceController {
    private static final String KEY_BATTERY_MANAGER = "smart_battery_manager";
    private static final int ON = 1;
    private PowerUsageFeatureProvider mPowerUsageFeatureProvider;
    private AppOpsManager mAppOpsManager;
    private UserManager mUserManager;

    public BatteryManagerPreferenceController(Context context) {
        super(context, KEY_BATTERY_MANAGER);
        mPowerUsageFeatureProvider = FeatureFactory.getFactory(
                context).getPowerUsageFeatureProvider(context);
        mAppOpsManager = context.getSystemService(AppOpsManager.class);
        mUserManager = context.getSystemService(UserManager.class);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE_UNSEARCHABLE;
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        final int num = BatteryTipUtils.getRestrictedAppsList(mAppOpsManager, mUserManager).size();
        final String setting = mPowerUsageFeatureProvider.isSmartBatterySupported()
                ? Settings.Global.ADAPTIVE_BATTERY_MANAGEMENT_ENABLED
                : Settings.Global.APP_AUTO_RESTRICTION_ENABLED;
        final boolean featureOn =
                Settings.Global.getInt(mContext.getContentResolver(), setting, ON) == ON;

        updateSummary(preference, featureOn, num);
    }

    @VisibleForTesting
    void updateSummary(Preference preference, boolean featureOn, int num) {
        if (num > 0) {
            preference.setSummary(mContext.getResources().getQuantityString(
                    R.plurals.battery_manager_app_restricted, num, num));
        } else if (featureOn) {
            preference.setSummary(R.string.battery_manager_on);
        } else {
            preference.setSummary(R.string.battery_manager_off);
        }
    }
}
