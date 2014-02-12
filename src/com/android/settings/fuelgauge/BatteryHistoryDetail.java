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

import android.app.Fragment;
import android.os.Bundle;
import android.os.Parcel;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.internal.os.BatteryStatsImpl;
import com.android.settings.R;

public class BatteryHistoryDetail extends Fragment {
    public static final String EXTRA_STATS = "stats";
    public static final String EXTRA_DOCK_STATS = "dockstats";

    private BatteryStatsImpl mStats;
    private BatteryStatsImpl mDockStats;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        byte[] data = getArguments().getByteArray(EXTRA_STATS);
        byte[] dockData = getArguments().getByteArray(EXTRA_DOCK_STATS);
        Parcel parcel = Parcel.obtain();
        parcel.unmarshall(data, 0, data.length);
        parcel.setDataPosition(0);
        mStats = com.android.internal.os.BatteryStatsImpl.CREATOR
                .createFromParcel(parcel);
        if (dockData != null) {
            parcel = Parcel.obtain();
            parcel.unmarshall(dockData, 0, dockData.length);
            parcel.setDataPosition(0);
            mDockStats = com.android.internal.os.DockBatteryStatsImpl.CREATOR
                    .createFromParcel(parcel);
        }
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.preference_batteryhistory, null);
        BatteryHistoryChart chart = (BatteryHistoryChart)view.findViewById(
                R.id.battery_history_chart);
        chart.setStats(mStats);
        chart.setDockStats(mDockStats);
        return view;
    }
}
