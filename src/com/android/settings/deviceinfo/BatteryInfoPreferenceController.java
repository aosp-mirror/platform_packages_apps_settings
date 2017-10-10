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

package com.android.settings.deviceinfo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.annotation.VisibleForTesting;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;

import com.android.settings.Utils;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;

public class BatteryInfoPreferenceController extends AbstractPreferenceController
        implements PreferenceControllerMixin, LifecycleObserver, OnStart, OnStop {

    @VisibleForTesting
    static final IntentFilter BATTERY_INFO_RECEIVER_INTENT_FILTER =
            new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
    @VisibleForTesting
    static final String KEY_BATTERY_STATUS = "battery_status";
    @VisibleForTesting
    static final String KEY_BATTERY_LEVEL = "battery_level";

    @VisibleForTesting
    BroadcastReceiver mBatteryInfoReceiver;
    private Preference mBatteryStatus;
    private Preference mBatteryLevel;


    public BatteryInfoPreferenceController(Context context, Lifecycle lifecycle) {
        super(context);
        mBatteryInfoReceiver = new BatteryInfoReceiver(context);
        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getPreferenceKey() {
        return null;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mBatteryLevel = screen.findPreference(KEY_BATTERY_LEVEL);
        mBatteryStatus = screen.findPreference(KEY_BATTERY_STATUS);
    }

    @Override
    public void onStart() {
        mContext.registerReceiver(mBatteryInfoReceiver, BATTERY_INFO_RECEIVER_INTENT_FILTER);
    }

    @Override
    public void onStop() {
        mContext.unregisterReceiver(mBatteryInfoReceiver);
    }

    private class BatteryInfoReceiver extends BroadcastReceiver {

        private final Context mContext;

        public BatteryInfoReceiver(Context context) {
            mContext = context;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Intent.ACTION_BATTERY_CHANGED.equals(action)) {
                mBatteryLevel.setSummary(Utils.getBatteryPercentage(intent));
                mBatteryStatus.setSummary(Utils.getBatteryStatus(mContext.getResources(), intent));
            }
        }
    }
}
