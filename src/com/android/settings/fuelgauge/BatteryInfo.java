/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.android.settings.fuelgauge;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.BatteryStats.HistoryItem;
import android.os.BatteryStatsManager;
import android.os.BatteryUsageStats;
import android.os.SystemClock;
import android.text.format.Formatter;
import android.util.Log;
import android.util.SparseIntArray;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import com.android.internal.os.BatteryStatsHistoryIterator;
import com.android.settings.Utils;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.widget.UsageView;
import com.android.settingslib.R;
import com.android.settingslib.fuelgauge.Estimate;
import com.android.settingslib.fuelgauge.EstimateKt;
import com.android.settingslib.utils.PowerUtil;
import com.android.settingslib.utils.StringUtil;

public class BatteryInfo {
    private static final String TAG = "BatteryInfo";

    public CharSequence chargeLabel;
    public CharSequence remainingLabel;
    public int batteryLevel;
    public int batteryStatus;
    public int pluggedStatus;
    public boolean discharging = true;
    public boolean isOverheated;
    public long remainingTimeUs = 0;
    public long averageTimeToDischarge = EstimateKt.AVERAGE_TIME_TO_DISCHARGE_UNKNOWN;
    public String batteryPercentString;
    public String statusLabel;
    public String suggestionLabel;
    private boolean mCharging;
    private BatteryUsageStats mBatteryUsageStats;
    private static final String LOG_TAG = "BatteryInfo";
    private long timePeriod;

    public interface Callback {
        void onBatteryInfoLoaded(BatteryInfo info);
    }

    public void bindHistory(final UsageView view, BatteryDataParser... parsers) {
        final Context context = view.getContext();
        BatteryDataParser parser = new BatteryDataParser() {
            SparseIntArray points = new SparseIntArray();
            long startTime;
            int lastTime = -1;
            byte lastLevel;

            @Override
            public void onParsingStarted(long startTime, long endTime) {
                this.startTime = startTime;
                timePeriod = endTime - startTime;
                view.clearPaths();
                // Initially configure the graph for history only.
                view.configureGraph((int) timePeriod, 100);
            }

            @Override
            public void onDataPoint(long time, HistoryItem record) {
                lastTime = (int) time;
                lastLevel = record.batteryLevel;
                points.put(lastTime, lastLevel);
            }

            @Override
            public void onDataGap() {
                if (points.size() > 1) {
                    view.addPath(points);
                }
                points.clear();
            }

            @Override
            public void onParsingDone() {
                onDataGap();

                // Add projection if we have an estimate.
                if (remainingTimeUs != 0) {
                    PowerUsageFeatureProvider provider = FeatureFactory.getFactory(context)
                            .getPowerUsageFeatureProvider(context);
                    if (!mCharging && provider.isEnhancedBatteryPredictionEnabled(context)) {
                        points = provider.getEnhancedBatteryPredictionCurve(context, startTime);
                    } else {
                        // Linear extrapolation.
                        if (lastTime >= 0) {
                            points.put(lastTime, lastLevel);
                            points.put((int) (timePeriod +
                                            PowerUtil.convertUsToMs(remainingTimeUs)),
                                    mCharging ? 100 : 0);
                        }
                    }
                }

                // If we have a projection, reconfigure the graph to show it.
                if (points != null && points.size() > 0) {
                    int maxTime = points.keyAt(points.size() - 1);
                    view.configureGraph(maxTime, 100);
                    view.addProjectedPath(points);
                }
            }
        };
        BatteryDataParser[] parserList = new BatteryDataParser[parsers.length + 1];
        for (int i = 0; i < parsers.length; i++) {
            parserList[i] = parsers[i];
        }
        parserList[parsers.length] = parser;
        parseBatteryHistory(parserList);
        String timeString = context.getString(R.string.charge_length_format,
                Formatter.formatShortElapsedTime(context, timePeriod));
        String remaining = "";
        if (remainingTimeUs != 0) {
            remaining = context.getString(R.string.remaining_length_format,
                    Formatter.formatShortElapsedTime(context, remainingTimeUs / 1000));
        }
        view.setBottomLabels(new CharSequence[]{timeString, remaining});
    }

    public static void getBatteryInfo(final Context context, final Callback callback,
            boolean shortString) {
        BatteryInfo.getBatteryInfo(context, callback,  /* batteryUsageStats */ null, shortString);
    }

