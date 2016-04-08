/*
 * Copyright (C) 2010 The Android Open Source Project
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
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.util.AttributeSet;
import android.widget.TextView;
import com.android.internal.os.BatteryStatsHelper;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settingslib.BatteryInfo;
import com.android.settingslib.graph.UsageView;

/**
 * Custom preference for displaying power consumption as a bar and an icon on the left for the
 * subsystem/app type.
 *
 */
public class BatteryHistoryPreference extends Preference {

    protected static final String BATTERY_HISTORY_FILE = "tmp_bat_history.bin";

    private BatteryStatsHelper mHelper;
    private BatteryInfo mBatteryInfo;

    public BatteryHistoryPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.battery_usage_graph);
        setSelectable(true);
    }

    @Override
    public void performClick() {
        mHelper.storeStatsHistoryInFile(BATTERY_HISTORY_FILE);
        Bundle args = new Bundle();
        args.putString(BatteryHistoryDetail.EXTRA_STATS, BATTERY_HISTORY_FILE);
        args.putParcelable(BatteryHistoryDetail.EXTRA_BROADCAST, mHelper.getBatteryBroadcast());
        Utils.startWithFragment(getContext(), BatteryHistoryDetail.class.getName(), args,
                null, 0, R.string.history_details_title, null);
    }

    public void setStats(BatteryStatsHelper batteryStats) {
        mHelper = batteryStats;
        final long elapsedRealtimeUs = SystemClock.elapsedRealtime() * 1000;
        mBatteryInfo = BatteryInfo.getBatteryInfo(getContext(), batteryStats.getBatteryBroadcast(),
                batteryStats.getStats(), elapsedRealtimeUs);
        notifyChanged();
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder view) {
        super.onBindViewHolder(view);
        if (mBatteryInfo == null) {
            return;
        }
        view.itemView.setClickable(true);
        view.setDividerAllowedAbove(true);
        ((TextView) view.findViewById(R.id.charge)).setText(mBatteryInfo.batteryPercentString);
        ((TextView) view.findViewById(R.id.estimation)).setText(mBatteryInfo.remainingLabel);
        UsageView usageView = (UsageView) view.findViewById(R.id.battery_usage);
        usageView.findViewById(R.id.label_group).setAlpha(.7f);
        mBatteryInfo.bindHistory(usageView);
    }
}
