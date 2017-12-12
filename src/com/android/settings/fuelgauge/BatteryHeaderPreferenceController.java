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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.support.annotation.VisibleForTesting;
import android.support.v14.preference.PreferenceFragment;
import android.support.v7.preference.PreferenceScreen;
import android.widget.TextView;

import com.android.settings.R;
import com.android.settings.applications.LayoutPreference;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.widget.EntityHeaderController;
import com.android.settingslib.Utils;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;

/**
 * Controller that update the battery header view
 */
public class BatteryHeaderPreferenceController extends AbstractPreferenceController
        implements PreferenceControllerMixin, LifecycleObserver, OnStart {
    @VisibleForTesting
    static final String KEY_BATTERY_HEADER = "battery_header";

    @VisibleForTesting
    BatteryMeterView mBatteryMeterView;
    @VisibleForTesting
    TextView mBatteryPercentText;
    @VisibleForTesting
    TextView mSummary1;
    @VisibleForTesting
    TextView mSummary2;

    private final Activity mActivity;
    private final PreferenceFragment mHost;
    private final Lifecycle mLifecycle;

    private LayoutPreference mBatteryLayoutPref;

    public BatteryHeaderPreferenceController(Context context, Activity activity,
            PreferenceFragment host, Lifecycle lifecycle) {
        super(context);
        mActivity = activity;
        mHost = host;
        mLifecycle = lifecycle;
        if (mLifecycle != null) {
            mLifecycle.addObserver(this);
        }
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mBatteryLayoutPref = (LayoutPreference) screen.findPreference(KEY_BATTERY_HEADER);
        mBatteryMeterView = (BatteryMeterView) mBatteryLayoutPref
                .findViewById(R.id.battery_header_icon);
        mBatteryPercentText = mBatteryLayoutPref.findViewById(R.id.battery_percent);
        mSummary1 = mBatteryLayoutPref.findViewById(R.id.summary1);
        mSummary2 = mBatteryLayoutPref.findViewById(R.id.summary2);

        quickUpdateHeaderPreference();
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getPreferenceKey() {
        return KEY_BATTERY_HEADER;
    }

    @Override
    public void onStart() {
        EntityHeaderController.newInstance(mActivity, mHost,
                mBatteryLayoutPref.findViewById(R.id.battery_entity_header))
                .setRecyclerView(mHost.getListView(), mLifecycle)
                .styleActionBar(mActivity);
    }

    public void updateHeaderPreference(BatteryInfo info) {
        mBatteryPercentText.setText(Utils.formatPercentage(info.batteryLevel));
        if (info.remainingLabel == null) {
            mSummary1.setText(info.statusLabel);
        } else {
            mSummary1.setText(info.remainingLabel);
        }
        // Clear this just to be sure we don't get UI jank on re-entering this view from another
        // activity.
        mSummary2.setText("");

        mBatteryMeterView.setBatteryLevel(info.batteryLevel);
        mBatteryMeterView.setCharging(!info.discharging);
    }

    public void quickUpdateHeaderPreference() {
        Intent batteryBroadcast = mContext.registerReceiver(null,
                new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        final int batteryLevel = Utils.getBatteryLevel(batteryBroadcast);
        final boolean discharging =
                batteryBroadcast.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) == 0;

        // Set battery level and charging status
        mBatteryMeterView.setBatteryLevel(batteryLevel);
        mBatteryMeterView.setCharging(!discharging);
        mBatteryPercentText.setText(Utils.formatPercentage(batteryLevel));

        // clear all the summaries
        mSummary1.setText("");
        mSummary2.setText("");
    }
}
