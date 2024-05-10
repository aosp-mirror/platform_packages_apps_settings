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

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.text.format.Formatter;
import android.util.ArrayMap;
import android.util.Log;
import android.util.LongSparseArray;
import android.util.SparseArray;

import androidx.annotation.WorkerThread;

import com.android.internal.app.ProcessMap;
import com.android.internal.app.procstats.DumpUtils;
import com.android.internal.app.procstats.IProcessStats;
import com.android.internal.app.procstats.ProcessState;
import com.android.internal.app.procstats.ProcessStats;
import com.android.internal.app.procstats.ProcessStats.ProcessDataCollection;
import com.android.internal.app.procstats.ProcessStats.TotalMemoryUseCollection;
import com.android.internal.app.procstats.ServiceState;
import com.android.internal.util.MemInfoReader;
import com.android.settings.R;
import com.android.settings.Utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ProcStatsData {

    private static final String TAG = "ProcStatsManager";

    private static final boolean DEBUG = ProcessStatsUi.DEBUG;

    private static ProcessStats sStatsXfer;

    private PackageManager mPm;
    private Context mContext;
    private long memTotalTime;

    private IProcessStats mProcessStats;
    private ProcessStats mStats;

    private boolean mUseUss;
    private long mDuration;

    private int[] mMemStates;

    private int[] mStates;

    private MemInfo mMemInfo;

    private ArrayList<ProcStatsPackageEntry> pkgEntries;

    public ProcStatsData(Context context, boolean useXfer) {
        mContext = context;
        mPm = context.getPackageManager();
        mProcessStats = IProcessStats.Stub.asInterface(
                ServiceManager.getService(ProcessStats.SERVICE_NAME));
        mMemStates = ProcessStats.ALL_MEM_ADJ;
        mStates = ProcessStats.BACKGROUND_PROC_STATES;
        if (useXfer) {
            mStats = sStatsXfer;
        }
    }

    public void xferStats() {
        sStatsXfer = mStats;
    }

    public int getMemState() {
        int factor = mStats.mMemFactor;
        if (factor == ProcessStats.ADJ_NOTHING) {
            return ProcessStats.ADJ_MEM_FACTOR_NORMAL;
        }
        if (factor >= ProcessStats.ADJ_SCREEN_ON) {
            factor -= ProcessStats.ADJ_SCREEN_ON;
        }
        return factor;
    }

    public MemInfo getMemInfo() {
        return mMemInfo;
    }

    /**
     * Sets the duration.
     *
     * <p>Note: {@link #refreshStats(boolean)} needs to called manually to take effect.
     */
    public void setDuration(long duration) {
        mDuration = duration;
    }

    public long getDuration() {
        return mDuration;
    }

    public List<ProcStatsPackageEntry> getEntries() {
        return pkgEntries;
    }

    /**
     * Refreshes the stats.
     *
     * <p>Note: This needs to be called manually to take effect.
     */
    @WorkerThread
    public void refreshStats(boolean forceLoad) {
        if (mStats == null || forceLoad) {
            load();
        }

        pkgEntries = new ArrayList<>();

        long now = SystemClock.uptimeMillis();

        memTotalTime = DumpUtils.dumpSingleTime(null, null, mStats.mMemFactorDurations,
                mStats.mMemFactor, mStats.mStartTime, now);

        ProcessStats.TotalMemoryUseCollection totalMem = new ProcessStats.TotalMemoryUseCollection(
                ProcessStats.ALL_SCREEN_ADJ, mMemStates);
        mStats.computeTotalMemoryUse(totalMem, now);

        mMemInfo = new MemInfo(mContext, totalMem, memTotalTime);

        ProcessDataCollection bgTotals = new ProcessDataCollection(
                ProcessStats.ALL_SCREEN_ADJ, mMemStates, mStates);
        ProcessDataCollection runTotals = new ProcessDataCollection(
                ProcessStats.ALL_SCREEN_ADJ, mMemStates, ProcessStats.NON_CACHED_PROC_STATES);

        createPkgMap(getProcs(bgTotals, runTotals), bgTotals, runTotals);
        if (totalMem.sysMemZRamWeight > 0 && !totalMem.hasSwappedOutPss) {
            distributeZRam(totalMem.sysMemZRamWeight);
        }

        ProcStatsPackageEntry osPkg = createOsEntry(bgTotals, runTotals, totalMem,
                mMemInfo.baseCacheRam);
        pkgEntries.add(osPkg);
    }

    private void createPkgMap(ArrayList<ProcStatsEntry> procEntries, ProcessDataCollection bgTotals,
            ProcessDataCollection runTotals) {
        // Combine processes into packages.
        ArrayMap<String, ProcStatsPackageEntry> pkgMap = new ArrayMap<>();
        for (int i = procEntries.size() - 1; i >= 0; i--) {
            ProcStatsEntry proc = procEntries.get(i);
            proc.evaluateTargetPackage(mPm, mStats, bgTotals, runTotals, sEntryCompare, mUseUss);
            ProcStatsPackageEntry pkg = pkgMap.get(proc.mBestTargetPackage);
            if (pkg == null) {
                pkg = new ProcStatsPackageEntry(proc.mBestTargetPackage, memTotalTime);
                pkgMap.put(proc.mBestTargetPackage, pkg);
                pkgEntries.add(pkg);
            }
            pkg.addEntry(proc);
        }
    }

    private void distributeZRam(double zramWeight) {
        // Distribute kernel's Z-Ram across processes, based on how much they have been running.
        // The idea is that the memory used by the kernel for this is not really the kernel's
        // responsibility, but that of whoever got swapped in to it...  and we will take how
        // much a process runs for as a sign of the proportion of Z-Ram it is responsible for.

        long zramMem = (long) (zramWeight / memTotalTime);
        long totalTime = 0;
        for (int i = pkgEntries.size() - 1; i >= 0; i--) {
            ProcStatsPackageEntry entry = pkgEntries.get(i);
            for (int j = entry.mEntries.size() - 1; j >= 0; j--) {
                ProcStatsEntry proc = entry.mEntries.get(j);
                totalTime += proc.mRunDuration;
            }
        }
        for (int i = pkgEntries.size() - 1; i >= 0 && totalTime > 0; i--) {
            ProcStatsPackageEntry entry = pkgEntries.get(i);
            long pkgRunTime = 0;
            long maxRunTime = 0;
            for (int j = entry.mEntries.size() - 1; j >= 0; j--) {
                ProcStatsEntry proc = entry.mEntries.get(j);
                pkgRunTime += proc.mRunDuration;
                if (proc.mRunDuration > maxRunTime) {
                    maxRunTime = proc.mRunDuration;
                }
            }
            long pkgZRam = (zramMem*pkgRunTime)/totalTime;
            if (pkgZRam > 0) {
                zramMem -= pkgZRam;
                totalTime -= pkgRunTime;
                ProcStatsEntry procEntry = new ProcStatsEntry(entry.mPackage, 0,
                        mContext.getString(R.string.process_stats_os_zram), maxRunTime,
                        pkgZRam, memTotalTime);
                procEntry.evaluateTargetPackage(mPm, mStats, null, null, sEntryCompare, mUseUss);
                entry.addEntry(procEntry);
            }
        }
    }

    private ProcStatsPackageEntry createOsEntry(ProcessDataCollection bgTotals,
            ProcessDataCollection runTotals, TotalMemoryUseCollection totalMem, long baseCacheRam) {
        // Add in fake entry representing the OS itself.
        ProcStatsPackageEntry osPkg = new ProcStatsPackageEntry("os", memTotalTime);
        ProcStatsEntry osEntry;
        if (totalMem.sysMemNativeWeight > 0) {
            osEntry = new ProcStatsEntry(Utils.OS_PKG, 0,
                    mContext.getString(R.string.process_stats_os_native), memTotalTime,
                    (long) (totalMem.sysMemNativeWeight / memTotalTime), memTotalTime);
            osEntry.evaluateTargetPackage(mPm, mStats, bgTotals, runTotals, sEntryCompare, mUseUss);
            osPkg.addEntry(osEntry);
        }
        if (totalMem.sysMemKernelWeight > 0) {
            osEntry = new ProcStatsEntry(Utils.OS_PKG, 0,
                    mContext.getString(R.string.process_stats_os_kernel), memTotalTime,
                    (long) (totalMem.sysMemKernelWeight / memTotalTime), memTotalTime);
            osEntry.evaluateTargetPackage(mPm, mStats, bgTotals, runTotals, sEntryCompare, mUseUss);
            osPkg.addEntry(osEntry);
        }
        /*  Turned off now -- zram is being distributed across running apps.
        if (totalMem.sysMemZRamWeight > 0) {
            osEntry = new ProcStatsEntry(Utils.OS_PKG, 0,
                    mContext.getString(R.string.process_stats_os_zram), memTotalTime,
                    (long) (totalMem.sysMemZRamWeight / memTotalTime));
            osEntry.evaluateTargetPackage(mPm, mStats, bgTotals, runTotals, sEntryCompare, mUseUss);
            osPkg.addEntry(osEntry);
        }
        */
        if (baseCacheRam > 0) {
            osEntry = new ProcStatsEntry(Utils.OS_PKG, 0,
                    mContext.getString(R.string.process_stats_os_cache), memTotalTime,
                    baseCacheRam / 1024, memTotalTime);
            osEntry.evaluateTargetPackage(mPm, mStats, bgTotals, runTotals, sEntryCompare, mUseUss);
            osPkg.addEntry(osEntry);
        }
        return osPkg;
    }

    private ArrayList<ProcStatsEntry> getProcs(ProcessDataCollection bgTotals,
            ProcessDataCollection runTotals) {
        final ArrayList<ProcStatsEntry> procEntries = new ArrayList<>();
        if (DEBUG) Log.d(TAG, "-------------------- PULLING PROCESSES");

        final ProcessMap<ProcStatsEntry> entriesMap = new ProcessMap<ProcStatsEntry>();
        for (int ipkg = 0, N = mStats.mPackages.getMap().size(); ipkg < N; ipkg++) {
            final SparseArray<LongSparseArray<ProcessStats.PackageState>> pkgUids = mStats.mPackages
                    .getMap().valueAt(ipkg);
            for (int iu = 0; iu < pkgUids.size(); iu++) {
                final LongSparseArray<ProcessStats.PackageState> vpkgs = pkgUids.valueAt(iu);
                for (int iv = 0; iv < vpkgs.size(); iv++) {
                    final ProcessStats.PackageState st = vpkgs.valueAt(iv);
                    for (int iproc = 0; iproc < st.mProcesses.size(); iproc++) {
                        final ProcessState pkgProc = st.mProcesses.valueAt(iproc);
                        final ProcessState proc = mStats.mProcesses.get(pkgProc.getName(),
                                pkgProc.getUid());
                        if (proc == null) {
                            Log.w(TAG, "No process found for pkg " + st.mPackageName
                                    + "/" + st.mUid + " proc name " + pkgProc.getName());
                            continue;
                        }
                        ProcStatsEntry ent = entriesMap.get(proc.getName(), proc.getUid());
                        if (ent == null) {
                            ent = new ProcStatsEntry(proc, st.mPackageName, bgTotals, runTotals,
                                    mUseUss);
                            if (ent.mRunWeight > 0) {
                                if (DEBUG) Log.d(TAG, "Adding proc " + proc.getName() + "/"
                                            + proc.getUid() + ": time="
                                            + ProcessStatsUi.makeDuration(ent.mRunDuration) + " ("
                                            + ((((double) ent.mRunDuration) / memTotalTime) * 100)
                                            + "%)"
                                            + " pss=" + ent.mAvgRunMem);
                                entriesMap.put(proc.getName(), proc.getUid(), ent);
                                procEntries.add(ent);
                            }
                        } else {
                            ent.addPackage(st.mPackageName);
                        }
                    }
                }
            }
        }

        if (DEBUG) Log.d(TAG, "-------------------- MAPPING SERVICES");

        // Add in service info.
        for (int ip = 0, N = mStats.mPackages.getMap().size(); ip < N; ip++) {
            SparseArray<LongSparseArray<ProcessStats.PackageState>> uids = mStats.mPackages.getMap()
                    .valueAt(ip);
            for (int iu = 0; iu < uids.size(); iu++) {
                LongSparseArray<ProcessStats.PackageState> vpkgs = uids.valueAt(iu);
                for (int iv = 0; iv < vpkgs.size(); iv++) {
                    ProcessStats.PackageState ps = vpkgs.valueAt(iv);
                    for (int is = 0, NS = ps.mServices.size(); is < NS; is++) {
                        ServiceState ss = ps.mServices.valueAt(is);
                        if (ss.getProcessName() != null) {
                            ProcStatsEntry ent = entriesMap.get(ss.getProcessName(),
                                    uids.keyAt(iu));
                            if (ent != null) {
                                if (DEBUG) Log.d(TAG, "Adding service " + ps.mPackageName
                                            + "/" + ss.getName() + "/" + uids.keyAt(iu)
                                            + " to proc " + ss.getProcessName());
                                ent.addService(ss);
                            } else {
                                Log.w(TAG, "No process " + ss.getProcessName() + "/"
                                        + uids.keyAt(iu) + " for service " + ss.getName());
                            }
                        }
                    }
                }
            }
        }

        return procEntries;
    }

    private void load() {
        try {
            ParcelFileDescriptor pfd = mProcessStats.getStatsOverTime(mDuration);
            mStats = new ProcessStats(false);
            InputStream is = new ParcelFileDescriptor.AutoCloseInputStream(pfd);
            mStats.read(is);
            try {
                is.close();
            } catch (IOException e) {
            }
            if (mStats.mReadError != null) {
                Log.w(TAG, "Failure reading process stats: " + mStats.mReadError);
            }
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException:", e);
        }
    }

    public static class MemInfo {
        public double realUsedRam;
        public double realFreeRam;
        public double realTotalRam;
        long baseCacheRam;

        double[] mMemStateWeights = new double[ProcessStats.STATE_COUNT];
        double freeWeight;
        double usedWeight;
        double weightToRam;
        double totalRam;
        double totalScale;
        long memTotalTime;

        public double getWeightToRam() {
            return weightToRam;
        }

        private MemInfo(Context context, ProcessStats.TotalMemoryUseCollection totalMem,
                long memTotalTime) {
            this.memTotalTime = memTotalTime;
            calculateWeightInfo(context, totalMem, memTotalTime);

            double usedRam = (usedWeight * 1024) / memTotalTime;
            double freeRam = (freeWeight * 1024) / memTotalTime;
            totalRam = usedRam + freeRam;
            totalScale = realTotalRam / totalRam;
            weightToRam = totalScale / memTotalTime * 1024;

            realUsedRam = usedRam * totalScale;
            realFreeRam = freeRam * totalScale;
            if (DEBUG) {
                Log.i(TAG, "Scaled Used RAM: " + Formatter.formatShortFileSize(context,
                        (long) realUsedRam));
                Log.i(TAG, "Scaled Free RAM: " + Formatter.formatShortFileSize(context,
                        (long) realFreeRam));
            }
            if (DEBUG) {
                Log.i(TAG, "Adj Scaled Used RAM: " + Formatter.formatShortFileSize(context,
                        (long) realUsedRam));
                Log.i(TAG, "Adj Scaled Free RAM: " + Formatter.formatShortFileSize(context,
                        (long) realFreeRam));
            }

            ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
            ((ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE)).getMemoryInfo(
                    memInfo);
            if (memInfo.hiddenAppThreshold >= realFreeRam) {
                realUsedRam = freeRam;
                realFreeRam = 0;
                baseCacheRam = (long) realFreeRam;
            } else {
                realUsedRam += memInfo.hiddenAppThreshold;
                realFreeRam -= memInfo.hiddenAppThreshold;
                baseCacheRam = memInfo.hiddenAppThreshold;
            }
        }

        private void calculateWeightInfo(Context context, TotalMemoryUseCollection totalMem,
                long memTotalTime) {
            MemInfoReader memReader = new MemInfoReader();
            memReader.readMemInfo();
            realTotalRam = memReader.getTotalSize();
            freeWeight = totalMem.sysMemFreeWeight + totalMem.sysMemCachedWeight;
            usedWeight = totalMem.sysMemKernelWeight + totalMem.sysMemNativeWeight;
            if (!totalMem.hasSwappedOutPss) {
                usedWeight += totalMem.sysMemZRamWeight;
            }
            for (int i = 0; i < ProcessStats.STATE_COUNT; i++) {
                if (i == ProcessStats.STATE_SERVICE_RESTARTING) {
                    // These don't really run.
                    mMemStateWeights[i] = 0;
                } else {
                    mMemStateWeights[i] = totalMem.processStateWeight[i];
                    if (i >= ProcessStats.STATE_HOME) {
                        freeWeight += totalMem.processStateWeight[i];
                    } else {
                        usedWeight += totalMem.processStateWeight[i];
                    }
                }
            }
            if (DEBUG) {
                Log.i(TAG, "Used RAM: " + Formatter.formatShortFileSize(context,
                        (long) ((usedWeight * 1024) / memTotalTime)));
                Log.i(TAG, "Free RAM: " + Formatter.formatShortFileSize(context,
                        (long) ((freeWeight * 1024) / memTotalTime)));
                Log.i(TAG, "Total RAM: " + Formatter.formatShortFileSize(context,
                        (long) (((freeWeight + usedWeight) * 1024) / memTotalTime)));
            }
        }
    }

    final static Comparator<ProcStatsEntry> sEntryCompare = new Comparator<ProcStatsEntry>() {
        @Override
        public int compare(ProcStatsEntry lhs, ProcStatsEntry rhs) {
            if (lhs.mRunWeight < rhs.mRunWeight) {
                return 1;
            } else if (lhs.mRunWeight > rhs.mRunWeight) {
                return -1;
            } else if (lhs.mRunDuration < rhs.mRunDuration) {
                return 1;
            } else if (lhs.mRunDuration > rhs.mRunDuration) {
                return -1;
            }
            return 0;
        }
    };
}
