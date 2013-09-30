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
import android.util.ArrayMap;
import android.util.Log;
import com.android.internal.app.ProcessStats;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public final class ProcStatsEntry implements Parcelable {
    private static final String TAG = "ProcStatsEntry";
    private static boolean DEBUG = ProcessStatsUi.DEBUG;

    final String mPackage;
    final int mUid;
    final String mName;
    final ArrayList<String> mPackages = new ArrayList<String>();
    final long mDuration;
    final long mAvgPss;
    final long mMaxPss;
    final long mAvgUss;
    final long mMaxUss;
    final long mWeight;

    String mBestTargetPackage;

    ArrayMap<String, ArrayList<Service>> mServices = new ArrayMap<String, ArrayList<Service>>(1);

    public ApplicationInfo mUiTargetApp;
    public String mUiLabel;
    public String mUiBaseLabel;
    public String mUiPackage;

    public ProcStatsEntry(ProcessStats.ProcessState proc, String packageName,
            ProcessStats.ProcessDataCollection tmpTotals, boolean useUss, boolean weightWithTime) {
        ProcessStats.computeProcessData(proc, tmpTotals, 0);
        mPackage = proc.mPackage;
        mUid = proc.mUid;
        mName = proc.mName;
        mPackages.add(packageName);
        mDuration = tmpTotals.totalTime;
        mAvgPss = tmpTotals.avgPss;
        mMaxPss = tmpTotals.maxPss;
        mAvgUss = tmpTotals.avgUss;
        mMaxUss = tmpTotals.maxUss;
        mWeight = (weightWithTime ? mDuration : 1) * (useUss ? mAvgUss : mAvgPss);
        if (DEBUG) Log.d(TAG, "New proc entry " + proc.mName + ": dur=" + mDuration
                + " avgpss=" + mAvgPss + " weight=" + mWeight);
    }

    public ProcStatsEntry(Parcel in) {
        mPackage = in.readString();
        mUid = in.readInt();
        mName = in.readString();
        in.readStringList(mPackages);
        mDuration = in.readLong();
        mAvgPss = in.readLong();
        mMaxPss = in.readLong();
        mAvgUss = in.readLong();
        mMaxUss = in.readLong();
        mWeight = in.readLong();
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
            ProcessStats.ProcessDataCollection totals, Comparator<ProcStatsEntry> compare,
            boolean useUss, boolean weightWithTime) {
        mBestTargetPackage = null;
        if (mPackages.size() == 1) {
            if (DEBUG) Log.d(TAG, "Eval pkg of " + mName + ": single pkg " + mPackages.get(0));
            mBestTargetPackage = mPackages.get(0);
        } else {
            // See if there is one significant package that was running here.
            ArrayList<ProcStatsEntry> subProcs = new ArrayList<ProcStatsEntry>();
            for (int ipkg=0; ipkg<mPackages.size(); ipkg++) {
                ProcessStats.PackageState pkgState = stats.mPackages.get(mPackages.get(ipkg), mUid);
                if (DEBUG) Log.d(TAG, "Eval pkg of " + mName + ", pkg "
                        + mPackages.get(ipkg) + ":");
                if (pkgState == null) {
                    Log.w(TAG, "No package state found for " + mPackages.get(ipkg) + "/"
                            + mUid + " in process " + mName);
                    continue;
                }
                ProcessStats.ProcessState pkgProc = pkgState.mProcesses.get(mName);
                if (pkgProc == null) {
                    Log.w(TAG, "No process " + mName + " found in package state "
                            + mPackages.get(ipkg) + "/" + mUid);
                    continue;
                }
                subProcs.add(new ProcStatsEntry(pkgProc, pkgState.mPackageName, totals, useUss,
                        weightWithTime));
            }
            if (subProcs.size() > 1) {
                Collections.sort(subProcs, compare);
                if (subProcs.get(0).mWeight > (subProcs.get(1).mWeight*3)) {
                    if (DEBUG) Log.d(TAG, "Eval pkg of " + mName + ": best pkg "
                            + subProcs.get(0).mPackage + " weight " + subProcs.get(0).mWeight
                            + " better than " + subProcs.get(1).mPackage
                            + " weight " + subProcs.get(1).mWeight);
                    mBestTargetPackage = subProcs.get(0).mPackage;
                    return;
                }
                // Couldn't find one that is best by weight, let's decide on best another
                // way: the one that has the longest running service, accounts for at least
                // half of the maximum weight, and has specified an explicit app icon.
                long maxWeight = subProcs.get(0).mWeight;
                long bestRunTime = -1;
                for (int i=0; i<subProcs.size(); i++) {
                    if (subProcs.get(i).mWeight < (maxWeight/2)) {
                        if (DEBUG) Log.d(TAG, "Eval pkg of " + mName + ": pkg "
                                + subProcs.get(i).mPackage + " weight " + subProcs.get(i).mWeight
                                + " too small");
                        continue;
                    }
                    try {
                        ApplicationInfo ai = pm.getApplicationInfo(subProcs.get(i).mPackage, 0);
                        if (ai.icon == 0) {
                            if (DEBUG) Log.d(TAG, "Eval pkg of " + mName + ": pkg "
                                    + subProcs.get(i).mPackage + " has no icon");
                            continue;
                        }
                    } catch (PackageManager.NameNotFoundException e) {
                        if (DEBUG) Log.d(TAG, "Eval pkg of " + mName + ": pkg "
                                + subProcs.get(i).mPackage + " failed finding app info");
                        continue;
                    }
                    ArrayList<Service> subProcServices = null;
                    for (int isp=0, NSP=mServices.size(); isp<NSP; isp++) {
                        ArrayList<Service> subServices = mServices.valueAt(isp);
                        if (subServices.get(0).mPackage.equals(subProcs.get(i).mPackage)) {
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
                                        + subProcs.get(i).mPackage + " service " + service.mName
                                        + " run time is " + service.mDuration);
                                thisRunTime = service.mDuration;
                                break;
                            }
                        }
                    }
                    if (thisRunTime > bestRunTime) {
                        if (DEBUG) Log.d(TAG, "Eval pkg of " + mName + ": pkg "
                                + subProcs.get(i).mPackage + " new best run time " + thisRunTime);
                        mBestTargetPackage = subProcs.get(i).mPackage;
                        bestRunTime = thisRunTime;
                    } else {
                        if (DEBUG) Log.d(TAG, "Eval pkg of " + mName + ": pkg "
                                + subProcs.get(i).mPackage + " run time " + thisRunTime
                                + " not as good as last " + bestRunTime);
                    }
                }
            } else if (subProcs.size() == 1) {
                mBestTargetPackage = subProcs.get(0).mPackage;
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
        ArrayList<Service> services = mServices.get(svc.mPackage);
        if (services == null) {
            services = new ArrayList<Service>();
            mServices.put(svc.mPackage, services);
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
        dest.writeLong(mDuration);
        dest.writeLong(mAvgPss);
        dest.writeLong(mMaxPss);
        dest.writeLong(mAvgUss);
        dest.writeLong(mMaxUss);
        dest.writeLong(mWeight);
        dest.writeString(mBestTargetPackage);
        final int N = mServices.size();
        dest.writeInt(N);
        for (int i=0; i<N; i++) {
            dest.writeString(mServices.keyAt(i));
            dest.writeTypedList(mServices.valueAt(i));
        }
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
