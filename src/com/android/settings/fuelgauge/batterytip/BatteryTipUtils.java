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

package com.android.settings.fuelgauge.batterytip;

import android.app.AppOpsManager;
import android.app.PendingIntent;
import android.app.StatsManager;
import android.content.Context;
import android.content.Intent;
import android.os.UserHandle;
import android.os.UserManager;

import androidx.annotation.NonNull;

import com.android.internal.util.CollectionUtils;
import com.android.settings.SettingsActivity;
import com.android.settings.core.InstrumentedPreferenceFragment;
import com.android.settings.fuelgauge.batterytip.actions.BatteryTipAction;
import com.android.settings.fuelgauge.batterytip.actions.OpenBatterySaverAction;
import com.android.settings.fuelgauge.batterytip.actions.OpenRestrictAppFragmentAction;
import com.android.settings.fuelgauge.batterytip.actions.RestrictAppAction;
import com.android.settings.fuelgauge.batterytip.actions.SmartBatteryAction;
import com.android.settings.fuelgauge.batterytip.actions.UnrestrictAppAction;
import com.android.settings.fuelgauge.batterytip.tips.AppLabelPredicate;
import com.android.settings.fuelgauge.batterytip.tips.AppRestrictionPredicate;
import com.android.settings.fuelgauge.batterytip.tips.BatteryTip;
import com.android.settings.fuelgauge.batterytip.tips.RestrictAppTip;
import com.android.settings.fuelgauge.batterytip.tips.UnrestrictAppTip;

import java.util.ArrayList;
import java.util.List;

/** Utility class for {@link BatteryTip} */
public class BatteryTipUtils {
    private static final int REQUEST_CODE = 0;

    /** Get a list of restricted apps with {@link AppOpsManager#OP_RUN_ANY_IN_BACKGROUND} */
    @NonNull
    public static List<AppInfo> getRestrictedAppsList(
            AppOpsManager appOpsManager, UserManager userManager) {
        final List<UserHandle> userHandles = userManager.getUserProfiles();
        final List<AppOpsManager.PackageOps> packageOpsList =
                appOpsManager.getPackagesForOps(new int[] {AppOpsManager.OP_RUN_ANY_IN_BACKGROUND});
        final List<AppInfo> appInfos = new ArrayList<>();

        for (int i = 0, size = CollectionUtils.size(packageOpsList); i < size; i++) {
            final AppOpsManager.PackageOps packageOps = packageOpsList.get(i);
            final List<AppOpsManager.OpEntry> entries = packageOps.getOps();
            for (int j = 0, entriesSize = entries.size(); j < entriesSize; j++) {
                AppOpsManager.OpEntry entry = entries.get(j);
                if (entry.getOp() != AppOpsManager.OP_RUN_ANY_IN_BACKGROUND) {
                    continue;
                }
                if (entry.getMode() != AppOpsManager.MODE_ALLOWED
                        && userHandles.contains(
                                new UserHandle(UserHandle.getUserId(packageOps.getUid())))) {
                    appInfos.add(
                            new AppInfo.Builder()
                                    .setPackageName(packageOps.getPackageName())
                                    .setUid(packageOps.getUid())
                                    .build());
                }
            }
        }

        return appInfos;
    }

    /**
     * Get a corresponding action based on {@code batteryTip}
     *
     * @param batteryTip used to detect which action to choose
     * @param settingsActivity used to populate {@link BatteryTipAction}
     * @param fragment used to populate {@link BatteryTipAction}
     * @return an action for {@code batteryTip}
     */
    public static BatteryTipAction getActionForBatteryTip(
            BatteryTip batteryTip,
            SettingsActivity settingsActivity,
            InstrumentedPreferenceFragment fragment) {
        switch (batteryTip.getType()) {
            case BatteryTip.TipType.SMART_BATTERY_MANAGER:
                return new SmartBatteryAction(settingsActivity, fragment);
            case BatteryTip.TipType.BATTERY_SAVER:
            case BatteryTip.TipType.LOW_BATTERY:
                return new OpenBatterySaverAction(settingsActivity);
            case BatteryTip.TipType.APP_RESTRICTION:
                if (batteryTip.getState() == BatteryTip.StateType.HANDLED) {
                    return new OpenRestrictAppFragmentAction(fragment, (RestrictAppTip) batteryTip);
                } else {
                    return new RestrictAppAction(settingsActivity, (RestrictAppTip) batteryTip);
                }
            case BatteryTip.TipType.REMOVE_APP_RESTRICTION:
                return new UnrestrictAppAction(settingsActivity, (UnrestrictAppTip) batteryTip);
            default:
                return null;
        }
    }

    /**
     * Upload the {@link PendingIntent} to {@link StatsManager} for anomaly detection
     *
     * @throws StatsManager.StatsUnavailableException if failed to communicate with stats service
     */
    public static void uploadAnomalyPendingIntent(Context context, StatsManager statsManager)
            throws StatsManager.StatsUnavailableException {
        final Intent extraIntent = new Intent(context, AnomalyDetectionReceiver.class);
        final PendingIntent pendingIntent =
                PendingIntent.getBroadcast(
                        context,
                        REQUEST_CODE,
                        extraIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
        statsManager.setBroadcastSubscriber(
                pendingIntent,
                StatsManagerConfig.ANOMALY_CONFIG_KEY,
                StatsManagerConfig.SUBSCRIBER_ID);
    }

    /** Detect and return anomaly apps after {@code timeAfterMs} */
    public static List<AppInfo> detectAnomalies(Context context, long timeAfterMs) {
        final List<AppInfo> highUsageApps =
                BatteryDatabaseManager.getInstance(context)
                        .queryAllAnomalies(timeAfterMs, AnomalyDatabaseHelper.State.NEW);
        // Remove it if it doesn't have label or been restricted
        highUsageApps.removeIf(
                AppLabelPredicate.getInstance(context)
                        .or(AppRestrictionPredicate.getInstance(context)));

        return highUsageApps;
    }
}
