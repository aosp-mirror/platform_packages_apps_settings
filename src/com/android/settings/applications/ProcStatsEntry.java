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
import android.content.pm.PackageManager;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;
import android.util.LongSparseArray;

import com.android.internal.app.procstats.ProcessState;
import com.android.internal.app.procstats.ProcessStats;
import com.android.internal.app.procstats.ServiceState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public final class ProcStatsEntry implements Parcelable {

    private static final String TAG = "ProcStatsEntry";
    private static boolean DEBUG = ProcessStatsUi.DEBUG;

    final String mPackage;
    final int mUid;
    final String mName;
    public CharSequence mLabel;
    final ArrayList<String> mPackages = new ArrayList<>();
    final long mBgDuration;
    final long mAvgBgMem;
    final long mMaxBgMem;
    final double mBgWeight;
    final long mRunDuration;
    final long mAvgRunMem;
    final long mMaxRunMem;
    final double mRunWeight;

    String mBestTargetPackage;

    ArrayMap<String, ArrayList<Service>> mServices = new ArrayMap<>(1);

    public ProcStatsEntry(ProcessState proc, String packageName,
            ProcessStats.ProcessDataCollection tmpBgTotals,
            ProcessStats.ProcessDataCollection tmpRunTotals, boolean useUss) {
        proc.computeProcessData(tmpBgTotals, 0);
        proc.computeProcessData(tmpRunTotals, 0);
        mPackage = proc.getPackage();
        mUid = proc.getUid();
        mName = proc.getName();
        mPackages.add(packageName);
        mBgDuration = tmpBgTotals.totalTime;
        mAvgBgMem = useUss ? tmpBgTotals.avgUss : tmpBgTotals.avgPss;
        mMaxBgMem = useUss ? tmpBgTotals.maxUss : tmpBgTotals.maxPss;
        mBgWeight = mAvgBgMem * (double) mBgDuration;
        mRunDuration = tmpRunTotals.totalTime;
        mAvgRunMem = useUss ? tmpRunTotals.avgUss : tmpRunTotals.avgPss;
        mMaxRunMem = useUss ? tmpRunTotals.maxUss : tmpRunTotals.maxPss;
        mRunWeight = mAvgRunMem * (double) mRunDuration;
        if (DEBUG) Log.d(TAG, "New proc entry " + proc.getName() + ": dur=" + mBgDuration
                + " avgpss=" + mAvgBgMem + " weight=" + mBgWeight);
    }

    public ProcStatsEntry(String pkgName, int uid, String procName, long duration, long mem,
            long memDuration) {
        mPackage = pkgName;
        mUid = uid;
        mName = procName;
        mBgDuration = mRunDuration = duration;
        mAvgBgMem = mMaxBgMem = mAvgRunMem = mMaxRunMem = mem;
        mBgWeight = mRunWeight = ((double)memDuration) * mem;
        if (DEBUG) Log.d(TAG, "New proc entry " + procName + ": dur=" + mBgDuration
                + " avgpss=" + mAvgBgMem + " weight=" + mBgWeight);
    }

    public ProcStatsEntry(Parcel in) {
        mPackage = in.readString();
        mUid = in.readInt();
        mName = in.readString();
        in.readStringList(mPackages);
        mBgDuration = in.readLong();
        mAvgBgMem = in.readLong();
        mMaxBgMem = in.readLong();
        mBgWeight = in.readDouble();
        mRunDuration = in.readLong();
        mAvgRunMem = in.readLong();
        mMaxRunMem = in.readLong();
        mRunWeight = in.readDouble();
        mBestTargetPackage = in.readString();
        final int N = in.readInt();
        if (N > 0) {
            mServices.ensureCapacity(N);
            for (int i=0; i<N; i++) {
                String key = in.readString();
                ArrayList<Service> value = new ArrayList<Service>();
                in.readTypedList(value, Service.CREATOR);
                mServices.append(key, value);
            }
        }
    }

    public void addPackage(String packageName) {
        mPackages.add(packageName);
    }

    public void evaluateTargetPackage(PackageManager pm, ProcessStats stats,
            ProcessStats.ProcessDataCollection bgTotals,
            ProcessStats.ProcessDataCollection runTotals, Comparator<ProcStatsEntry> compare,
            boolean useUss) {
        mBestTargetPackage = null;
        if (mPackages.size() == 1) {
            if (DEBUG) Log.d(TAG, "Eval pkg of " + mName + ": single pkg " + mPackages.get(0));
            mBestTargetPackage = mPackages.get(0);
            return;
        }

        // If one of the packages is the framework itself, that wins.
        // See if there is one significant package that was running here.
        for (int ipkg=0; ipkg<mPackages.size(); ipkg++) {
            if ("android".equals(mPackages.get(ipkg))) {
                mBestTargetPackage = mPackages.get(ipkg);
                return;
            }
        }

        // Collect information about each package running in the process.
        ArrayList<ProcStatsEntry> subProcs = new ArrayList<>();
        for (int ipkg=0; ipkg<mPackages.size(); ipkg++) {
            LongSparseArray<ProcessStats.PackageState> vpkgs
                    = stats.mPackages.get(mPackages.get(ipkg), mUid);
            for (int ivers=0;  ivers<vpkgs.size(); ivers++) {
                ProcessStats.PackageState pkgState = vpkgs.valueAt(ivers);
                if (DEBUG) Log.d(TAG, "Eval pkg of " + mName + ", pkg "
                        + pkgState + ":");
                if (pkgState == null) {
                    Log.w(TAG, "No package state found for " + mPackages.get(ipkg) + "/"
                            + mUid + " in process " + mName);
                    continue;
                }
                ProcessState pkgProc = pkgState.mProcesses.get(mName);
                if (pkgProc == null) {
                    Log.w(TAG, "No process " + mName + " found in package state "
                            + mPackages.get(ipkg) + "/" + mUid);
                    continue;
                }
                subProcs.add(new ProcStatsEntry(pkgProc, pkgState.mPackageName, bgTotals,
                        runTotals, useUss));
            }
        }

        if (subProcs.size() > 1) {
            Collections.sort(subProcs, compare);
            if (subProcs.get(0).mRunWeight > (subProcs.get(1).mRunWeight *3)) {
                if (DEBUG) Log.d(TAG, "Eval pkg of " + mName + ": best pkg "
                        + subProcs.get(0).mPackage + " weight " + subProcs.get(0).mRunWeight
                        + " better than " + subProcs.get(1).mPackage
                        + " weight " + subProcs.get(1).mRunWeight);
                mBestTargetPackage = subProcs.get(0).mPackage;
                return;
            }
            // Couldn't find one that is best by weight, let's decide on best another
            // way: the one that has the longest running service, accounts for at least
            // half of the maximum weight, and has specified an explicit app icon.
            double maxWeight = subProcs.get(0).mRunWeight;
            long bestRunTime = -1;
            boolean bestPersistent = false;
            for (int i=0; i<subProcs.size(); i++) {
                final ProcStatsEntry subProc = subProcs.get(i);
                if (subProc.mRunWeight < (maxWeight/2)) {
                    if (DEBUG) Log.d(TAG, "Eval pkg of " + mName + ": pkg "
                            + subProc.mPackage + " weight " + subProc.mRunWeight
                            + " too small");
                    continue;
                }
                try {
                    ApplicationInfo ai = pm.getApplicationInfo(subProc.mPackage, 0);
                    if (ai.icon == 0) {
                        if (DEBUG) Log.d(TAG, "Eval pkg of " + mName + ": pkg "
                                + subProc.mPackage + " has no icon");
                        continue;
                    }
                    if ((ai.flags&ApplicationInfo.FLAG_PERSISTENT) != 0) {
                        long thisRunTime = subProc.mRunDuration;
                        if (!bestPersistent || thisRunTime > bestRunTime) {
                            if (DEBUG) Log.d(TAG, "Eval pkg of " + mName + ": pkg "
                                    + subProc.mPackage + " new best pers run time "
                                    + thisRunTime);
                            bestRunTime = thisRunTime;
                            bestPersistent = true;
                        } else {
                            if (DEBUG) Log.d(TAG, "Eval pkg of " + mName + ": pkg "
                                    + subProc.mPackage + " pers run time " + thisRunTime
                                    + " not as good as last " + bestRunTime);
                        }
                        continue;
                    } else if (bestPersistent) {
                        if (DEBUG) Log.d(TAG, "Eval pkg of " + mName + ": pkg "
                                + subProc.mPackage + " is not persistent");
                        continue;
                    }
                } catch (PackageManager.NameNotFoundException e) {
                    if (DEBUG) Log.d(TAG, "Eval pkg of " + mName + ": pkg "
                            + subProc.mPackage + " failed finding app info");
                    continue;
                }
                ArrayList<Service> subProcServices = null;
                for (int isp=0, NSP=mServices.size(); isp<NSP; isp++) {
                    ArrayList<Service> subServices = mServices.valueAt(isp);
                    if (subServices.get(0).mPackage.equals(subProc.mPackage)) {
                        subProcServices = subServices;
                        break;
                    }
                }
                long thisRunTime = 0;
                if (subProcServices != null) {
                    for (int iss=0, NSS=subProcServices.size(); iss<NSS; iss++) {
                        Service service = subProcServices.get(iss);
                        if (service.mDuration > thisRunTime) {
                            if (DEBUG) Log.d(TAG, "Eval pkg of " + mName + ": pkg "
                                    + subProc.mPackage + " service " + service.mName
                                    + " run time is " + service.mDuration);
                            thisRunTime = service.mDuration;
                            break;
                        }
                    }
                }
                if (thisRunTime > bestRunTime) {
                    if (DEBUG) Log.d(TAG, "Eval pkg of " + mName + ": pkg "
                            + subProc.mPackage + " new best run time " + thisRunTime);
                    mBestTargetPackage = subProc.mPackage;
                    bestRunTime = thisRunTime;
                } else {
                    if (DEBUG) Log.d(TAG, "Eval pkg of " + mName + ": pkg "
                            + subProc.mPackage + " run time " + thisRunTime
                            + " not as good as last " + bestRunTime);
                }
            }
            // Final fallback, just pick the first subProc.
            if (TextUtils.isEmpty(mBestTargetPackage)) {
                mBestTargetPackage = subProcs.get(0).mPackage;
            }
        } else if (subProcs.size() == 1) {
            mBestTargetPackage = subProcs.get(0).mPackage;
        }
    }

    public void addService(ServiceState svc) {
        ArrayList<Service> services = mServices.get(svc.getPackage());
        if (services == null) {
            services = new ArrayList<Service>();
            mServices.put(svc.getPackage(), services);
        }
        services.add(new Service(svc));
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
        dest.writeStringList(mPackages);
        dest.writeLong(mBgDuration);
        dest.writeLong(mAvgBgMem);
        dest.writeLong(mMaxBgMem);
        dest.writeDouble(mBgWeight);
        dest.writeLong(mRunDuration);
        dest.writeLong(mAvgRunMem);
        dest.writeLong(mMaxRunMem);
        dest.writeDouble(mRunWeight);
        dest.writeString(mBestTargetPackage);
        final int N = mServices.size();
        dest.writeInt(N);
        for (int i=0; i<N; i++) {
            dest.writeString(mServices.keyAt(i));
            dest.writeTypedList(mServices.valueAt(i));
        }
    }

    public int getUid() {
        return mUid;
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

        public Service(ServiceState service) {
            mPackage = service.getPackage();
            mName = service.getName();
            mProcess = service.getProcessName();
            mDuration = service.dumpTime(null, null,
                    ServiceState.SERVICE_RUN, ProcessStats.STATE_NOTHING, 0, 0);
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
