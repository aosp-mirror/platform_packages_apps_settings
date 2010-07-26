/*
 * Copyright (C) 2009 The Android Open Source Project
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

import android.app.Activity;
import android.os.Bundle;
import android.os.Parcel;

import com.android.internal.os.BatteryStatsImpl;
import com.android.settings.R;

public class BatteryHistoryDetail extends Activity {
    public static final String EXTRA_STATS = "stats";

    private BatteryStatsImpl mStats;

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        byte[] data = getIntent().getByteArrayExtra(EXTRA_STATS);
        Parcel parcel = Parcel.obtain();
        parcel.unmarshall(data, 0, data.length);
        parcel.setDataPosition(0);
        setContentView(R.layout.preference_batteryhistory);
        mStats = com.android.internal.os.BatteryStatsImpl.CREATOR
                .createFromParcel(parcel);
        BatteryHistoryChart chart = (BatteryHistoryChart)findViewById(
                R.id.battery_history_chart);
        chart.setStats(mStats);
    }
}
