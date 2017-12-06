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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Check whether apps has too many wakeup alarms
 */
public class WakeupAlarmAnomalyDetector implements AnomalyDetector {
    private static final String TAG = "WakeupAlarmAnomalyDetector";
    @VisibleForTesting
    BatteryUtils mBatteryUtils;
    private long mWakeupAlarmThreshold;
    private Set<String> mWakeupBlacklistedTags;
    private Context mContext;
    private AnomalyUtils mAnomalyUtils;

    public WakeupAlarmAnomalyDetector(Context context) {
        this(context, new AnomalyDetectionPolicy(context), AnomalyUtils.getInstance(context));
    }

    @VisibleForTesting
    WakeupAlarmAnomalyDetector(Context context, AnomalyDetectionPolicy policy,
            AnomalyUtils anomalyUtils) {
        mContext = context;
        mBatteryUtils = BatteryUtils.getInstance(context);
        mAnomalyUtils = anomalyUtils;
        mWakeupAlarmThreshold = policy.wakeupAlarmThreshold;
        mWakeupBlacklistedTags = policy.wakeupBlacklistedTags;
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
        final double totalRunningHours = mBatteryUtils.calculateRunningTimeBasedOnStatsType(
                batteryStatsHelper, BatteryStats.STATS_SINCE_CHARGED)
                / (double) DateUtils.HOUR_IN_MILLIS;
        final int targetUid = mBatteryUtils.getPackageUid(targetPackageName);

        if (totalRunningHours >= 1) {
            for (int i = 0, size = batterySippers.size(); i < size; i++) {
                final BatterySipper sipper = batterySippers.get(i);
                final BatteryStats.Uid uid = sipper.uidObj;
                if (uid == null
                        || mBatteryUtils.shouldHideSipper(sipper)
                        || (targetUid != BatteryUtils.UID_NULL && targetUid != uid.getUid())) {
                    continue;
                }

                final int wakeupAlarmCount = (int) (getWakeupAlarmCountFromUid(uid)
                        / totalRunningHours);
                if (wakeupAlarmCount > mWakeupAlarmThreshold) {
                    final String packageName = mBatteryUtils.getPackageName(uid.getUid());
                    final CharSequence displayName = Utils.getApplicationLabel(mContext,
                            packageName);
                    final int targetSdkVersion = mBatteryUtils.getTargetSdkVersion(packageName);

                    Anomaly anomaly = new Anomaly.Builder()
                            .setUid(uid.getUid())
                            .setType(Anomaly.AnomalyType.WAKEUP_ALARM)
                            .setDisplayName(displayName)
                            .setPackageName(packageName)
                            .setTargetSdkVersion(targetSdkVersion)
                            .setBackgroundRestrictionEnabled(
                                    mBatteryUtils.isBackgroundRestrictionEnabled(targetSdkVersion,
                                            uid.getUid(), packageName))
                            .setWakeupAlarmCount(wakeupAlarmCount)
                            .build();

                    if (mAnomalyUtils.getAnomalyAction(anomaly).isActionActive(anomaly)) {
                        anomalies.add(anomaly);
                    }
                }
            }
        }

        return anomalies;
    }

    @VisibleForTesting
    int getWakeupAlarmCountFromUid(BatteryStats.Uid uid) {
        int wakeups = 0;
        final ArrayMap<String, ? extends BatteryStats.Uid.Pkg> packageStats
                = uid.getPackageStats();
        for (int ipkg = packageStats.size() - 1; ipkg >= 0; ipkg--) {
            final BatteryStats.Uid.Pkg ps = packageStats.valueAt(ipkg);
            final ArrayMap<String, ? extends BatteryStats.Counter> alarms =
                    ps.getWakeupAlarmStats();
            for (Map.Entry<String, ? extends BatteryStats.Counter> alarm : alarms.entrySet()) {
                if (mWakeupBlacklistedTags != null
                        && mWakeupBlacklistedTags.contains(alarm.getKey())) {
                    continue;
                }
                int count = alarm.getValue().getCountLocked(BatteryStats.STATS_SINCE_CHARGED);
                wakeups += count;
            }
        }

        return wakeups;
    }

}
