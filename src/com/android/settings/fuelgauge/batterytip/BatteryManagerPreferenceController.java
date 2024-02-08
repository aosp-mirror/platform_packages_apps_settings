/*
 * Copyright (C) 2024 The Android Open Source Project
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

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.fuelgauge.PowerUsageFeatureProvider;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.utils.StringUtil;

/** Preference controller to control the battery manager */
public class BatteryManagerPreferenceController extends BasePreferenceController {
    private static final String KEY_BATTERY_MANAGER = "smart_battery_manager";

    private PowerUsageFeatureProvider mPowerUsageFeatureProvider;
    private AppOpsManager mAppOpsManager;
    private UserManager mUserManager;
    private boolean mEnableAppBatteryUsagePage;

    public BatteryManagerPreferenceController(Context context) {
        super(context, KEY_BATTERY_MANAGER);
        mPowerUsageFeatureProvider = FeatureFactory.getFeatureFactory()
                .getPowerUsageFeatureProvider();
        mAppOpsManager = context.getSystemService(AppOpsManager.class);
        mUserManager = context.getSystemService(UserManager.class);
        mEnableAppBatteryUsagePage =
                mContext.getResources().getBoolean(R.bool.config_app_battery_usage_list_enabled);
    }

    @Override
    public int getAvailabilityStatus() {
        if (!mPowerUsageFeatureProvider.isBatteryManagerSupported()) {
            return UNSUPPORTED_ON_DEVICE;
        }
        if (!mContext.getResources().getBoolean(R.bool.config_battery_manager_consider_ac)) {
            return AVAILABLE_UNSEARCHABLE;
        }
        return mPowerUsageFeatureProvider.isAdaptiveChargingSupported()
                ? AVAILABLE_UNSEARCHABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        if (!mEnableAppBatteryUsagePage) {
            final int num = BatteryTipUtils.getRestrictedAppsList(mAppOpsManager,
                    mUserManager).size();
            updateSummary(preference, num);
        }
    }

    @VisibleForTesting
    void updateSummary(Preference preference, int num) {
        if (num > 0) {
            preference.setSummary(StringUtil.getIcuPluralsString(mContext, num,
                    R.string.battery_manager_app_restricted));
        } else {
            preference.setSummary(
                    mPowerUsageFeatureProvider.isAdaptiveChargingSupported()
                            ? R.string.battery_manager_summary
                            : R.string.battery_manager_summary_unsupported);
        }
    }
}
