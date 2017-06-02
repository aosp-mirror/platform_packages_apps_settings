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

import android.content.Intent;
import android.os.BatteryStats;
import android.os.BatteryStats.HistoryItem;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.internal.os.BatteryStatsHelper;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.fuelgauge.BatteryActiveView.BatteryActiveProvider;
import com.android.settings.graph.UsageView;

public class BatteryHistoryDetail extends SettingsPreferenceFragment {
    public static final String EXTRA_STATS = "stats";
    public static final String EXTRA_BROADCAST = "broadcast";
    public static final String BATTERY_HISTORY_FILE = "tmp_bat_history.bin";

    private BatteryStats mStats;
    private Intent mBatteryBroadcast;

    private BatteryFlagParser mChargingParser;
    private BatteryFlagParser mScreenOn;
    private BatteryFlagParser mGpsParser;
    private BatteryFlagParser mFlashlightParser;
    private BatteryFlagParser mCameraParser;
    private BatteryWifiParser mWifiParser;
    private BatteryFlagParser mCpuParser;
    private BatteryCellParser mPhoneParser;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        String histFile = getArguments().getString(EXTRA_STATS);
        mStats = BatteryStatsHelper.statsFromFile(getActivity(), histFile);
        mBatteryBroadcast = getArguments().getParcelable(EXTRA_BROADCAST);

        TypedValue value = new TypedValue();
        getContext().getTheme().resolveAttribute(android.R.attr.colorAccent, value, true);
        int accentColor = getContext().getColor(value.resourceId);

        mChargingParser = new BatteryFlagParser(accentColor, false,
                HistoryItem.STATE_BATTERY_PLUGGED_FLAG);
        mScreenOn = new BatteryFlagParser(accentColor, false,
                HistoryItem.STATE_SCREEN_ON_FLAG);
        mGpsParser = new BatteryFlagParser(accentColor, false,
                HistoryItem.STATE_GPS_ON_FLAG);
        mFlashlightParser = new BatteryFlagParser(accentColor, true,
                HistoryItem.STATE2_FLASHLIGHT_FLAG);
        mCameraParser = new BatteryFlagParser(accentColor, true,
                HistoryItem.STATE2_CAMERA_FLAG);
        mWifiParser = new BatteryWifiParser(accentColor);
        mCpuParser = new BatteryFlagParser(accentColor, false,
                HistoryItem.STATE_CPU_RUNNING_FLAG);
        mPhoneParser = new BatteryCellParser();
        setHasOptionsMenu(true);
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.battery_history_detail, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        updateEverything();
    }

    private void updateEverything() {
        BatteryInfo.getBatteryInfo(getContext(), info -> {
            final View view = getView();
            info.bindHistory((UsageView) view.findViewById(R.id.battery_usage), mChargingParser,
                    mScreenOn, mGpsParser, mFlashlightParser, mCameraParser, mWifiParser,
                    mCpuParser, mPhoneParser);
            ((TextView) view.findViewById(R.id.charge)).setText(info.batteryPercentString);
            ((TextView) view.findViewById(R.id.estimation)).setText(info.remainingLabel);

            bindData(mChargingParser, R.string.battery_stats_charging_label, R.id.charging_group);
            bindData(mScreenOn, R.string.battery_stats_screen_on_label, R.id.screen_on_group);
            bindData(mGpsParser, R.string.battery_stats_gps_on_label, R.id.gps_group);
            bindData(mFlashlightParser, R.string.battery_stats_flashlight_on_label,
                    R.id.flashlight_group);
            bindData(mCameraParser, R.string.battery_stats_camera_on_label, R.id.camera_group);
            bindData(mWifiParser, R.string.battery_stats_wifi_running_label, R.id.wifi_group);
            bindData(mCpuParser, R.string.battery_stats_wake_lock_label, R.id.cpu_group);
            bindData(mPhoneParser, R.string.battery_stats_phone_signal_label,
                    R.id.cell_network_group);
        }, mStats, false /* shortString */);
    }

    private void bindData(BatteryActiveProvider provider, int label, int groupId) {
        View group = getView().findViewById(groupId);
        group.setVisibility(provider.hasData() ? View.VISIBLE : View.GONE);
        ((TextView) group.findViewById(android.R.id.title)).setText(label);
        ((BatteryActiveView) group.findViewById(R.id.battery_active)).setProvider(provider);
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.FUELGAUGE_BATTERY_HISTORY_DETAIL;
    }
}
