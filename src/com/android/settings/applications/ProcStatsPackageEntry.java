/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.settings.applications;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.android.settings.R;
import com.android.settings.Utils;

import java.util.ArrayList;

public class ProcStatsPackageEntry implements Parcelable {
    private static final String TAG = "ProcStatsEntry";
    private static boolean DEBUG = ProcessStatsUi.DEBUG;

    private static final float ALWAYS_THRESHOLD = .95f;
    private static final float SOMETIMES_THRESHOLD = .25f;

    final String mPackage;
    final ArrayList<ProcStatsEntry> mEntries = new ArrayList<ProcStatsEntry>();

    long mBgDuration;
    long mAvgBgMem;
    long mMaxBgMem;
    double mBgWeight;
    long mRunDuration;
    long mAvgRunMem;
    long mMaxRunMem;
    double mRunWeight;

    public ApplicationInfo mUiTargetApp;
    public String mUiLabel;
    private long mWindowLength;

    public ProcStatsPackageEntry(String pkg, long windowLength) {
        mPackage = pkg;
        mWindowLength = windowLength;
    }

    public ProcStatsPackageEntry(Parcel in) {
        mPackage = in.readString();
        in.readTypedList(mEntries, ProcStatsEntry.CREATOR);
        mBgDuration = in.readLong();
        mAvgBgMem = in.readLong();
        mMaxBgMem = in.readLong();
        mBgWeight = in.readDouble();
        mRunDuration = in.readLong();
        mAvgRunMem = in.readLong();
        mMaxRunMem = in.readLong();
        mRunWeight = in.readDouble();
    }

    public CharSequence getRunningFrequency(Context context) {
        float amountRunning = mRunDuration / (float) mWindowLength;
        return getFrequency(amountRunning, context);
    }

    public CharSequence getBackgroundFrequency(Context context) {
        float amountRunning = mBgDuration / (float) mWindowLength;
        return getFrequency(amountRunning, context);
    }

    public void addEntry(ProcStatsEntry entry) {
        mEntries.add(entry);
    }

    public void updateMetrics() {
        mBgDuration = mAvgBgMem = mMaxBgMem = 0;
        mBgWeight = 0;
        mRunDuration = mAvgRunMem = mMaxRunMem = 0;
        mRunWeight = 0;
        final int N = mEntries.size();
        for (int i=0; i < N; i++) {
            ProcStatsEntry entry = mEntries.get(i);
            mBgDuration = Math.max(entry.mBgDuration, mBgDuration);
            mAvgBgMem += entry.mAvgBgMem;
            mBgWeight += entry.mBgWeight;
            mRunDuration = Math.max(entry.mRunDuration, mRunDuration);
            mAvgRunMem += entry.mAvgRunMem;
            mRunWeight += entry.mRunWeight;

            // Each entry is generally a process or something similar.  Since it is extremely
            // unlikely that any apps are going to avoid running processes at the same time
            // to avoid memory usage, we will sum the maximum memory usage to create a
            // hypothetical worst case scenario of memory.
            mMaxBgMem += entry.mMaxBgMem;
            mMaxRunMem += entry.mMaxRunMem;
        }
        mAvgBgMem /= N;
        mAvgRunMem /= N;
    }

    public void retrieveUiData(Context context, PackageManager pm) {
        mUiTargetApp = null;
        mUiLabel = mPackage;
        // Only one app associated with this process.
        try {
            if ("os".equals(mPackage)) {
                mUiTargetApp = pm.getApplicationInfo("android",
                        PackageManager.MATCH_DISABLED_COMPONENTS |
                        PackageManager.MATCH_DISABLED_UNTIL_USED_COMPONENTS |
                        PackageManager.MATCH_ANY_USER);
                mUiLabel = context.getString(R.string.process_stats_os_label);
            } else {
                mUiTargetApp = pm.getApplicationInfo(mPackage,
                        PackageManager.MATCH_DISABLED_COMPONENTS |
                        PackageManager.MATCH_DISABLED_UNTIL_USED_COMPONENTS |
                        PackageManager.MATCH_ANY_USER);
                mUiLabel = mUiTargetApp.loadLabel(pm).toString();
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.d(TAG, "could not find package: " + mPackage);
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mPackage);
        dest.writeTypedList(mEntries);
        dest.writeLong(mBgDuration);
        dest.writeLong(mAvgBgMem);
        dest.writeLong(mMaxBgMem);
        dest.writeDouble(mBgWeight);
        dest.writeLong(mRunDuration);
        dest.writeLong(mAvgRunMem);
        dest.writeLong(mMaxRunMem);
        dest.writeDouble(mRunWeight);
    }

    public static final Parcelable.Creator<ProcStatsPackageEntry> CREATOR
            = new Parcelable.Creator<ProcStatsPackageEntry>() {
        public ProcStatsPackageEntry createFromParcel(Parcel in) {
            return new ProcStatsPackageEntry(in);
        }

        public ProcStatsPackageEntry[] newArray(int size) {
            return new ProcStatsPackageEntry[size];
        }
    };

    // TODO: Find better place for this.
    public static CharSequence getFrequency(float amount, Context context) {
        if (amount > ALWAYS_THRESHOLD) {
            return context.getString(R.string.always_running,
                    Utils.formatPercentage((int) (amount * 100)));
        } else if (amount > SOMETIMES_THRESHOLD) {
            return context.getString(R.string.sometimes_running,
                    Utils.formatPercentage((int) (amount * 100)));
        } else {
            return context.getString(R.string.rarely_running,
                    Utils.formatPercentage((int) (amount * 100)));
        }
    }

    public double getRunWeight() {
        return mRunWeight;
    }

    public double getBgWeight() {
        return mBgWeight;
    }

    public ArrayList<ProcStatsEntry> getEntries() {
        return mEntries;
    }

}
