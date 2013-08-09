/*
 * Copyright (C) 2013 The Android Open Source Project
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

import com.android.internal.app.ProcessStats;

public class ProcStatsEntry {
    final String mPackage;
    final int mUid;
    final String mName;
    final boolean mUnique;
    final long mDuration;
    final long mAvgPss;
    final long mWeight;

    ProcStatsEntry(ProcessStats.ProcessState proc, ProcessStats.ProcessDataCollection tmpTotals) {
        ProcessStats.computeProcessData(proc, tmpTotals, 0);
        mPackage = proc.mPackage;
        mUid = proc.mUid;
        mName = proc.mName;
        mUnique = proc.mCommonProcess == proc;
        mDuration = tmpTotals.totalTime;
        mAvgPss = tmpTotals.avgPss;
        mWeight = mDuration * mAvgPss;
    }
}
