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

import java.util.ArrayList;

public final class ProcStatsEntry {
    final String mPackage;
    final int mUid;
    final String mName;
    final boolean mUnique;
    final long mDuration;
    final long mAvgPss;
    final long mWeight;

    ArrayList<Service> mServices;

    public ProcStatsEntry(ProcessStats.ProcessState proc,
            ProcessStats.ProcessDataCollection tmpTotals) {
        ProcessStats.computeProcessData(proc, tmpTotals, 0);
        mPackage = proc.mPackage;
        mUid = proc.mUid;
        mName = proc.mName;
        mUnique = proc.mCommonProcess == proc;
        mDuration = tmpTotals.totalTime;
        mAvgPss = tmpTotals.avgPss;
        mWeight = mDuration * mAvgPss;
    }

    public void addServices(ProcessStats.PackageState pkgState) {
        for (int isvc=0, NSVC=pkgState.mServices.size(); isvc<NSVC; isvc++) {
            ProcessStats.ServiceState svc = pkgState.mServices.valueAt(isvc);
            // XXX can't tell what process it is in!
            if (mServices == null) {
                mServices = new ArrayList<Service>();
            }
            mServices.add(new Service(svc));
        }
    }

    public static final class Service {
        final String mPackage;
        final String mName;
        final long mDuration;

        public Service(ProcessStats.ServiceState service) {
            mPackage = service.mPackage;
            mName = service.mName;
            mDuration = ProcessStats.dumpSingleServiceTime(null, null, service,
                    ProcessStats.ServiceState.SERVICE_STARTED,
                    ProcessStats.STATE_NOTHING, 0, 0)
                + ProcessStats.dumpSingleServiceTime(null, null, service,
                    ProcessStats.ServiceState.SERVICE_BOUND,
                    ProcessStats.STATE_NOTHING, 0, 0)
                + ProcessStats.dumpSingleServiceTime(null, null, service,
                    ProcessStats.ServiceState.SERVICE_EXEC,
                    ProcessStats.STATE_NOTHING, 0, 0);
        }
    }
}
