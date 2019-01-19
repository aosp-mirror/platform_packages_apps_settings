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

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.util.TimeUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;

import com.android.internal.app.procstats.ProcessStats;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.applications.ProcStatsData.MemInfo;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ProcessStatsUi extends ProcessStatsBase {
    static final String TAG = "ProcessStatsUi";
    static final boolean DEBUG = false;

    private static final String KEY_APP_LIST = "app_list";

    private static final int MENU_SHOW_AVG = Menu.FIRST;
    private static final int MENU_SHOW_MAX = Menu.FIRST + 1;

    private PreferenceGroup mAppListGroup;
    private PackageManager mPm;

    private boolean mShowMax;
    private MenuItem mMenuAvg;
    private MenuItem mMenuMax;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        mPm = getActivity().getPackageManager();

        addPreferencesFromResource(R.xml.process_stats_ui);
        mAppListGroup = (PreferenceGroup) findPreference(KEY_APP_LIST);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        mMenuAvg = menu.add(0, MENU_SHOW_AVG, 0, R.string.sort_avg_use);
        mMenuMax = menu.add(0, MENU_SHOW_MAX, 0, R.string.sort_max_use);
        updateMenu();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_SHOW_AVG:
            case MENU_SHOW_MAX:
                mShowMax = !mShowMax;
                refreshUi();
                updateMenu();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateMenu() {
        mMenuMax.setVisible(!mShowMax);
        mMenuAvg.setVisible(mShowMax);
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.APPLICATIONS_PROCESS_STATS_UI;
    }

    @Override
    public int getHelpResource() {
        return R.string.help_uri_process_stats_apps;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (!(preference instanceof ProcessStatsPreference)) {
            return false;
        }
        ProcessStatsPreference pgp = (ProcessStatsPreference) preference;
        MemInfo memInfo = mStatsManager.getMemInfo();
        launchMemoryDetail((SettingsActivity) getActivity(), memInfo, pgp.getEntry(), true);

        return super.onPreferenceTreeClick(preference);
    }

    /**
     * All states in which we consider a process to be actively running (rather than
     * something that can be freely killed to reclaim RAM).  Note this also includes
     * the HOME state, because we prioritize home over all cached processes even when
     * it is in the background, so it is effectively always running from the perspective
     * of the information we want to show the user here.
     */
    public static final int[] BACKGROUND_AND_SYSTEM_PROC_STATES = new int[] {
            ProcessStats.STATE_PERSISTENT, ProcessStats.STATE_IMPORTANT_FOREGROUND,
            ProcessStats.STATE_IMPORTANT_BACKGROUND, ProcessStats.STATE_BACKUP,
            ProcessStats.STATE_HEAVY_WEIGHT, ProcessStats.STATE_SERVICE,
            ProcessStats.STATE_SERVICE_RESTARTING, ProcessStats.STATE_RECEIVER,
            ProcessStats.STATE_HOME
    };

    public static final int[] FOREGROUND_PROC_STATES = new int[] {
            ProcessStats.STATE_TOP
    };

    public static final int[] CACHED_PROC_STATES = new int[] {
            ProcessStats.STATE_CACHED_ACTIVITY, ProcessStats.STATE_CACHED_ACTIVITY_CLIENT,
            ProcessStats.STATE_CACHED_EMPTY
    };

    public static String makeDuration(long time) {
        StringBuilder sb = new StringBuilder(32);
        TimeUtils.formatDuration(time, sb);
        return sb.toString();
    }

    @Override
    public void refreshUi() {
        mAppListGroup.removeAll();
        mAppListGroup.setOrderingAsAdded(false);
        mAppListGroup.setTitle(mShowMax ? R.string.maximum_memory_use
                : R.string.average_memory_use);

        final Context context = getActivity();
        MemInfo memInfo = mStatsManager.getMemInfo();

        List<ProcStatsPackageEntry> pkgEntries = mStatsManager.getEntries();

        // Update everything and get the absolute maximum of memory usage for scaling.
        for (int i=0, N=pkgEntries.size(); i<N; i++) {
            ProcStatsPackageEntry pkg = pkgEntries.get(i);
            pkg.updateMetrics();
        }

        Collections.sort(pkgEntries, mShowMax ? sMaxPackageEntryCompare : sPackageEntryCompare);

        // Now collect the per-process information into applications, so that applications
        // running as multiple processes will have only one entry representing all of them.

        if (DEBUG) Log.d(TAG, "-------------------- BUILDING UI");

        double maxMemory = mShowMax ? memInfo.realTotalRam
                : memInfo.usedWeight * memInfo.weightToRam;
        for (int i = 0; i < pkgEntries.size(); i++) {
            ProcStatsPackageEntry pkg = pkgEntries.get(i);
            ProcessStatsPreference pref = new ProcessStatsPreference(getPrefContext());
            pkg.retrieveUiData(context, mPm);
            pref.init(pkg, mPm, maxMemory, memInfo.weightToRam,
                    memInfo.totalScale, !mShowMax);
            pref.setOrder(i);
            mAppListGroup.addPreference(pref);
        }
    }

    final static Comparator<ProcStatsPackageEntry> sPackageEntryCompare
            = new Comparator<ProcStatsPackageEntry>() {
        @Override
        public int compare(ProcStatsPackageEntry lhs, ProcStatsPackageEntry rhs) {
            double rhsWeight = Math.max(rhs.mRunWeight, rhs.mBgWeight);
            double lhsWeight = Math.max(lhs.mRunWeight, lhs.mBgWeight);
            if (lhsWeight == rhsWeight) {
                return 0;
            }
            return lhsWeight < rhsWeight ? 1 : -1;
        }
    };

    final static Comparator<ProcStatsPackageEntry> sMaxPackageEntryCompare
            = new Comparator<ProcStatsPackageEntry>() {
        @Override
        public int compare(ProcStatsPackageEntry lhs, ProcStatsPackageEntry rhs) {
            double rhsMax = Math.max(rhs.mMaxBgMem, rhs.mMaxRunMem);
            double lhsMax = Math.max(lhs.mMaxBgMem, lhs.mMaxRunMem);
            if (lhsMax == rhsMax) {
                return 0;
            }
            return lhsMax < rhsMax ? 1 : -1;
        }
    };
}
