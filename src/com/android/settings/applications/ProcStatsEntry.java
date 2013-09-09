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

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import android.util.SparseArray;
import com.android.internal.app.ProcessStats;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public final class ProcStatsEntry implements Parcelable {
    private static final String TAG = "ProcStatsEntry";

    final String mPackage;
    final int mUid;
    final String mName;
    final boolean mUnique;
    final long mDuration;
    final long mAvgPss;
    final long mWeight;

    String mBestTargetPackage;

    ArrayList<Service> mServices = new ArrayList<Service>(2);

    public ApplicationInfo mUiTargetApp;
    public String mUiLabel;
    public String mUiBaseLabel;
    public String mUiPackage;

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

    public ProcStatsEntry(Parcel in) {
        mPackage = in.readString();
        mUid = in.readInt();
        mName = in.readString();
        mUnique = in.readInt() != 0;
        mDuration = in.readLong();
        mAvgPss = in.readLong();
        mWeight = in.readLong();
        mBestTargetPackage = in.readString();
        in.readTypedList(mServices, Service.CREATOR);
    }

    public void evaluateTargetPackage(ProcessStats stats,
            ProcessStats.ProcessDataCollection totals, Comparator<ProcStatsEntry> compare) {
        mBestTargetPackage = null;
        if (mUnique) {
            mBestTargetPackage = mPackage;
        } else {
            // See if there is one significant package that was running here.
            ArrayList<ProcStatsEntry> subProcs = new ArrayList<ProcStatsEntry>();
            for (int ipkg=0, NPKG=stats.mPackages.getMap().size(); ipkg<NPKG; ipkg++) {
                SparseArray<ProcessStats.PackageState> uids
                        = stats.mPackages.getMap().valueAt(ipkg);
                for (int iu=0, NU=uids.size(); iu<NU; iu++) {
                    if (uids.keyAt(iu) != mUid) {
                        continue;
                    }
                    ProcessStats.PackageState pkgState = uids.valueAt(iu);
                    for (int iproc=0, NPROC=pkgState.mProcesses.size(); iproc<NPROC; iproc++) {
                        ProcessStats.ProcessState subProc =
                                pkgState.mProcesses.valueAt(iproc);
                        if (subProc.mName.equals(mName)) {
                            subProcs.add(new ProcStatsEntry(subProc, totals));
                        }
                    }
                }
            }
            if (subProcs.size() > 1) {
                Collections.sort(subProcs, compare);
                if (subProcs.get(0).mWeight > (subProcs.get(1).mWeight*3)) {
                    mBestTargetPackage = subProcs.get(0).mPackage;
                }
            }
        }
    }

    public void retrieveUiData(PackageManager pm) {
        mUiTargetApp = null;
        mUiLabel = mUiBaseLabel = mName;
        mUiPackage = mBestTargetPackage;
        if (mUiPackage != null) {
            // Only one app associated with this process.
            try {
                mUiTargetApp = pm.getApplicationInfo(mUiPackage,
                        PackageManager.GET_DISABLED_COMPONENTS |
                        PackageManager.GET_DISABLED_UNTIL_USED_COMPONENTS |
                        PackageManager.GET_UNINSTALLED_PACKAGES);
                String name = mUiBaseLabel = mUiTargetApp.loadLabel(pm).toString();
                if (mName.equals(mUiPackage)) {
                    mUiLabel = name;
                } else {
                    if (mName.startsWith(mUiPackage)) {
                        int off = mUiPackage.length();
                        if (mName.length() > off) {
                            off++;
                        }
                        mUiLabel = name + " (" + mName.substring(off) + ")";
                    } else {
                        mUiLabel = name + " (" + mName + ")";
                    }
                }
            } catch (PackageManager.NameNotFoundException e) {
            }
        }
        if (mUiTargetApp == null) {
            String[] packages = pm.getPackagesForUid(mUid);
            if (packages != null) {
                for (String curPkg : packages) {
                    try {
                        final PackageInfo pi = pm.getPackageInfo(curPkg,
                                PackageManager.GET_DISABLED_COMPONENTS |
                                PackageManager.GET_DISABLED_UNTIL_USED_COMPONENTS |
                                PackageManager.GET_UNINSTALLED_PACKAGES);
                        if (pi.sharedUserLabel != 0) {
                            mUiTargetApp = pi.applicationInfo;
                            final CharSequence nm = pm.getText(curPkg,
                                    pi.sharedUserLabel, pi.applicationInfo);
                            if (nm != null) {
                                mUiBaseLabel = nm.toString();
                                mUiLabel = mUiBaseLabel + " (" + mName + ")";
                            } else {
                                mUiBaseLabel = mUiTargetApp.loadLabel(pm).toString();
                                mUiLabel = mUiBaseLabel + " (" + mName + ")";
                            }
                            break;
                        }
                    } catch (PackageManager.NameNotFoundException e) {
                    }
                }
            } else {
                // no current packages for this uid, typically because of uninstall
                Log.i(TAG, "No package for uid " + mUid);
            }
        }
    }

    public void addService(ProcessStats.ServiceState svc) {
        mServices.add(new Service(svc));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mPackage);
        dest.writeInt(mUid);
        dest.writeString(mName);
        dest.writeInt(mUnique ? 1 : 0);
        dest.writeLong(mDuration);
        dest.writeLong(mAvgPss);
        dest.writeLong(mWeight);
        dest.writeString(mBestTargetPackage);
        dest.writeTypedList(mServices);
    }

    public static final Parcelable.Creator<ProcStatsEntry> CREATOR
            = new Parcelable.Creator<ProcStatsEntry>() {
        public ProcStatsEntry createFromParcel(Parcel in) {
            return new ProcStatsEntry(in);
        }

        public ProcStatsEntry[] newArray(int size) {
            return new ProcStatsEntry[size];
        }
    };

    public static final class Service implements Parcelable {
        final String mPackage;
        final String mName;
        final String mProcess;
        final long mDuration;

        public Service(ProcessStats.ServiceState service) {
            mPackage = service.mPackage;
            mName = service.mName;
            mProcess = service.mProcessName;
            mDuration = ProcessStats.dumpSingleServiceTime(null, null, service,
                    ProcessStats.ServiceState.SERVICE_RUN,
                    ProcessStats.STATE_NOTHING, 0, 0);
        }

        public Service(Parcel in) {
            mPackage = in.readString();
            mName = in.readString();
            mProcess = in.readString();
            mDuration = in.readLong();
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(mPackage);
            dest.writeString(mName);
            dest.writeString(mProcess);
            dest.writeLong(mDuration);
        }

        public static final Parcelable.Creator<Service> CREATOR
                = new Parcelable.Creator<Service>() {
            public Service createFromParcel(Parcel in) {
                return new Service(in);
            }

            public Service[] newArray(int size) {
                return new Service[size];
            }
        };
    }
}
