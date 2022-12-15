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

    /** Constructor for the diff entries. */
    public BatteryDiffData(
            @NonNull List<BatteryDiffEntry> appDiffEntries,
            @NonNull List<BatteryDiffEntry> systemDiffEntries) {
        mAppEntries = appDiffEntries;
        mSystemEntries = systemDiffEntries;
    }

    public List<BatteryDiffEntry> getAppDiffEntryList() {
        return mAppEntries;
    }

    public List<BatteryDiffEntry> getSystemDiffEntryList() {
        return mSystemEntries;
    }

    // Sorts entries based on consumed percentage.
    void sortEntries() {
        Collections.sort(mAppEntries, BatteryDiffEntry.COMPARATOR);
        Collections.sort(mSystemEntries, BatteryDiffEntry.COMPARATOR);
    }

    // Sets total consume power for app and system entries separately.
    void setTotalConsumePower() {
        setTotalConsumePowerForAllEntries(mAppEntries);
        setTotalConsumePowerForAllEntries(mSystemEntries);
    }

    // Sets total consume power for each entry.
    private void setTotalConsumePowerForAllEntries(List<BatteryDiffEntry> batteryDiffEntries) {
        double totalConsumePower = 0.0;
        for (BatteryDiffEntry batteryDiffEntry : batteryDiffEntries) {
            totalConsumePower += batteryDiffEntry.mConsumePower;
        }
        for (BatteryDiffEntry batteryDiffEntry : batteryDiffEntries) {
            batteryDiffEntry.setTotalConsumePower(totalConsumePower);
        }
    }
}
