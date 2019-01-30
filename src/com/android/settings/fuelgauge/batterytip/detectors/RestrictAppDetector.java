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

package com.android.settings.fuelgauge.batterytip.detectors;

import static com.android.settings.Utils.SETTINGS_PACKAGE_NAME;

import android.content.Context;

import androidx.annotation.VisibleForTesting;

import com.android.settings.fuelgauge.batterytip.AnomalyDatabaseHelper;
import com.android.settings.fuelgauge.batterytip.AppInfo;
import com.android.settings.fuelgauge.batterytip.BatteryDatabaseManager;
import com.android.settings.fuelgauge.batterytip.BatteryTipPolicy;
import com.android.settings.fuelgauge.batterytip.BatteryTipUtils;
import com.android.settings.fuelgauge.batterytip.tips.AppLabelPredicate;
import com.android.settings.fuelgauge.batterytip.tips.AppRestrictionPredicate;
import com.android.settings.fuelgauge.batterytip.tips.BatteryTip;
import com.android.settings.fuelgauge.batterytip.tips.RestrictAppTip;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Detector whether to show summary tip. This detector should be executed as the last
 * {@link BatteryTipDetector} since it need the most up-to-date {@code visibleTips}
 */
public class RestrictAppDetector implements BatteryTipDetector {
    @VisibleForTesting
    static final boolean USE_FAKE_DATA = false;
    private BatteryTipPolicy mPolicy;
    @VisibleForTesting
    BatteryDatabaseManager mBatteryDatabaseManager;
    private Context mContext;

    private AppRestrictionPredicate mAppRestrictionPredicate;
    private AppLabelPredicate mAppLabelPredicate;

    public RestrictAppDetector(Context context, BatteryTipPolicy policy) {
        mContext = context;
        mPolicy = policy;
        mBatteryDatabaseManager = BatteryDatabaseManager.getInstance(context);
        mAppRestrictionPredicate = AppRestrictionPredicate.getInstance(context);
        mAppLabelPredicate = AppLabelPredicate.getInstance(context);
    }

    @Override
    public BatteryTip detect() {
        if (USE_FAKE_DATA) {
            return getFakeData();
        }
        if (mPolicy.appRestrictionEnabled) {
            final long oneDayBeforeMs = System.currentTimeMillis()
                    - TimeUnit.HOURS.toMillis(mPolicy.appRestrictionActiveHour);
            final List<AppInfo> highUsageApps = BatteryTipUtils.detectAnomalies(mContext,
                    oneDayBeforeMs);
            if (!highUsageApps.isEmpty()) {
                // If there are new anomalies, show them
                return new RestrictAppTip(BatteryTip.StateType.NEW, highUsageApps);
            } else {
                // Otherwise, show auto-handled one if it exists
                final List<AppInfo> autoHandledApps = mBatteryDatabaseManager.queryAllAnomalies(
                        oneDayBeforeMs, AnomalyDatabaseHelper.State.AUTO_HANDLED);
                // Remove it if it doesn't have label or unrestricted
                autoHandledApps.removeIf(mAppLabelPredicate.or(mAppRestrictionPredicate.negate()));
                return new RestrictAppTip(autoHandledApps.isEmpty() ? BatteryTip.StateType.INVISIBLE
                        : BatteryTip.StateType.HANDLED, autoHandledApps);
            }
        } else {
            return new RestrictAppTip(BatteryTip.StateType.INVISIBLE, new ArrayList<>());
        }
    }

    private BatteryTip getFakeData() {
        final List<AppInfo> highUsageApps = new ArrayList<>();
        highUsageApps.add(new AppInfo.Builder()
                .setPackageName(SETTINGS_PACKAGE_NAME)
                .build());
        return new RestrictAppTip(BatteryTip.StateType.NEW, highUsageApps);
    }
}
