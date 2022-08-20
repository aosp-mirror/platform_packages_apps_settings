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

import androidx.annotation.NonNull;

import java.util.Collections;
import java.util.List;

/** Wraps the battery usage diff data for each entry used for battery usage app list. */
public class BatteryDiffData {
    private final List<BatteryDiffEntry> mAppEntries;
    private final List<BatteryDiffEntry> mSystemEntries;

    /** Constructor for the diff entries which already have totalConsumePower value. */
    public BatteryDiffData(
            @NonNull List<BatteryDiffEntry> appDiffEntries,
            @NonNull List<BatteryDiffEntry> systemDiffEntries) {
        mAppEntries = appDiffEntries;
        mSystemEntries = systemDiffEntries;
        sortEntries();
    }

    /** Constructor for the diff entries which have not set totalConsumePower value. */
    public BatteryDiffData(
            @NonNull List<BatteryDiffEntry> appDiffEntries,
            @NonNull List<BatteryDiffEntry> systemDiffEntries,
            final double totalConsumePower) {
        mAppEntries = appDiffEntries;
        mSystemEntries = systemDiffEntries;
        setTotalConsumePowerForAllEntries(totalConsumePower);
        sortEntries();
    }

    public List<BatteryDiffEntry> getAppDiffEntryList() {
        return mAppEntries;
    }

    public List<BatteryDiffEntry> getSystemDiffEntryList() {
        return mSystemEntries;
    }

    // Sets total consume power for each entry.
    private void setTotalConsumePowerForAllEntries(final double totalConsumePower) {
        mAppEntries.forEach(diffEntry -> diffEntry.setTotalConsumePower(totalConsumePower));
        mSystemEntries.forEach(diffEntry -> diffEntry.setTotalConsumePower(totalConsumePower));
    }

    // Sorts entries based on consumed percentage.
    private void sortEntries() {
        Collections.sort(mAppEntries, BatteryDiffEntry.COMPARATOR);
        Collections.sort(mSystemEntries, BatteryDiffEntry.COMPARATOR);
    }
}
