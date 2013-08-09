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
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import com.android.internal.app.IProcessStats;
import com.android.internal.app.ProcessStats;
import com.android.settings.R;

import java.io.IOException;
import java.util.ArrayList;

public class ProcessStatsUi extends PreferenceFragment {
    private static final String TAG = "ProcessStatsUi";
    private static final boolean DEBUG = false;

    private static final String KEY_APP_LIST = "app_list";
    private static final String KEY_MEM_STATUS = "mem_status";

    private static final int MENU_STATS_REFRESH = Menu.FIRST;
    private static final int MENU_HELP = Menu.FIRST + 2;

    static final int MAX_ITEMS_TO_LIST = 20;

    private static ProcessStats sStatsXfer;

    IProcessStats mProcessStats;
    UserManager mUm;
    ProcessStats mStats;

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
        /*
        BatteryHistoryPreference hist = new BatteryHistoryPreference(getActivity(), mStats);
        hist.setOrder(-1);
        mAppListGroup.addPreference(hist);
        */

        ProcessStats.ProcessDataCollection totals = new ProcessStats.ProcessDataCollection(
                ProcessStats.ALL_SCREEN_ADJ, ProcessStats.ALL_MEM_ADJ,
                ProcessStats.BACKGROUND_PROC_STATES);

        long now = SystemClock.uptimeMillis();

        mTotalTime = ProcessStats.dumpSingleTime(null, null, mStats.mMemFactorDurations,
                mStats.mMemFactor, mStats.mStartTime, now);

        ArrayList<ProcessStats.ProcessState> procs = mStats.collectProcessesLocked(
                ProcessStats.ALL_SCREEN_ADJ, ProcessStats.ALL_MEM_ADJ,
                ProcessStats.BACKGROUND_PROC_STATES, now, null);

        final PackageManager pm = getActivity().getPackageManager();

        for (int i=0, N=(procs != null ? procs.size() : 0); i<N; i++) {
            ProcessStats.ProcessState ps = procs.get(i);
            final double percentOfTotal = (((double)ps.mTmpTotalTime) / mTotalTime) * 100;
            if (percentOfTotal < 1) continue;
            ProcessStatsPreference pref = new ProcessStatsPreference(getActivity(), null);
            ApplicationInfo targetApp = null;
            String label = ps.mName;
            if (ps.mCommonProcess == ps) {
                // Only one app associated with this process.
                try {
                    targetApp = pm.getApplicationInfo(ps.mPackage,
                            PackageManager.GET_DISABLED_COMPONENTS |
                            PackageManager.GET_DISABLED_UNTIL_USED_COMPONENTS |
                            PackageManager.GET_UNINSTALLED_PACKAGES);
                    String name = targetApp.loadLabel(pm).toString();
                    if (ps.mName.equals(ps.mPackage)) {
                        label = name;
                    } else {
                        if (ps.mName.startsWith(ps.mPackage)) {
                            int off = ps.mPackage.length();
                            if (ps.mName.length() > off) {
                                off++;
                            }
                            label = name + " (" + ps.mName.substring(off) + ")";
                        } else {
                            label = name + " (" + ps.mName + ")";
                        }
                    }
                } catch (PackageManager.NameNotFoundException e) {
                }
            }
            if (targetApp == null) {
                String[] packages = pm.getPackagesForUid(ps.mUid);
                for (String pkgName : packages) {
                    try {
                        final PackageInfo pi = pm.getPackageInfo(pkgName,
                                PackageManager.GET_DISABLED_COMPONENTS |
                                PackageManager.GET_DISABLED_UNTIL_USED_COMPONENTS |
                                PackageManager.GET_UNINSTALLED_PACKAGES);
                        if (pi.sharedUserLabel != 0) {
                            targetApp = pi.applicationInfo;
                            final CharSequence nm = pm.getText(pkgName,
                                    pi.sharedUserLabel, pi.applicationInfo);
                            if (nm != null) {
                                label = nm.toString() + " (" + ps.mName + ")";
                            } else {
                                label = targetApp.loadLabel(pm).toString() + " (" + ps.mName + ")";
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
            pref.setOrder(N+100-i);
            ProcessStats.computeProcessData(ps, totals, now);
            pref.setPercent(percentOfTotal, totals.avgPss * 1024);
            mAppListGroup.addPreference(pref);
            if (mAppListGroup.getPreferenceCount() > (MAX_ITEMS_TO_LIST+1)) break;
        }
    }

    private void load() {
        try {
            ArrayList<ParcelFileDescriptor> fds = new ArrayList<ParcelFileDescriptor>();
            byte[] data = mProcessStats.getCurrentStats(fds);
            for (int i=0; i<fds.size(); i++) {
                try {
                    fds.get(i).close();
                } catch (IOException e) {
                }
            }
            Parcel parcel = Parcel.obtain();
            parcel.unmarshall(data, 0, data.length);
            parcel.setDataPosition(0);
            mStats = ProcessStats.CREATOR.createFromParcel(parcel);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException:", e);
        }
    }
}
