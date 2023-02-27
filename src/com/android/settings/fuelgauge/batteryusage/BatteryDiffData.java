/*
 * Copyright (C) 2022 The Android Open Source Project
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
import android.os.BatteryConsumer;

import androidx.annotation.NonNull;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.fuelgauge.PowerUsageFeatureProvider;
import com.android.settings.overlay.FeatureFactory;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/** Wraps the battery usage diff data for each entry used for battery usage app list. */
public class BatteryDiffData {
    static final double SMALL_PERCENTAGE_THRESHOLD = 1f;

    private final List<BatteryDiffEntry> mAppEntries;
    private final List<BatteryDiffEntry> mSystemEntries;

    /** Constructor for the diff entries. */
    public BatteryDiffData(
            final Context context,
            final @NonNull List<BatteryDiffEntry> appDiffEntries,
            final @NonNull List<BatteryDiffEntry> systemDiffEntries,
            final Set<String> systemAppsSet,
            final boolean isAccumulated) {
        mAppEntries = appDiffEntries;
        mSystemEntries = systemDiffEntries;

        if (!isAccumulated) {
            final PowerUsageFeatureProvider featureProvider =
                    FeatureFactory.getFactory(context).getPowerUsageFeatureProvider(context);
            purgeBatteryDiffData(featureProvider);
            combineBatteryDiffEntry(context, featureProvider, systemAppsSet);
        }

        processAndSortEntries(mAppEntries);
        processAndSortEntries(mSystemEntries);
    }

    public List<BatteryDiffEntry> getAppDiffEntryList() {
        return mAppEntries;
    }

    public List<BatteryDiffEntry> getSystemDiffEntryList() {
        return mSystemEntries;
    }

    /** Removes fake usage data and hidden packages. */
    private void purgeBatteryDiffData(final PowerUsageFeatureProvider featureProvider) {
        purgeBatteryDiffData(featureProvider, mAppEntries);
        purgeBatteryDiffData(featureProvider, mSystemEntries);
    }

    /** Combines into SystemAppsBatteryDiffEntry and OthersBatteryDiffEntry. */
    private void combineBatteryDiffEntry(final Context context,
            final PowerUsageFeatureProvider featureProvider, final Set<String> systemAppsSet) {
        combineIntoSystemApps(context, featureProvider, systemAppsSet, mAppEntries);
        combineSystemItemsIntoOthers(context, featureProvider, mSystemEntries);
    }

    private static void purgeBatteryDiffData(
            final PowerUsageFeatureProvider featureProvider,
            final List<BatteryDiffEntry> entries) {
        final double screenOnTimeThresholdInMs =
                featureProvider.getBatteryUsageListScreenOnTimeThresholdInMs();
        final double consumePowerThreshold =
                featureProvider.getBatteryUsageListConsumePowerThreshold();
        final Set<Integer> hideSystemComponentSet = featureProvider.getHideSystemComponentSet();
        final Set<String> hideBackgroundUsageTimeSet =
                featureProvider.getHideBackgroundUsageTimeSet();
        final Set<String> hideApplicationSet = featureProvider.getHideApplicationSet();
        final Iterator<BatteryDiffEntry> iterator = entries.iterator();
        while (iterator.hasNext()) {
            final BatteryDiffEntry entry = iterator.next();
            final long screenOnTimeInMs = entry.mScreenOnTimeInMs;
            final double comsumePower = entry.mConsumePower;
            final String packageName = entry.getPackageName();
            final Integer componentId = entry.mBatteryHistEntry.mDrainType;
            if ((screenOnTimeInMs < screenOnTimeThresholdInMs
                    && comsumePower < consumePowerThreshold)
                    || ConvertUtils.FAKE_PACKAGE_NAME.equals(packageName)
                    || hideSystemComponentSet.contains(componentId)
                    || (packageName != null && hideApplicationSet.contains(packageName))) {
                iterator.remove();
            }
            if (packageName != null && hideBackgroundUsageTimeSet.contains(packageName)) {
                entry.mBackgroundUsageTimeInMs = 0;
            }
        }
    }

    private static void combineIntoSystemApps(
            final Context context,
            final PowerUsageFeatureProvider featureProvider,
            final Set<String> systemAppsSet,
            final List<BatteryDiffEntry> appEntries) {
        final List<String> systemAppsAllowlist = featureProvider.getSystemAppsAllowlist();
        BatteryDiffEntry.SystemAppsBatteryDiffEntry systemAppsDiffEntry = null;
        final Iterator<BatteryDiffEntry> appListIterator = appEntries.iterator();
        while (appListIterator.hasNext()) {
            final BatteryDiffEntry batteryDiffEntry = appListIterator.next();
            if (needsCombineInSystemApp(batteryDiffEntry, systemAppsAllowlist, systemAppsSet)) {
                if (systemAppsDiffEntry == null) {
                    systemAppsDiffEntry = new BatteryDiffEntry.SystemAppsBatteryDiffEntry(context);
                }
                systemAppsDiffEntry.mConsumePower += batteryDiffEntry.mConsumePower;
                systemAppsDiffEntry.mForegroundUsageTimeInMs +=
                        batteryDiffEntry.mForegroundUsageTimeInMs;
                systemAppsDiffEntry.setTotalConsumePower(
                        batteryDiffEntry.getTotalConsumePower());
                appListIterator.remove();
            }
        }
        if (systemAppsDiffEntry != null) {
            appEntries.add(systemAppsDiffEntry);
        }
    }

