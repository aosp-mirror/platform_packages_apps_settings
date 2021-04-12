/*
 * Copyright (C) 2021 The Android Open Source Project
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

import java.time.Duration;

/** A container class to carry battery data in a specific time slot. */
public final class BatteryDiffEntry {
    private static final String TAG = "BatteryDiffEntry";

    public final long mForegroundUsageTimeInMs;
    public final long mBackgroundUsageTimeInMs;
    public final double mConsumePower;
    // A BatteryHistEntry corresponding to this diff usage daata.
    public final BatteryHistEntry mBatteryHistEntry;

    private double mTotalConsumePower;
    private double mPercentOfTotal;

    public BatteryDiffEntry(
            long foregroundUsageTimeInMs,
            long backgroundUsageTimeInMs,
            double consumePower,
            BatteryHistEntry batteryHistEntry) {
        mForegroundUsageTimeInMs = foregroundUsageTimeInMs;
        mBackgroundUsageTimeInMs = backgroundUsageTimeInMs;
        mConsumePower = consumePower;
        mBatteryHistEntry = batteryHistEntry;
    }

    /** Sets the total consumed power in a specific time slot. */
    public void setTotalConsumePower(double totalConsumePower) {
        mTotalConsumePower = totalConsumePower;
        mPercentOfTotal = totalConsumePower == 0
            ? 0 : (mConsumePower / mTotalConsumePower) * 100.0;
    }

    /** Gets the percentage of total consumed power. */
    public double getPercentOfTotal() {
        return mPercentOfTotal;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder()
            .append("BatteryDiffEntry{")
            .append("\n\tname=" + mBatteryHistEntry.mAppLabel)
            .append(String.format("\n\tconsume=%.2f%% %f/%f",
                  mPercentOfTotal, mConsumePower, mTotalConsumePower))
            .append(String.format("\n\tforeground:%d background:%d",
                  Duration.ofMillis(mForegroundUsageTimeInMs).getSeconds(),
                  Duration.ofMillis(mBackgroundUsageTimeInMs).getSeconds()))
            .append(String.format("\n\tpackage:%s uid:%s",
                  mBatteryHistEntry.mPackageName, mBatteryHistEntry.mUid));
        return builder.toString();
    }
}
