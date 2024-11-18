/*
 * Copyright (C) 2024 The Android Open Source Project
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

import static com.android.settings.fuelgauge.BatteryBroadcastReceiver.BatteryUpdateType.BATTERY_NOT_PRESENT;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.BasePreferenceController;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.Utils;
import com.android.settingslib.widget.UsageProgressBarPreference;

// LINT.IfChange
/** Controller that update the battery header view */
public class BatteryHeaderPreferenceController extends BasePreferenceController
        implements PreferenceControllerMixin, LifecycleEventObserver {
    private static final String TAG = "BatteryHeaderPreferenceController";
    private static final int BATTERY_MAX_LEVEL = 100;

    @Nullable @VisibleForTesting BatteryBroadcastReceiver mBatteryBroadcastReceiver;
    @Nullable @VisibleForTesting UsageProgressBarPreference mBatteryUsageProgressBarPreference;

    public BatteryHeaderPreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public void onStateChanged(@NonNull LifecycleOwner lifecycleOwner,
            @NonNull Lifecycle.Event event) {
        switch (event) {
            case ON_CREATE:
                mBatteryBroadcastReceiver = new BatteryBroadcastReceiver(mContext);
                mBatteryBroadcastReceiver.setBatteryChangedListener(
                        type -> {
                            if (type != BATTERY_NOT_PRESENT) {
                                quickUpdateHeaderPreference();
                            }
                        });
                break;
            case ON_START:
                if (mBatteryBroadcastReceiver != null) {
                    mBatteryBroadcastReceiver.register();
                }
                break;
            case ON_STOP:
                if (mBatteryBroadcastReceiver != null) {
                    mBatteryBroadcastReceiver.unRegister();
                }
                break;
            default:
                break;
        }
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mBatteryUsageProgressBarPreference = screen.findPreference(getPreferenceKey());
        // Hide the bottom summary from the progress bar.
        mBatteryUsageProgressBarPreference.setBottomSummary("");

        if (com.android.settings.Utils.isBatteryPresent(mContext)) {
            quickUpdateHeaderPreference();
        } else {
            mBatteryUsageProgressBarPreference.setVisible(false);
        }
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE_UNSEARCHABLE;
    }

    /** Updates {@link UsageProgressBarPreference} information. */
    public void quickUpdateHeaderPreference() {
        if (mBatteryUsageProgressBarPreference == null) {
            return;
        }

        Intent batteryBroadcast =
                com.android.settingslib.fuelgauge.BatteryUtils.getBatteryIntent(mContext);
        final int batteryLevel = Utils.getBatteryLevel(batteryBroadcast);

        mBatteryUsageProgressBarPreference.setUsageSummary(
                formatBatteryPercentageText(batteryLevel));
        mBatteryUsageProgressBarPreference.setPercent(batteryLevel, BATTERY_MAX_LEVEL);
    }

    private CharSequence formatBatteryPercentageText(int batteryLevel) {
        return com.android.settings.Utils.formatPercentage(batteryLevel);
    }
}
// LINT.ThenChange(BatteryHeaderPreference.kt)
