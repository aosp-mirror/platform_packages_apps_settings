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

package com.android.settings.fuelgauge.batterytip.tips;

import android.app.Dialog;
import android.content.Context;

import com.android.settings.R;

import java.util.List;

/**
 * Tip to show general summary about battery life
 */
public class HighUsageTip extends BatteryTip {

    private final CharSequence mScreenTimeText;
    private final List<HighUsageApp> mHighUsageAppList;

    public HighUsageTip(CharSequence screenTimeText, List<HighUsageApp> appList) {
        mShowDialog = true;
        mScreenTimeText = screenTimeText;
        mType = TipType.HIGH_DEVICE_USAGE;
        mHighUsageAppList = appList;
        mState = appList.isEmpty() ? StateType.INVISIBLE : StateType.NEW;
    }

    @Override
    public CharSequence getTitle(Context context) {
        return context.getString(R.string.battery_tip_high_usage_title);
    }

    @Override
    public CharSequence getSummary(Context context) {
        return context.getString(R.string.battery_tip_high_usage_summary, mScreenTimeText);
    }

    @Override
    public int getIconId() {
        return R.drawable.ic_perm_device_information_red_24dp;
    }

    @Override
    public void updateState(BatteryTip tip) {
        mState = tip.mState;
    }

    @Override
    public void action() {
        // do nothing
    }

    @Override
    public Dialog buildDialog() {
        //TODO(b/70570352): build the real dialog
        return null;
    }

    /**
     * Class representing app with high screen usage
     */
    public static class HighUsageApp implements Comparable<HighUsageApp> {
        public final String packageName;
        public final long screenOnTimeMs;

        public HighUsageApp(String packageName, long screenOnTimeMs) {
            this.packageName = packageName;
            this.screenOnTimeMs = screenOnTimeMs;
        }

        @Override
        public int compareTo(HighUsageApp o) {
            return Long.compare(screenOnTimeMs, o.screenOnTimeMs);
        }
    }
}
