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

package com.android.settings.fuelgauge;

import android.content.Context;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;

public class TopLevelBatteryPreferenceController extends BasePreferenceController implements
        LifecycleObserver, OnStart, OnStop {

    @VisibleForTesting
    boolean mIsBatteryPresent = true;
    private final BatteryBroadcastReceiver mBatteryBroadcastReceiver;
    private Preference mPreference;
    private BatteryInfo mBatteryInfo;

    public TopLevelBatteryPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mBatteryBroadcastReceiver = new BatteryBroadcastReceiver(mContext);
        mBatteryBroadcastReceiver.setBatteryChangedListener(type -> {
            if (type == BatteryBroadcastReceiver.BatteryUpdateType.BATTERY_NOT_PRESENT) {
                mIsBatteryPresent = false;
            }
            BatteryInfo.getBatteryInfo(mContext, info -> {
                mBatteryInfo = info;
                updateState(mPreference);
            }, true /* shortString */);
        });
    }

    @Override
    public int getAvailabilityStatus() {
        return mContext.getResources().getBoolean(R.bool.config_show_top_level_battery)
                ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
    }

    @Override
    public void onStart() {
        mBatteryBroadcastReceiver.register();
    }

    @Override
    public void onStop() {
        mBatteryBroadcastReceiver.unRegister();
    }

    @Override
    public CharSequence getSummary() {
        // Display help message if battery is not present.
        if (!mIsBatteryPresent) {
            return mContext.getText(R.string.battery_missing_message);
        }
        return getDashboardLabel(mContext, mBatteryInfo);
    }

    static CharSequence getDashboardLabel(Context context, BatteryInfo info) {
        if (info == null || context == null) {
            return null;
        }
        CharSequence label;
        if (!info.discharging && info.chargeLabel != null) {
            label = info.chargeLabel;
        } else if (info.remainingLabel == null) {
            label = info.batteryPercentString;
        } else {
            label = context.getString(R.string.power_remaining_settings_home_page,
                    info.batteryPercentString,
                    info.remainingLabel);
        }
        return label;
    }
}
