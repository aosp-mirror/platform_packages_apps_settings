/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.android.settings.datausage;

import android.graphics.Color;

/**
 * Mock data plan usage data.
 */
@Deprecated // This class is only needed because we don't have working API yet.
final class MockDataPlanUsage {
    static final MockDataPlanUsage[] DATA_PLAN_USAGES = new MockDataPlanUsage[3];
    static final String SYNC_TIME = "Today 12:24pm";
    final String mUsage;
    final int mUsageTextColor;
    final String mName;
    final double mPercentageUsage;
    final int mMeterBackgroundColor;
    final int mMeterConsumedColor;
    final String mDescription;

    private MockDataPlanUsage(String usage, int usageTextColor, String name,
            double percentageUsage, int meterBackgroundColor, int meterConsumedColor,
            String description) {
        mUsage = usage;
        mUsageTextColor = usageTextColor;
        mName = name;
        mPercentageUsage = percentageUsage;
        mMeterBackgroundColor = meterBackgroundColor;
        mMeterConsumedColor = meterConsumedColor;
        mDescription = description;
    }

    static MockDataPlanUsage[] getDataPlanUsage() {
        DATA_PLAN_USAGES[0] = new MockDataPlanUsage("100 MB and 14 days left",
                Color.parseColor("#FF5C94F1"), "GigaMaxLite / 1GB", 0.27D,
                Color.parseColor("#FFDBDCDC"), Color.parseColor("#FF5C94F1"),
                "Premium plan from Telekomsel");

        DATA_PLAN_USAGES[1] = new MockDataPlanUsage("1.25 GB and 14 days left",
                Color.parseColor("#FF673AB7"), "GigaMaxLite 4G / 5GB", 0.47D,
                Color.parseColor("#FFDBDCDC"), Color.parseColor("#FF673AB7"),
                "Plenty of 4G data");

        DATA_PLAN_USAGES[2] = new MockDataPlanUsage("700 MB and 14 days left",
                Color.parseColor("#FF4CAF50"), "GigaMaxLite Video / 7GB", 0.67D,
                Color.parseColor("#FFDBDCDC"), Color.parseColor("#FF4CAF50"),
                "Use certain video apps for free");
        return DATA_PLAN_USAGES;
    }
}
