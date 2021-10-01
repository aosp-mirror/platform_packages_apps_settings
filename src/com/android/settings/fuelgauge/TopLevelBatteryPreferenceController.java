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

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;
import com.android.settingslib.utils.ThreadUtils;

import java.util.HashMap;

public class TopLevelBatteryPreferenceController extends BasePreferenceController implements
        LifecycleObserver, OnStart, OnStop, BatteryPreferenceController {

    @VisibleForTesting
    protected boolean mIsBatteryPresent = true;
    @VisibleForTesting
    Preference mPreference;
    private final BatteryBroadcastReceiver mBatteryBroadcastReceiver;
    private BatteryInfo mBatteryInfo;
    private BatterySettingsFeatureProvider mBatterySettingsFeatureProvider;
    private BatteryStatusFeatureProvider mBatteryStatusFeatureProvider;
    private String mBatteryStatusLabel;

    @VisibleForTesting
    protected static HashMap<String, ComponentName> sReplacingActivityMap = new HashMap<>();

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

        mBatterySettingsFeatureProvider = FeatureFactory.getFactory(context)
                .getBatterySettingsFeatureProvider(context);
        mBatteryStatusFeatureProvider = FeatureFactory.getFactory(context)
                .getBatteryStatusFeatureProvider(context);
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
    public boolean handlePreferenceTreeClick(Preference preference) {
        String prefFrag = preference.getFragment();
        if (prefFrag == null || prefFrag.isEmpty()) {
            // Not a redirect, so use the default.
            return super.handlePreferenceTreeClick(preference);
        }

        ComponentName currentFragmentName = convertClassPathToComponentName(prefFrag);
        if (currentFragmentName == null) {
            return super.handlePreferenceTreeClick(preference);
        }

        ComponentName replacingActivity;
        if (sReplacingActivityMap.containsKey(prefFrag)) {
            replacingActivity = sReplacingActivityMap.get(prefFrag);
        } else {
            replacingActivity = mBatterySettingsFeatureProvider.getReplacingActivity(
                    currentFragmentName);
            sReplacingActivityMap.put(prefFrag, replacingActivity);
        }

        if (replacingActivity == null || currentFragmentName.compareTo(replacingActivity) == 0) {
            return super.handlePreferenceTreeClick(preference);
        }
        Intent intent = new Intent();
        intent.setComponent(replacingActivity);
        mContext.startActivity(intent);
        return true;
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
        return getSummary(true /* batteryStatusUpdate */);
    }

    private CharSequence getSummary(boolean batteryStatusUpdate) {
        // Display help message if battery is not present.
        if (!mIsBatteryPresent) {
            return mContext.getText(R.string.battery_missing_message);
        }
        return getDashboardLabel(mContext, mBatteryInfo, batteryStatusUpdate);
    }

    protected CharSequence getDashboardLabel(Context context, BatteryInfo info,
            boolean batteryStatusUpdate) {
        if (info == null || context == null) {
            return null;
        }

        if (batteryStatusUpdate) {
            setSummaryAsync(info);
        }

        return (mBatteryStatusLabel == null) ? generateLabel(info) : mBatteryStatusLabel;
    }

    private void setSummaryAsync(BatteryInfo info) {
        ThreadUtils.postOnBackgroundThread(() -> {
            final boolean triggerBatteryStatusUpdate =
                    mBatteryStatusFeatureProvider.triggerBatteryStatusUpdate(this, info);
            ThreadUtils.postOnMainThread(() -> {
                if (!triggerBatteryStatusUpdate) {
                    mBatteryStatusLabel = null; // will generateLabel()
                }
                mPreference.setSummary(
                        (mBatteryStatusLabel == null) ? generateLabel(info) : mBatteryStatusLabel);
            });
        });
    }

    private CharSequence generateLabel(BatteryInfo info) {
        if (!info.discharging && info.chargeLabel != null) {
            return info.chargeLabel;
        } else if (info.remainingLabel == null) {
            return info.batteryPercentString;
        } else {
            return mContext.getString(R.string.power_remaining_settings_home_page,
                    info.batteryPercentString,
                    info.remainingLabel);
        }
    }

    /**
     * Callback which receives text for the label.
     */
    public void updateBatteryStatus(String label, BatteryInfo info) {
        mBatteryStatusLabel = label; // Null if adaptive charging is not active

        if (mPreference != null) {
            // Do not triggerBatteryStatusUpdate(), otherwise there will be an infinite loop
            final CharSequence summary = getSummary(false /* batteryStatusUpdate */);
            if (summary != null) {
                mPreference.setSummary(summary);
            }
        }
    }

    @VisibleForTesting
    protected static ComponentName convertClassPathToComponentName(String classPath) {
        if (classPath == null || classPath.isEmpty()) {
            return null;
        }
        String[] split = classPath.split("\\.");
        int classNameIndex = split.length - 1;
        if (classNameIndex < 0) {
            return null;
        }
        int lastPkgIndex = classPath.length() - split[classNameIndex].length() - 1;
        String pkgName = lastPkgIndex > 0 ? classPath.substring(0, lastPkgIndex) : "";
        return new ComponentName(pkgName, split[classNameIndex]);
    }
}