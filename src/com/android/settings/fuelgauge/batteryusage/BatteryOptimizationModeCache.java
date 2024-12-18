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

package com.android.settings.fuelgauge.batteryusage;

import android.content.Context;
import android.util.SparseArray;

import androidx.annotation.VisibleForTesting;
import androidx.core.util.Pair;

import com.android.settings.fuelgauge.BatteryOptimizeUtils;
import com.android.settingslib.fuelgauge.PowerAllowlistBackend;

/** A cache to log battery optimization mode of an app */
final class BatteryOptimizationModeCache {
    private static final String TAG = "BatteryOptimizationModeCache";

    /* Stores the battery optimization mode and mutable state for each UID. */
    @VisibleForTesting
    final SparseArray<Pair<BatteryOptimizationMode, Boolean>> mBatteryOptimizeModeCache;

    private final Context mContext;

    BatteryOptimizationModeCache(final Context context) {
        mContext = context;
        mBatteryOptimizeModeCache = new SparseArray<>();
        PowerAllowlistBackend.getInstance(mContext).refreshList();
    }

    Pair<BatteryOptimizationMode, Boolean> getBatteryOptimizeModeInfo(
            final int uid, final String packageName) {
        if (!mBatteryOptimizeModeCache.contains(uid)) {
            final BatteryOptimizeUtils batteryOptimizeUtils =
                    new BatteryOptimizeUtils(mContext, uid, packageName);
            mBatteryOptimizeModeCache.put(
                    uid,
                    Pair.create(
                            BatteryOptimizationMode.forNumber(
                                    batteryOptimizeUtils.getAppOptimizationMode(
                                            /* refreshList= */ false)),
                            batteryOptimizeUtils.isOptimizeModeMutable()));
        }
        final Pair<BatteryOptimizationMode, Boolean> batteryOptimizeModeInfo =
                mBatteryOptimizeModeCache.get(uid);
        return batteryOptimizeModeInfo != null
                ? batteryOptimizeModeInfo
                : new Pair<>(BatteryOptimizationMode.MODE_UNKNOWN, false);
    }
}