    public static void getBatteryInfo(final Context context, final Callback callback,
            @Nullable final BatteryUsageStats batteryUsageStats,
            boolean shortString) {
        new AsyncTask<Void, Void, BatteryInfo>() {
            @Override
            protected BatteryInfo doInBackground(Void... params) {
                boolean shouldCloseBatteryUsageStats = false;
                BatteryUsageStats stats;
                if (batteryUsageStats != null) {
                    stats = batteryUsageStats;
                } else {
                    try {
                        stats = context.getSystemService(BatteryStatsManager.class)
                                .getBatteryUsageStats();
                        shouldCloseBatteryUsageStats = true;
                    } catch (RuntimeException e) {
                        Log.e(TAG, "getBatteryInfo() from getBatteryUsageStats()", e);
                        // Use default BatteryUsageStats.
                        stats = new BatteryUsageStats.Builder(new String[0]).build();
                    }
                }
                final BatteryInfo batteryInfo =
                        getBatteryInfo(context, stats, shortString);
                if (shouldCloseBatteryUsageStats) {
                    try {
                        stats.close();
                    } catch (Exception e) {
                        Log.e(TAG, "BatteryUsageStats.close() failed", e);
                    }
                }
                return batteryInfo;
            }

            @Override
            protected void onPostExecute(BatteryInfo batteryInfo) {
                final long startTime = System.currentTimeMillis();
                callback.onBatteryInfoLoaded(batteryInfo);
                BatteryUtils.logRuntime(LOG_TAG, "time for callback", startTime);
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    /**
     * Creates a BatteryInfo based on BatteryUsageStats
     */
    @WorkerThread
    public static BatteryInfo getBatteryInfo(final Context context,
            @NonNull final BatteryUsageStats batteryUsageStats, boolean shortString) {
        final long batteryStatsTime = System.currentTimeMillis();
        BatteryUtils.logRuntime(LOG_TAG, "time for getStats", batteryStatsTime);

        final long startTime = System.currentTimeMillis();
        PowerUsageFeatureProvider provider =
                FeatureFactory.getFactory(context).getPowerUsageFeatureProvider(context);
        final long elapsedRealtimeUs =
                PowerUtil.convertMsToUs(SystemClock.elapsedRealtime());

        final Intent batteryBroadcast = context.registerReceiver(null,
                new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        // 0 means we are discharging, anything else means charging
        final boolean discharging =
                batteryBroadcast.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) == 0;

        if (discharging && provider != null
                && provider.isEnhancedBatteryPredictionEnabled(context)) {
            Estimate estimate = provider.getEnhancedBatteryPrediction(context);
            if (estimate != null) {
                Estimate.storeCachedEstimate(context, estimate);
                BatteryUtils
                        .logRuntime(LOG_TAG, "time for enhanced BatteryInfo", startTime);
                return BatteryInfo.getBatteryInfo(context, batteryBroadcast, batteryUsageStats,
                        estimate, elapsedRealtimeUs, shortString);
            }
        }
        final long prediction = discharging ? batteryUsageStats.getBatteryTimeRemainingMs() : 0;
        final Estimate estimate = new Estimate(
                prediction,
                false, /* isBasedOnUsage */
                EstimateKt.AVERAGE_TIME_TO_DISCHARGE_UNKNOWN);
        BatteryUtils.logRuntime(LOG_TAG, "time for regular BatteryInfo", startTime);
        return BatteryInfo.getBatteryInfo(context, batteryBroadcast, batteryUsageStats,
                estimate, elapsedRealtimeUs, shortString);
    }

    @WorkerThread
    public static BatteryInfo getBatteryInfoOld(Context context, Intent batteryBroadcast,
            BatteryUsageStats batteryUsageStats, long elapsedRealtimeUs, boolean shortString) {
        Estimate estimate = new Estimate(
                batteryUsageStats.getBatteryTimeRemainingMs(),
                false,
                EstimateKt.AVERAGE_TIME_TO_DISCHARGE_UNKNOWN);
        return getBatteryInfo(context, batteryBroadcast, batteryUsageStats, estimate,
                elapsedRealtimeUs, shortString);
    }

    @WorkerThread
    public static BatteryInfo getBatteryInfo(Context context, Intent batteryBroadcast,
            @NonNull BatteryUsageStats batteryUsageStats, Estimate estimate,
            long elapsedRealtimeUs, boolean shortString) {
        final long startTime = System.currentTimeMillis();
        final boolean isCompactStatus = context.getResources().getBoolean(
                com.android.settings.R.bool.config_use_compact_battery_status);
        BatteryInfo info = new BatteryInfo();
        info.mBatteryUsageStats = batteryUsageStats;
        info.batteryLevel = Utils.getBatteryLevel(batteryBroadcast);
        info.batteryPercentString = Utils.formatPercentage(info.batteryLevel);
        info.pluggedStatus = batteryBroadcast.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);
        info.mCharging = info.pluggedStatus != 0;
        info.averageTimeToDischarge = estimate.getAverageDischargeTime();
        info.isOverheated = batteryBroadcast.getIntExtra(
                BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN)
                == BatteryManager.BATTERY_HEALTH_OVERHEAT;

        info.statusLabel = Utils.getBatteryStatus(context, batteryBroadcast, isCompactStatus);
        info.batteryStatus = batteryBroadcast.getIntExtra(
                BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN);
        if (!info.mCharging) {
            updateBatteryInfoDischarging(context, shortString, estimate, info);
        } else {
            updateBatteryInfoCharging(context, batteryBroadcast, batteryUsageStats,
                    info, isCompactStatus);
        }
        BatteryUtils.logRuntime(LOG_TAG, "time for getBatteryInfo", startTime);
        return info;
    }

    private static void updateBatteryInfoCharging(Context context, Intent batteryBroadcast,
            BatteryUsageStats stats, BatteryInfo info, boolean compactStatus) {
        final Resources resources = context.getResources();
        final long chargeTimeMs = stats.getChargeTimeRemainingMs();
        final int status = batteryBroadcast.getIntExtra(BatteryManager.EXTRA_STATUS,
                BatteryManager.BATTERY_STATUS_UNKNOWN);
        info.discharging = false;
        info.suggestionLabel = null;
        int dockDefenderMode = BatteryUtils.getCurrentDockDefenderMode(context, info);
        if ((info.isOverheated && status != BatteryManager.BATTERY_STATUS_FULL
                && dockDefenderMode == BatteryUtils.DockDefenderMode.DISABLED)
                || dockDefenderMode == BatteryUtils.DockDefenderMode.ACTIVE) {
            // Battery defender active, battery charging paused
            info.remainingLabel = null;
            int chargingLimitedResId = R.string.power_charging_limited;
            info.chargeLabel = context.getString(chargingLimitedResId, info.batteryPercentString);
        } else if ((chargeTimeMs > 0 && status != BatteryManager.BATTERY_STATUS_FULL
                && dockDefenderMode == BatteryUtils.DockDefenderMode.DISABLED)
                || dockDefenderMode == BatteryUtils.DockDefenderMode.TEMPORARILY_BYPASSED) {
            // Battery is charging to full
            info.remainingTimeUs = PowerUtil.convertMsToUs(chargeTimeMs);
            final CharSequence timeString = StringUtil.formatElapsedTime(context,
                    (double) PowerUtil.convertUsToMs(info.remainingTimeUs), false /* withSeconds */,
                    true /* collapseTimeUnit */);
            int resId = R.string.power_charging_duration;
            info.remainingLabel = context.getString(R.string.power_remaining_charging_duration_only,
                    timeString);
            info.chargeLabel = context.getString(resId, info.batteryPercentString, timeString);
        } else if (dockDefenderMode == BatteryUtils.DockDefenderMode.FUTURE_BYPASS) {
            // Dock defender will be triggered in the future, charging will be optimized.
            info.chargeLabel = context.getString(R.string.power_charging_future_paused,
                    info.batteryPercentString);
        } else {
            final String chargeStatusLabel = Utils.getBatteryStatus(context, batteryBroadcast,
                    compactStatus);
            info.remainingLabel = null;
            info.chargeLabel = info.batteryLevel == 100 ? info.batteryPercentString :
                    resources.getString(R.string.power_charging, info.batteryPercentString,
                            chargeStatusLabel.toLowerCase());
        }
    }

    private static void updateBatteryInfoDischarging(Context context, boolean shortString,
            Estimate estimate, BatteryInfo info) {
        final long drainTimeUs = PowerUtil.convertMsToUs(estimate.getEstimateMillis());
        if (drainTimeUs > 0) {
            info.remainingTimeUs = drainTimeUs;
            info.remainingLabel = PowerUtil.getBatteryRemainingStringFormatted(
                    context,
                    PowerUtil.convertUsToMs(drainTimeUs),
                    null /* percentageString */,
                    false /* basedOnUsage */
            );
            info.chargeLabel = PowerUtil.getBatteryRemainingStringFormatted(
                    context,
                    PowerUtil.convertUsToMs(drainTimeUs),
                    info.batteryPercentString,
                    estimate.isBasedOnUsage() && !shortString
            );
            info.suggestionLabel = PowerUtil.getBatteryTipStringFormatted(
                    context, PowerUtil.convertUsToMs(drainTimeUs));
        } else {
            info.remainingLabel = null;
            info.suggestionLabel = null;
            info.chargeLabel = info.batteryPercentString;
        }
    }

    public interface BatteryDataParser {
        void onParsingStarted(long startTime, long endTime);

        void onDataPoint(long time, HistoryItem record);

        void onDataGap();

        void onParsingDone();
    }

    /**
     * Iterates over battery history included in the BatteryUsageStats that this object
     * was initialized with.
     */
    public void parseBatteryHistory(BatteryDataParser... parsers) {
        long startWalltime = 0;
        long endWalltime = 0;
        long historyStart = 0;
        long historyEnd = 0;
        long curWalltime = startWalltime;
        long lastWallTime = 0;
        long lastRealtime = 0;
        int lastInteresting = 0;
        int pos = 0;
        boolean first = true;
        final BatteryStatsHistoryIterator iterator1 =
                mBatteryUsageStats.iterateBatteryStatsHistory();
        final HistoryItem rec = new HistoryItem();
        while (iterator1.next(rec)) {
            pos++;
            if (first) {
                first = false;
                historyStart = rec.time;
            }
            if (rec.cmd == HistoryItem.CMD_CURRENT_TIME
                    || rec.cmd == HistoryItem.CMD_RESET) {
                // If there is a ridiculously large jump in time, then we won't be
                // able to create a good chart with that data, so just ignore the
                // times we got before and pretend like our data extends back from
                // the time we have now.
                // Also, if we are getting a time change and we are less than 5 minutes
                // since the start of the history real time, then also use this new
                // time to compute the base time, since whatever time we had before is
                // pretty much just noise.
                if (rec.currentTime > (lastWallTime + (180 * 24 * 60 * 60 * 1000L))
                        || rec.time < (historyStart + (5 * 60 * 1000L))) {
                    startWalltime = 0;
                }
                lastWallTime = rec.currentTime;
                lastRealtime = rec.time;
                if (startWalltime == 0) {
                    startWalltime = lastWallTime - (lastRealtime - historyStart);
                }
            }
            if (rec.isDeltaData()) {
                lastInteresting = pos;
                historyEnd = rec.time;
            }
        }

        endWalltime = lastWallTime + historyEnd - lastRealtime;

        int i = 0;
        final int N = lastInteresting;

        for (int j = 0; j < parsers.length; j++) {
            parsers[j].onParsingStarted(startWalltime, endWalltime);
        }

        if (endWalltime > startWalltime) {
            final BatteryStatsHistoryIterator iterator2 =
                    mBatteryUsageStats.iterateBatteryStatsHistory();
            while (iterator2.next(rec) && i < N) {
                if (rec.isDeltaData()) {
                    curWalltime += rec.time - lastRealtime;
                    lastRealtime = rec.time;
                    long x = (curWalltime - startWalltime);
                    if (x < 0) {
                        x = 0;
                    }
                    for (int j = 0; j < parsers.length; j++) {
                        parsers[j].onDataPoint(x, rec);
                    }
                } else {
                    long lastWalltime = curWalltime;
                    if (rec.cmd == HistoryItem.CMD_CURRENT_TIME
                            || rec.cmd == HistoryItem.CMD_RESET) {
                        if (rec.currentTime >= startWalltime) {
                            curWalltime = rec.currentTime;
                        } else {
                            curWalltime = startWalltime + (rec.time - historyStart);
                        }
                        lastRealtime = rec.time;
                    }

                    if (rec.cmd != HistoryItem.CMD_OVERFLOW
                            && (rec.cmd != HistoryItem.CMD_CURRENT_TIME
                            || Math.abs(lastWalltime - curWalltime) > (60 * 60 * 1000))) {
                        for (int j = 0; j < parsers.length; j++) {
                            parsers[j].onDataGap();
                        }
                    }
                }
                i++;
            }
        }

        for (int j = 0; j < parsers.length; j++) {
            parsers[j].onParsingDone();
        }
    }
}
