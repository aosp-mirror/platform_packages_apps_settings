/*
 * Copyright (C) 2017 The Android Open Source Project
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

import com.android.settings.fuelgauge.batterytip.BatteryTipPolicy;
import com.android.settings.fuelgauge.batterytip.tips.BatteryTip;
import com.android.settings.fuelgauge.batterytip.tips.SummaryTip;

/**
 * Detector whether to show summary tip. This detector should be executed as the last
 * {@link BatteryTipDetector} since it need the most up-to-date {@code visibleTips}
 */
public class SummaryDetector implements BatteryTipDetector {
    private BatteryTipPolicy mPolicy;
    private long mAverageTimeMs;

    public SummaryDetector(BatteryTipPolicy policy, long averageTimeMs) {
        mPolicy = policy;
        mAverageTimeMs = averageTimeMs;
    }

    @Override
    public BatteryTip detect() {
        // Show it if there is no other tips shown
        final int state = mPolicy.summaryEnabled
                ? BatteryTip.StateType.NEW
                : BatteryTip.StateType.INVISIBLE;
        return new SummaryTip(state, mAverageTimeMs);
    }
}
