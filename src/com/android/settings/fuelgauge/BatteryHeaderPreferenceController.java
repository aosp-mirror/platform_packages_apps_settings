/*
 * Copyright (C) 2017 The Android Open Source Project
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

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.annotation.VisibleForTesting;
import android.support.v7.preference.PreferenceScreen;
import android.widget.TextView;

import com.android.settings.R;
import com.android.settings.applications.LayoutPreference;
import com.android.settings.core.PreferenceController;
import com.android.settingslib.BatteryInfo;
import com.android.settingslib.Utils;

/**
 * Controller that update the battery header view
 */
public class BatteryHeaderPreferenceController extends PreferenceController {
    @VisibleForTesting
    static final String KEY_BATTERY_HEADER = "battery_header";
    @VisibleForTesting
    BatteryMeterView mBatteryMeterView;
    @VisibleForTesting
    TextView mTimeText;
    @VisibleForTesting
    TextView mSummary;

    private LayoutPreference mBatteryLayoutPref;

    public BatteryHeaderPreferenceController(Context context) {
        super(context);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);

        mBatteryLayoutPref = (LayoutPreference) screen.findPreference(KEY_BATTERY_HEADER);
        mBatteryMeterView = (BatteryMeterView) mBatteryLayoutPref
                .findViewById(R.id.battery_header_icon);
        mTimeText = (TextView) mBatteryLayoutPref.findViewById(R.id.battery_percent);
        mSummary = (TextView) mBatteryLayoutPref.findViewById(R.id.summary1);

        Intent batteryBroadcast = mContext.registerReceiver(null,
                new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        final int batteryLevel = Utils.getBatteryLevel(batteryBroadcast);

        mBatteryMeterView.setBatteryLevel(batteryLevel);
        mTimeText.setText(Utils.formatPercentage(batteryLevel));
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getPreferenceKey() {
        return KEY_BATTERY_HEADER;
    }

    public void updateHeaderPreference(BatteryInfo info) {
        mTimeText.setText(Utils.formatPercentage(info.batteryLevel));
        if (info.remainingLabel == null) {
            mSummary.setText(info.statusLabel);
        } else {
            mSummary.setText(info.remainingLabel);
        }

        mBatteryMeterView.setBatteryLevel(info.batteryLevel);
        mBatteryMeterView.setCharging(!info.discharging);
    }
}
