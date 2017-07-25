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

package com.android.settings.fuelgauge.anomaly.checker;

import android.content.Context;
import android.os.BatteryStats;
import android.os.SystemClock;
import android.support.annotation.VisibleForTesting;
import android.text.format.DateUtils;
import android.util.ArrayMap;

import com.android.internal.os.BatterySipper;
import com.android.internal.os.BatteryStatsHelper;
import com.android.settings.Utils;
import com.android.settings.fuelgauge.BatteryUtils;
import com.android.settings.fuelgauge.anomaly.Anomaly;
import com.android.settings.fuelgauge.anomaly.AnomalyDetectionPolicy;
import com.android.settings.fuelgauge.anomaly.AnomalyUtils;
import com.android.settings.fuelgauge.anomaly.action.AnomalyAction;

import java.util.ArrayList;
import java.util.List;

/**
 * Check whether apps have unoptimized bluetooth scanning in the background
 */
public class BluetoothScanAnomalyDetector implements AnomalyDetector {
    private static final String TAG = "BluetoothScanAnomalyDetector";
    @VisibleForTesting
    BatteryUtils mBatteryUtils;
    private long mBluetoothScanningThreshold;
    private Context mContext;
    private AnomalyUtils mAnomalyUtils;

    public BluetoothScanAnomalyDetector(Context context) {
        this(context, new AnomalyDetectionPolicy(context), AnomalyUtils.getInstance(context));
    }

    @VisibleForTesting
    BluetoothScanAnomalyDetector(Context context, AnomalyDetectionPolicy policy,
            AnomalyUtils anomalyUtils) {
        mContext = context;
        mBatteryUtils = BatteryUtils.getInstance(context);
        mBluetoothScanningThreshold = policy.bluetoothScanThreshold;
        mAnomalyUtils = anomalyUtils;
    }

    @Override
    public List<Anomaly> detectAnomalies(BatteryStatsHelper batteryStatsHelper) {
        // Detect all apps if targetPackageName is null
        return detectAnomalies(batteryStatsHelper, null /* targetPackageName */);
    }

    @Override
    public List<Anomaly> detectAnomalies(BatteryStatsHelper batteryStatsHelper,
            String targetPackageName) {
        final List<BatterySipper> batterySippers = batteryStatsHelper.getUsageList();
        final List<Anomaly> anomalies = new ArrayList<>();
        final int targetUid = mBatteryUtils.getPackageUid(targetPackageName);
        final long elapsedRealtimeMs = SystemClock.elapsedRealtime();

        for (int i = 0, size = batterySippers.size(); i < size; i++) {
            final BatterySipper sipper = batterySippers.get(i);
            final BatteryStats.Uid uid = sipper.uidObj;
            if (uid == null
                    || mBatteryUtils.shouldHideSipper(sipper)
                    || (targetUid != BatteryUtils.UID_NULL && targetUid != uid.getUid())) {
                continue;
            }

            final long bluetoothTimeMs = getBluetoothUnoptimizedBgTimeMs(uid, elapsedRealtimeMs);
            if (bluetoothTimeMs > mBluetoothScanningThreshold) {
                final String packageName = mBatteryUtils.getPackageName(uid.getUid());
                final CharSequence displayName = Utils.getApplicationLabel(mContext,
                        packageName);

                Anomaly anomaly = new Anomaly.Builder()
                        .setUid(uid.getUid())
                        .setType(Anomaly.AnomalyType.BLUETOOTH_SCAN)
                        .setDisplayName(displayName)
                        .setPackageName(packageName)
                        .setBluetoothScanningTimeMs(bluetoothTimeMs)
                        .build();

                if (mAnomalyUtils.getAnomalyAction(anomaly).isActionActive(anomaly)) {
                    anomalies.add(anomaly);
                }
            }
        }

        return anomalies;
    }

    @VisibleForTesting
    public long getBluetoothUnoptimizedBgTimeMs(BatteryStats.Uid uid, long elapsedRealtimeMs) {
        BatteryStats.Timer timer = uid.getBluetoothUnoptimizedScanBackgroundTimer();

        return timer != null ? timer.getTotalDurationMsLocked(elapsedRealtimeMs) : 0;
    }

}