    private static void combineSystemItemsIntoOthers(
            final Context context,
            final PowerUsageFeatureProvider featureProvider,
            final List<BatteryDiffEntry> systemEntries) {
        final Set<Integer> othersSystemComponentSet = featureProvider.getOthersSystemComponentSet();
        final Set<String> othersCustomComponentNameSet =
                featureProvider.getOthersCustomComponentNameSet();
        BatteryDiffEntry.OthersBatteryDiffEntry othersDiffEntry = null;
        final Iterator<BatteryDiffEntry> systemListIterator = systemEntries.iterator();
        while (systemListIterator.hasNext()) {
            final BatteryDiffEntry batteryDiffEntry = systemListIterator.next();
            final int componentId = batteryDiffEntry.mBatteryHistEntry.mDrainType;
            if (othersSystemComponentSet.contains(componentId) || (
                    componentId >= BatteryConsumer.FIRST_CUSTOM_POWER_COMPONENT_ID
                            && othersCustomComponentNameSet.contains(
                                    batteryDiffEntry.getAppLabel()))) {
                if (othersDiffEntry == null) {
                    othersDiffEntry = new BatteryDiffEntry.OthersBatteryDiffEntry(context);
                }
                othersDiffEntry.mConsumePower += batteryDiffEntry.mConsumePower;
                othersDiffEntry.setTotalConsumePower(
                        batteryDiffEntry.getTotalConsumePower());
                systemListIterator.remove();
            }
        }
        if (othersDiffEntry != null) {
            systemEntries.add(othersDiffEntry);
        }
    }

    @VisibleForTesting
    static boolean needsCombineInSystemApp(final BatteryDiffEntry batteryDiffEntry,
            final List<String> systemAppsAllowlist, final Set<String> systemAppsSet) {
        if (batteryDiffEntry.mBatteryHistEntry.mIsHidden) {
            return true;
        }

        final String packageName = batteryDiffEntry.getPackageName();
        if (packageName == null || packageName.isEmpty()) {
            return false;
        }

        if (systemAppsAllowlist != null && systemAppsAllowlist.contains(packageName)) {
            return true;
        }

        return systemAppsSet != null && systemAppsSet.contains(packageName);
    }

    /**
     * Sets total consume power, and adjusts the percentages to ensure the total round percentage
     * could be 100%, and then sorts entries based on the sorting key.
     */
    @VisibleForTesting
    static void processAndSortEntries(final List<BatteryDiffEntry> batteryDiffEntries) {
        if (batteryDiffEntries.isEmpty()) {
            return;
        }

        // Sets total consume power.
        double totalConsumePower = 0.0;
        for (BatteryDiffEntry batteryDiffEntry : batteryDiffEntries) {
            totalConsumePower += batteryDiffEntry.mConsumePower;
        }
        for (BatteryDiffEntry batteryDiffEntry : batteryDiffEntries) {
            batteryDiffEntry.setTotalConsumePower(totalConsumePower);
        }

        // Adjusts percentages to show.
        // The lower bound is treating all the small percentages as 0.
        // The upper bound is treating all the small percentages as 1.
        int totalLowerBound = 0;
        int totalUpperBound = 0;
        for (BatteryDiffEntry entry : batteryDiffEntries) {
            if (entry.getPercentage() < SMALL_PERCENTAGE_THRESHOLD) {
                totalUpperBound += 1;
            } else {
                int roundPercentage = Math.round((float) entry.getPercentage());
                totalLowerBound += roundPercentage;
                totalUpperBound += roundPercentage;
            }
        }
        if (totalLowerBound > 100 || totalUpperBound < 100) {
            Collections.sort(batteryDiffEntries, BatteryDiffEntry.COMPARATOR);
            for (int i = 0; i < totalLowerBound - 100 && i < batteryDiffEntries.size(); i++) {
                batteryDiffEntries.get(i).setAdjustPercentageOffset(-1);
            }
            for (int i = 0; i < 100 - totalUpperBound && i < batteryDiffEntries.size(); i++) {
                batteryDiffEntries.get(i).setAdjustPercentageOffset(1);
            }
        }

        // Sorts entries.
        Collections.sort(batteryDiffEntries, BatteryDiffEntry.COMPARATOR);
    }
}
