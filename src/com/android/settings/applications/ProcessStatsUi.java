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

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.UserManager;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.text.format.DateFormat;
import android.util.Log;
import android.util.SparseArray;
import android.util.TimeUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import com.android.internal.app.IProcessStats;
import com.android.internal.app.ProcessStats;
import com.android.settings.R;
import com.android.settings.fuelgauge.Utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class ProcessStatsUi extends PreferenceFragment {
    private static final String TAG = "ProcessStatsUi";
    private static final boolean DEBUG = false;

    private static final String KEY_APP_LIST = "app_list";
    private static final String KEY_MEM_STATUS = "mem_status";

    private static final int MENU_STATS_REFRESH = Menu.FIRST;
    private static final int MENU_HELP = Menu.FIRST + 2;

    static final int MAX_ITEMS_TO_LIST = 20;

    final static Comparator<ProcStatsEntry> sEntryCompare = new Comparator<ProcStatsEntry>() {
        @Override
        public int compare(ProcStatsEntry lhs, ProcStatsEntry rhs) {
            if (lhs.mWeight < rhs.mWeight) {
                return 1;
            } else if (lhs.mWeight > rhs.mWeight) {
                return -1;
            }
            return 0;
        }
    };

    private static ProcessStats sStatsXfer;

    IProcessStats mProcessStats;
    UserManager mUm;
    ProcessStats mStats;
    int mMemState;

    private PreferenceGroup mAppListGroup;
    private Preference mMemStatusPref;

    long mTotalTime;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        if (icicle != null) {
            mStats = sStatsXfer;
        }

        addPreferencesFromResource(R.xml.process_stats_summary);
        mProcessStats = IProcessStats.Stub.asInterface(
                ServiceManager.getService(ProcessStats.SERVICE_NAME));
        mUm = (UserManager)getActivity().getSystemService(Context.USER_SERVICE);
        mAppListGroup = (PreferenceGroup) findPreference(KEY_APP_LIST);
        mMemStatusPref = mAppListGroup.findPreference(KEY_MEM_STATUS);
        setHasOptionsMenu(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshStats();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (getActivity().isChangingConfigurations()) {
            sStatsXfer = mStats;
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (!(preference instanceof ProcessStatsPreference)) {
            return false;
        }

        /*
        PreferenceActivity pa = (PreferenceActivity)getActivity();
        pa.startPreferencePanel(PowerUsageDetail.class.getName(), args,
                R.string.details_title, null, null, 0);
        */

        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        MenuItem refresh = menu.add(0, MENU_STATS_REFRESH, 0, R.string.menu_stats_refresh)
                .setIcon(R.drawable.ic_menu_refresh_holo_dark)
                .setAlphabeticShortcut('r');
        refresh.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM |
                MenuItem.SHOW_AS_ACTION_WITH_TEXT);

        /*
        String helpUrl;
        if (!TextUtils.isEmpty(helpUrl = getResources().getString(R.string.help_url_battery))) {
            final MenuItem help = menu.add(0, MENU_HELP, 0, R.string.help_label);
            HelpUtils.prepareHelpMenuItem(getActivity(), help, helpUrl);
        }
        */
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_STATS_REFRESH:
                mStats = null;
                refreshStats();
                return true;
            default:
                return false;
        }
    }

    private void addNotAvailableMessage() {
        Preference notAvailable = new Preference(getActivity());
        notAvailable.setTitle(R.string.power_usage_not_available);
        mAppListGroup.addPreference(notAvailable);
    }

    private void refreshStats() {
        if (mStats == null) {
            load();
        }

        mAppListGroup.removeAll();
        mAppListGroup.setOrderingAsAdded(false);

        mMemStatusPref.setOrder(-2);
        mAppListGroup.addPreference(mMemStatusPref);
        String durationString = Utils.formatElapsedTime(getActivity(),
                mStats.mTimePeriodEndRealtime-mStats.mTimePeriodStartRealtime);
        CharSequence memString;
        CharSequence[] memStates = getResources().getTextArray(R.array.ram_states);
        if (mMemState >= 0 && mMemState < memStates.length) {
            memString = memStates[mMemState];
        } else {
            memString = "?";
        }
        mMemStatusPref.setTitle(getActivity().getString(R.string.process_stats_total_duration,
                durationString));
        mMemStatusPref.setSummary(getActivity().getString(R.string.process_stats_memory_status,
                        memString));
        /*
        mMemStatusPref.setTitle(DateFormat.format(DateFormat.getBestDateTimePattern(
                getActivity().getResources().getConfiguration().locale,
                "MMMM dd, yyyy h:mm a"), mStats.mTimePeriodStartClock));
        */
        /*
        BatteryHistoryPreference hist = new BatteryHistoryPreference(getActivity(), mStats);
        hist.setOrder(-1);
        mAppListGroup.addPreference(hist);
        */

        ProcessStats.ProcessDataCollection totals = new ProcessStats.ProcessDataCollection(
                ProcessStats.ALL_SCREEN_ADJ, ProcessStats.ALL_MEM_ADJ,
                ProcessStats.BACKGROUND_PROC_STATES);

        long now = SystemClock.uptimeMillis();

        final PackageManager pm = getActivity().getPackageManager();

        mTotalTime = ProcessStats.dumpSingleTime(null, null, mStats.mMemFactorDurations,
                mStats.mMemFactor, mStats.mStartTime, now);

        LinearColorPreference colors = new LinearColorPreference(getActivity());
        colors.setOrder(-1);
        mAppListGroup.addPreference(colors);

        long[] memTimes = new long[ProcessStats.ADJ_MEM_FACTOR_COUNT];
        for (int iscreen=0; iscreen<ProcessStats.ADJ_COUNT; iscreen+=ProcessStats.ADJ_SCREEN_MOD) {
            for (int imem=0; imem<ProcessStats.ADJ_MEM_FACTOR_COUNT; imem++) {
                int state = imem+iscreen;
                memTimes[imem] += mStats.mMemFactorDurations[state];
            }
        }

        colors.setRatios(memTimes[ProcessStats.ADJ_MEM_FACTOR_CRITICAL] / (float)mTotalTime,
                (memTimes[ProcessStats.ADJ_MEM_FACTOR_LOW]
                        + memTimes[ProcessStats.ADJ_MEM_FACTOR_MODERATE]) / (float)mTotalTime,
                memTimes[ProcessStats.ADJ_MEM_FACTOR_NORMAL] / (float)mTotalTime);

        ArrayList<ProcStatsEntry> procs = new ArrayList<ProcStatsEntry>();

        /*
        ArrayList<ProcessStats.ProcessState> rawProcs = mStats.collectProcessesLocked(
                ProcessStats.ALL_SCREEN_ADJ, ProcessStats.ALL_MEM_ADJ,
                ProcessStats.BACKGROUND_PROC_STATES, now, null);
        for (int i=0, N=(rawProcs != null ? rawProcs.size() : 0); i<N; i++) {
            procs.add(new ProcStatsEntry(rawProcs.get(i), totals));
        }
        */

        for (int ip=0, N=mStats.mProcesses.getMap().size(); ip<N; ip++) {
            SparseArray<ProcessStats.ProcessState> uids = mStats.mProcesses.getMap().valueAt(ip);
            for (int iu=0; iu<uids.size(); iu++) {
                procs.add(new ProcStatsEntry(uids.valueAt(iu), totals));
            }
        }

        Collections.sort(procs, sEntryCompare);
        while (procs.size() > MAX_ITEMS_TO_LIST) {
            procs.remove(procs.size()-1);
        }

        long maxWeight = 0;
        for (int i=0, N=(procs != null ? procs.size() : 0); i<N; i++) {
            ProcStatsEntry proc = procs.get(i);
            if (maxWeight < proc.mWeight) {
                maxWeight = proc.mWeight;
            }
        }

        for (int i=0, N=(procs != null ? procs.size() : 0); i<N; i++) {
            ProcStatsEntry proc = procs.get(i);
            final double percentOfWeight = (((double)proc.mWeight) / maxWeight) * 100;
            final double percentOfTime = (((double)proc.mDuration) / mTotalTime) * 100;
            if (percentOfWeight < 1) continue;
            ProcessStatsPreference pref = new ProcessStatsPreference(getActivity(), null);
            ApplicationInfo targetApp = null;
            String label = proc.mName;
            String pkgName = null;
            if (proc.mUnique) {
                pkgName = proc.mPackage;
                proc.addServices(mStats.getPackageStateLocked(proc.mPackage, proc.mUid));
            } else {
                // See if there is one significant package that was running here.
                ArrayList<ProcStatsEntry> subProcs = new ArrayList<ProcStatsEntry>();
                for (int ipkg=0, NPKG=mStats.mPackages.getMap().size(); ipkg<NPKG; ipkg++) {
                    SparseArray<ProcessStats.PackageState> uids
                            = mStats.mPackages.getMap().valueAt(ipkg);
                    for (int iu=0, NU=uids.size(); iu<NU; iu++) {
                        if (uids.keyAt(iu) != proc.mUid) {
                            continue;
                        }
                        ProcessStats.PackageState pkgState = uids.valueAt(iu);
                        boolean match = false;
                        for (int iproc=0, NPROC=pkgState.mProcesses.size(); iproc<NPROC; iproc++) {
                            ProcessStats.ProcessState subProc =
                                    pkgState.mProcesses.valueAt(iproc);
                            if (subProc.mName.equals(proc.mName)) {
                                match = true;
                                subProcs.add(new ProcStatsEntry(subProc, totals));
                            }
                        }
                        if (match) {
                            proc.addServices(mStats.getPackageStateLocked(proc.mPackage,
                                    proc.mUid));
                        }
                    }
                }
                if ( subProcs.size() > 1) {
                    Collections.sort(subProcs, sEntryCompare);
                    if (subProcs.get(0).mWeight > (subProcs.get(1).mWeight*3)) {
                        pkgName = subProcs.get(0).mPackage;
                    }
                }
            }
            if (pkgName != null) {
                // Only one app associated with this process.
                try {
                    targetApp = pm.getApplicationInfo(pkgName,
                            PackageManager.GET_DISABLED_COMPONENTS |
                            PackageManager.GET_DISABLED_UNTIL_USED_COMPONENTS |
                            PackageManager.GET_UNINSTALLED_PACKAGES);
                    String name = targetApp.loadLabel(pm).toString();
                    if (proc.mName.equals(pkgName)) {
                        label = name;
                    } else {
                        if (proc.mName.startsWith(pkgName)) {
                            int off = pkgName.length();
                            if (proc.mName.length() > off) {
                                off++;
                            }
                            label = name + " (" + proc.mName.substring(off) + ")";
                        } else {
                            label = name + " (" + proc.mName + ")";
                        }
                    }
                } catch (PackageManager.NameNotFoundException e) {
                }
            }
            if (targetApp == null) {
                String[] packages = pm.getPackagesForUid(proc.mUid);
                for (String curPkg : packages) {
                    try {
                        final PackageInfo pi = pm.getPackageInfo(curPkg,
                                PackageManager.GET_DISABLED_COMPONENTS |
                                PackageManager.GET_DISABLED_UNTIL_USED_COMPONENTS |
                                PackageManager.GET_UNINSTALLED_PACKAGES);
                        if (pi.sharedUserLabel != 0) {
                            targetApp = pi.applicationInfo;
                            final CharSequence nm = pm.getText(curPkg,
                                    pi.sharedUserLabel, pi.applicationInfo);
                            if (nm != null) {
                                label = nm.toString() + " (" + proc.mName + ")";
                            } else {
                                label = targetApp.loadLabel(pm).toString() + " ("
                                        + proc.mName + ")";
                            }
                            break;
                        }
                    } catch (PackageManager.NameNotFoundException e) {
                    }
                }
            }
            pref.setTitle(label);
            if (targetApp != null) {
                pref.setIcon(targetApp.loadIcon(pm));
            }
            pref.setOrder(i);
            pref.setPercent(percentOfWeight, percentOfTime);
            mAppListGroup.addPreference(pref);
            if (mAppListGroup.getPreferenceCount() > (MAX_ITEMS_TO_LIST+1)) break;
        }
    }

    private void load() {
        try {
            mMemState = mProcessStats.getCurrentMemoryState();
            ArrayList<ParcelFileDescriptor> fds = new ArrayList<ParcelFileDescriptor>();
            byte[] data = mProcessStats.getCurrentStats(fds);
            Parcel parcel = Parcel.obtain();
            parcel.unmarshall(data, 0, data.length);
            parcel.setDataPosition(0);
            mStats = ProcessStats.CREATOR.createFromParcel(parcel);
            int i = fds.size()-1;
            while (i >= 0 && (mStats.mTimePeriodEndRealtime-mStats.mTimePeriodStartRealtime)
                    < (24*60*60*1000)) {
                Log.i(TAG, "Not enough data, loading next file @ " + i);
                ProcessStats stats = new ProcessStats(false);
                InputStream stream = new ParcelFileDescriptor.AutoCloseInputStream(fds.get(i));
                stats.read(stream);
                try {
                    stream.close();
                } catch (IOException e) {
                }
                if (stats.mReadError == null) {
                    mStats.add(stats);
                    StringBuilder sb = new StringBuilder();
                    sb.append("Added stats: ");
                    sb.append(stats.mTimePeriodStartClockStr);
                    sb.append(", over ");
                    TimeUtils.formatDuration(
                            stats.mTimePeriodEndRealtime-stats.mTimePeriodStartRealtime, sb);
                    Log.i(TAG, sb.toString());
                } else {
                    Log.w(TAG, "Read error: " + stats.mReadError);
                }
                i--;
            }
            while (i >= 0) {
                try {
                    fds.get(i).close();
                } catch (IOException e) {
                }
                i--;
            }
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException:", e);
        }
    }
}
