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

package com.android.settings.fuelgauge.batterytip.actions;

import android.app.settings.SettingsEnums;

import androidx.annotation.VisibleForTesting;

import com.android.settings.core.InstrumentedPreferenceFragment;
import com.android.settings.fuelgauge.RestrictedAppDetails;
import com.android.settings.fuelgauge.batterytip.AnomalyDatabaseHelper;
import com.android.settings.fuelgauge.batterytip.AppInfo;
import com.android.settings.fuelgauge.batterytip.BatteryDatabaseManager;
import com.android.settings.fuelgauge.batterytip.tips.RestrictAppTip;
import com.android.settingslib.utils.ThreadUtils;

import java.util.List;

/**
 * Action to open the {@link com.android.settings.fuelgauge.RestrictedAppDetails}
 */
public class OpenRestrictAppFragmentAction extends BatteryTipAction {
    private final RestrictAppTip mRestrictAppTip;
    private final InstrumentedPreferenceFragment mFragment;
    @VisibleForTesting
    BatteryDatabaseManager mBatteryDatabaseManager;

    public OpenRestrictAppFragmentAction(InstrumentedPreferenceFragment fragment,
            RestrictAppTip tip) {
        super(fragment.getContext());
        mFragment = fragment;
        mRestrictAppTip = tip;
        mBatteryDatabaseManager = BatteryDatabaseManager.getInstance(mContext);
    }

    /**
     * Handle the action when user clicks positive button
     */
    @Override
    public void handlePositiveAction(int metricsKey) {
        mMetricsFeatureProvider.action(mContext,
                SettingsEnums.ACTION_TIP_OPEN_APP_RESTRICTION_PAGE, metricsKey);
        final List<AppInfo> mAppInfos = mRestrictAppTip.getRestrictAppList();
        RestrictedAppDetails.startRestrictedAppDetails(mFragment, mAppInfos);

        // Mark all the anomalies as handled, so it won't show up again.
        ThreadUtils.postOnBackgroundThread(() -> mBatteryDatabaseManager.updateAnomalies(mAppInfos,
                AnomalyDatabaseHelper.State.HANDLED));
    }
}
