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
 *
 *
 */

package com.android.settings.fuelgauge;

import android.app.AppOpsManager;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.os.UserManager;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.core.InstrumentedPreferenceFragment;
import com.android.settings.fuelgauge.batterytip.AppInfo;
import com.android.settings.fuelgauge.batterytip.BatteryTipUtils;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.utils.StringUtil;

import java.util.List;

/** Controller to change and update the smart battery toggle */
public class RestrictAppPreferenceController extends BasePreferenceController {
    @VisibleForTesting static final String KEY_RESTRICT_APP = "restricted_app";

    @VisibleForTesting List<AppInfo> mAppInfos;
    private AppOpsManager mAppOpsManager;
    private InstrumentedPreferenceFragment mPreferenceFragment;
    private UserManager mUserManager;
    private boolean mEnableAppBatteryUsagePage;

    public RestrictAppPreferenceController(Context context) {
        super(context, KEY_RESTRICT_APP);
        mAppOpsManager = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
        mUserManager = context.getSystemService(UserManager.class);
        mAppInfos = BatteryTipUtils.getRestrictedAppsList(mAppOpsManager, mUserManager);
        mEnableAppBatteryUsagePage =
                mContext.getResources().getBoolean(R.bool.config_app_battery_usage_list_enabled);
    }

    public RestrictAppPreferenceController(InstrumentedPreferenceFragment preferenceFragment) {
        this(preferenceFragment.getContext());
        mPreferenceFragment = preferenceFragment;
    }

    @Override
    public int getAvailabilityStatus() {
        return mAppInfos.size() > 0 && !mEnableAppBatteryUsagePage
                ? AVAILABLE
                : CONDITIONALLY_UNAVAILABLE;
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        mAppInfos = BatteryTipUtils.getRestrictedAppsList(mAppOpsManager, mUserManager);
        final int num = mAppInfos.size();
        // Fragment change RestrictedAppsList after onPause(), UI needs to be updated in onResume()
        preference.setVisible(num > 0);
        preference.setSummary(
                StringUtil.getIcuPluralsString(mContext, num, R.string.restricted_app_summary));
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (getPreferenceKey().equals(preference.getKey())) {
            // start fragment
            RestrictedAppDetails.startRestrictedAppDetails(mPreferenceFragment, mAppInfos);
            FeatureFactory.getFeatureFactory()
                    .getMetricsFeatureProvider()
                    .action(mContext, SettingsEnums.OPEN_APP_RESTRICTED_LIST);
            return true;
        }

        return super.handlePreferenceTreeClick(preference);
    }
}
