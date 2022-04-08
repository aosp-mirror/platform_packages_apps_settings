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
import android.icu.text.NumberFormat;
import android.os.BatteryManager;
import android.os.PowerManager;
import android.text.TextUtils;
import android.widget.TextView;

import androidx.annotation.VisibleForTesting;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.widget.EntityHeaderController;
import com.android.settingslib.Utils;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.widget.LayoutPreference;

/**
 * Controller that update the battery header view
 */
public class BatteryHeaderPreferenceController extends BasePreferenceController
        implements PreferenceControllerMixin, LifecycleObserver, OnStart {
    @VisibleForTesting
    static final String KEY_BATTERY_HEADER = "battery_header";

    @VisibleForTesting
    BatteryMeterView mBatteryMeterView;
    @VisibleForTesting
    TextView mBatteryPercentText;
    @VisibleForTesting
    TextView mSummary1;

    private Activity mActivity;
    private PreferenceFragmentCompat mHost;
    private Lifecycle mLifecycle;
    private final PowerManager mPowerManager;

    private LayoutPreference mBatteryLayoutPref;

    public BatteryHeaderPreferenceController(Context context, String key) {
        super(context, key);
        mPowerManager = context.getSystemService(PowerManager.class);
    }

    public void setActivity(Activity activity) {
        mActivity = activity;
    }

    public void setFragment(PreferenceFragmentCompat fragment) {
        mHost = fragment;
    }

    public void setLifecycle(Lifecycle lifecycle) {
        mLifecycle = lifecycle;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mBatteryLayoutPref = screen.findPreference(getPreferenceKey());
        mBatteryMeterView = mBatteryLayoutPref
                .findViewById(R.id.battery_header_icon);
        mBatteryPercentText = mBatteryLayoutPref.findViewById(R.id.battery_percent);
        mSummary1 = mBatteryLayoutPref.findViewById(R.id.summary1);

        quickUpdateHeaderPreference();
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE_UNSEARCHABLE;
    }

    @Override
    public void onStart() {
        EntityHeaderController.newInstance(mActivity, mHost,
                mBatteryLayoutPref.findViewById(R.id.battery_entity_header))
                .setRecyclerView(mHost.getListView(), mLifecycle)
                .styleActionBar(mActivity);
    }

    public void updateHeaderPreference(BatteryInfo info) {
        mBatteryPercentText.setText(formatBatteryPercentageText(info.batteryLevel));
        if (info.remainingLabel == null) {
            mSummary1.setText(info.statusLabel);
        } else {
            mSummary1.setText(info.remainingLabel);
        }

        mBatteryMeterView.setBatteryLevel(info.batteryLevel);
        mBatteryMeterView.setCharging(!info.discharging);
        mBatteryMeterView.setPowerSave(mPowerManager.isPowerSaveMode());
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
        mBatteryMeterView.setPowerSave(mPowerManager.isPowerSaveMode());
        mBatteryPercentText.setText(formatBatteryPercentageText(batteryLevel));
    }

    private CharSequence formatBatteryPercentageText(int batteryLevel) {
        return TextUtils.expandTemplate(mContext.getText(R.string.battery_header_title_alternate),
                NumberFormat.getIntegerInstance().format(batteryLevel));
    }
}
