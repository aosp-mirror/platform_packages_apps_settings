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

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.text.format.Formatter;
import android.util.Log;
import android.util.TimeUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;

import com.android.internal.app.ProcessStats;
import com.android.internal.logging.MetricsLogger;
import com.android.settings.InstrumentedPreferenceFragment;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.Utils;
import com.android.settings.applications.ProcStatsData.MemInfo;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ProcessStatsUi extends InstrumentedPreferenceFragment {
    private static final String MEM_REGION = "mem_region";
    private static final String STATS_TYPE = "stats_type";
    private static final String USE_USS = "use_uss";
    private static final String SHOW_SYSTEM = "show_system";
    private static final String SHOW_PERCENTAGE = "show_percentage";
    private static final String DURATION = "duration";
    static final String TAG = "ProcessStatsUi";
    static final boolean DEBUG = false;

    private static final String KEY_APP_LIST = "app_list";
    private static final String KEY_STATUS_HEADER = "status_header";

    private static final int NUM_DURATIONS = 4;

    private static final int MENU_STATS_REFRESH = Menu.FIRST;
    private static final int MENU_DURATION = Menu.FIRST + 1;
    private static final int MENU_SHOW_PERCENTAGE = MENU_DURATION + NUM_DURATIONS;
    private static final int MENU_SHOW_SYSTEM = MENU_SHOW_PERCENTAGE + 1;
    private static final int MENU_USE_USS = MENU_SHOW_SYSTEM + 1;
    private static final int MENU_TYPE_BACKGROUND = MENU_USE_USS + 1;
    private static final int MENU_TYPE_FOREGROUND = MENU_TYPE_BACKGROUND + 1;
    private static final int MENU_TYPE_CACHED = MENU_TYPE_FOREGROUND + 1;

    static final int MAX_ITEMS_TO_LIST = 60;

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

    private boolean mShowPercentage;
    private boolean mShowSystem;
    private boolean mUseUss;
    private int mMemRegion;

    private MenuItem[] mDurationMenus = new MenuItem[NUM_DURATIONS];
    private MenuItem mShowPercentageMenu;
    private MenuItem mShowSystemMenu;
    private MenuItem mUseUssMenu;
    private MenuItem mTypeBackgroundMenu;
    private MenuItem mTypeForegroundMenu;
    private MenuItem mTypeCachedMenu;

    private PreferenceGroup mAppListGroup;
    private TextView mMemStatus;

    private long[] mMemTimes = new long[ProcessStats.ADJ_MEM_FACTOR_COUNT];
    private LinearColorBar mColors;
    private TextView mMemUsed;
    private LayoutPreference mHeader;
    private PackageManager mPm;
    private long memTotalTime;

    private int mStatsType;

    // The actual duration value to use for each duration option.  Note these
    // are lower than the actual duration, since our durations are computed in
    // batches of 3 hours so we want to allow the time we use to be slightly
    // smaller than the actual time selected instead of bumping up to 3 hours
    // beyond it.
    private static final long DURATION_QUANTUM = ProcessStats.COMMIT_PERIOD;
    private static long[] sDurations = new long[] {
        3*60*60*1000 - DURATION_QUANTUM/2, 6*60*60*1000 - DURATION_QUANTUM/2,
        12*60*60*1000 - DURATION_QUANTUM/2, 24*60*60*1000 - DURATION_QUANTUM/2
    };
    private static int[] sDurationLabels = new int[] {
            R.string.menu_duration_3h, R.string.menu_duration_6h,
            R.string.menu_duration_12h, R.string.menu_duration_1d
    };

    private ProcStatsData mStatsManager;
    private double mMaxMemoryUsage;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        mStatsManager = new ProcStatsData(getActivity(), icicle != null);

        mPm = getActivity().getPackageManager();

        addPreferencesFromResource(R.xml.process_stats_summary);
        mAppListGroup = (PreferenceGroup) findPreference(KEY_APP_LIST);
        mHeader = (LayoutPreference)mAppListGroup.findPreference(KEY_STATUS_HEADER);
        mMemStatus = (TextView) mHeader.findViewById(R.id.memory_state);
        mColors = (LinearColorBar) mHeader.findViewById(R.id.color_bar);
        mMemUsed = (TextView) mHeader.findViewById(R.id.memory_used);
        mStatsManager.setDuration(icicle != null
                ? icicle.getLong(DURATION, sDurations[0]) : sDurations[0]);
        mShowPercentage = icicle != null ? icicle.getBoolean(SHOW_PERCENTAGE) : true;
        mShowSystem = icicle != null ? icicle.getBoolean(SHOW_SYSTEM) : false;
        mUseUss = icicle != null ? icicle.getBoolean(USE_USS) : false;
        mStatsType = icicle != null ? icicle.getInt(STATS_TYPE, MENU_TYPE_BACKGROUND)
                : MENU_TYPE_BACKGROUND;
        mMemRegion = icicle != null ? icicle.getInt(MEM_REGION, LinearColorBar.REGION_GREEN)
                : LinearColorBar.REGION_GREEN;
        setHasOptionsMenu(true);
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.APPLICATIONS_PROCESS_STATS_UI;
    }

    @Override
    public void onResume() {
        super.onResume();
        mStatsManager.refreshStats(false);
        refreshUi();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(DURATION, mStatsManager.getDuration());
        outState.putBoolean(SHOW_PERCENTAGE, mShowPercentage);
        outState.putBoolean(SHOW_SYSTEM, mShowSystem);
        outState.putBoolean(USE_USS, mUseUss);
        outState.putInt(STATS_TYPE, mStatsType);
        outState.putInt(MEM_REGION, mMemRegion);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (getActivity().isChangingConfigurations()) {
            mStatsManager.xferStats();
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (!(preference instanceof ProcessStatsPreference)) {
            return false;
        }

        ProcessStatsPreference pgp = (ProcessStatsPreference) preference;
        Bundle args = new Bundle();
        args.putParcelable(ProcessStatsDetail.EXTRA_PACKAGE_ENTRY, pgp.getEntry());
        args.putBoolean(ProcessStatsDetail.EXTRA_USE_USS, mUseUss);
        args.putDouble(ProcessStatsDetail.EXTRA_WEIGHT_TO_RAM,
                mStatsManager.getMemInfo().weightToRam);
        args.putLong(ProcessStatsDetail.EXTRA_TOTAL_TIME, memTotalTime);
        args.putDouble(ProcessStatsDetail.EXTRA_MAX_MEMORY_USAGE, mMaxMemoryUsage);
        args.putDouble(ProcessStatsDetail.EXTRA_TOTAL_SCALE, mStatsManager.getMemInfo().totalScale);
        ((SettingsActivity) getActivity()).startPreferencePanel(
                ProcessStatsDetail.class.getName(), args, R.string.details_title, null, null, 0);

        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        MenuItem refresh = menu.add(0, MENU_STATS_REFRESH, 0, R.string.menu_stats_refresh)
                .setIcon(R.drawable.ic_menu_refresh_holo_dark)
                .setAlphabeticShortcut('r');
        refresh.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM |
                MenuItem.SHOW_AS_ACTION_WITH_TEXT);
        menu.add(0, MENU_DURATION, 0, R.string.menu_proc_stats_duration);

        // Hide these for now, until their need is determined.
//        mShowPercentageMenu = menu.add(0, MENU_SHOW_PERCENTAGE, 0, R.string.menu_show_percentage)
//                .setAlphabeticShortcut('p')
//                .setCheckable(true);
//        mShowSystemMenu = menu.add(0, MENU_SHOW_SYSTEM, 0, R.string.menu_show_system)
//                .setAlphabeticShortcut('s')
//                .setCheckable(true);
//        mUseUssMenu = menu.add(0, MENU_USE_USS, 0, R.string.menu_use_uss)
//                .setAlphabeticShortcut('u')
//                .setCheckable(true);
//        subMenu = menu.addSubMenu(R.string.menu_proc_stats_type);
//        mTypeBackgroundMenu = subMenu.add(0, MENU_TYPE_BACKGROUND, 0,
//                R.string.menu_proc_stats_type_background)
//                .setAlphabeticShortcut('b')
//                .setCheckable(true);
//        mTypeForegroundMenu = subMenu.add(0, MENU_TYPE_FOREGROUND, 0,
//                R.string.menu_proc_stats_type_foreground)
//                .setAlphabeticShortcut('f')
//                .setCheckable(true);
//        mTypeCachedMenu = subMenu.add(0, MENU_TYPE_CACHED, 0,
//                R.string.menu_proc_stats_type_cached)
//                .setCheckable(true);

        updateMenus();
    }

    void updateMenus() {
        int closestIndex = 0;
        long closestDelta = Math.abs(sDurations[0] - mStatsManager.getDuration());
        for (int i = 1; i < NUM_DURATIONS; i++) {
            long delta = Math.abs(sDurations[i] - mStatsManager.getDuration());
            if (delta < closestDelta) {
                closestDelta = delta;
                closestIndex = i;
            }
        }
        for (int i=0; i<NUM_DURATIONS; i++) {
            if (mDurationMenus[i] != null) {
                mDurationMenus[i].setChecked(i == closestIndex);
            }
        }
        mStatsManager.setDuration(sDurations[closestIndex]);
        if (mShowPercentageMenu != null) {
            mShowPercentageMenu.setChecked(mShowPercentage);
        }
        if (mShowSystemMenu != null) {
            mShowSystemMenu.setChecked(mShowSystem);
            mShowSystemMenu.setEnabled(mStatsType == MENU_TYPE_BACKGROUND);
        }
        if (mUseUssMenu != null) {
            mUseUssMenu.setChecked(mUseUss);
        }
        if (mTypeBackgroundMenu != null) {
            mTypeBackgroundMenu.setChecked(mStatsType == MENU_TYPE_BACKGROUND);
        }
        if (mTypeForegroundMenu != null) {
            mTypeForegroundMenu.setChecked(mStatsType == MENU_TYPE_FOREGROUND);
        }
        if (mTypeCachedMenu != null) {
            mTypeCachedMenu.setChecked(mStatsType == MENU_TYPE_CACHED);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_STATS_REFRESH:
                mStatsManager.refreshStats(false);
                refreshUi();
                return true;
            case MENU_SHOW_PERCENTAGE:
                mShowPercentage = !mShowPercentage;
                refreshUi();
                return true;
            case MENU_SHOW_SYSTEM:
                mShowSystem = !mShowSystem;
                refreshUi();
                return true;
            case MENU_USE_USS:
                mUseUss = !mUseUss;
                refreshUi();
                return true;
            case MENU_TYPE_BACKGROUND:
            case MENU_TYPE_FOREGROUND:
            case MENU_TYPE_CACHED:
                mStatsType = item.getItemId();
                if (mStatsType == MENU_TYPE_FOREGROUND) {
                    mStatsManager.setStats(FOREGROUND_PROC_STATES);
                } else if (mStatsType == MENU_TYPE_CACHED) {
                    mStatsManager.setStats(CACHED_PROC_STATES);
                } else {
                    mStatsManager.setStats(mShowSystem ? BACKGROUND_AND_SYSTEM_PROC_STATES
                            : ProcessStats.BACKGROUND_PROC_STATES);
                }
                refreshUi();
                return true;
            case MENU_DURATION:
                CharSequence[] durations = new CharSequence[sDurationLabels.length];
                for (int i = 0; i < sDurationLabels.length; i++) {
                    durations[i] = getString(sDurationLabels[i]);
                }
                new AlertDialog.Builder(getContext())
                        .setTitle(item.getTitle())
                        .setItems(durations, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mStatsManager.setDuration(sDurations[which]);
                                refreshUi();
                            }
                        }).show();
                return true;
        }
        return false;
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

    private void refreshUi() {
        updateMenus();

        mAppListGroup.removeAll();
        mAppListGroup.setOrderingAsAdded(false);
        mHeader.setOrder(-1);
        mAppListGroup.addPreference(mHeader);

        final long elapsedTime = mStatsManager.getElapsedTime();

        final Context context = getActivity();
        // TODO: More Colors.

        // For computing the ratio to show, we want to count the baseline cached RAM we
        // need (at which point we start killing processes) as used RAM, so that if we
        // reach the point of thrashing due to no RAM for any background processes we
        // report that as RAM being full.  To do this, we need to first convert the weights
        // back to actual RAM...  and since the RAM values we compute here won't exactly
        // match the real physical RAM, scale those to the actual physical RAM.  No problem!
        MemInfo memInfo = mStatsManager.getMemInfo();

        memTotalTime = memInfo.memTotalTime;
        double usedRam = memInfo.realUsedRam;
        double totalRam = memInfo.realTotalRam;
        double freeRam = memInfo.realFreeRam;
        String durationString = Utils.formatElapsedTime(context, elapsedTime, false);
        String usedString = Formatter.formatShortFileSize(context, (long) usedRam);
        String totalString = Formatter.formatShortFileSize(context, (long) totalRam);
        CharSequence memString;
        CharSequence[] memStatesStr = getResources().getTextArray(R.array.ram_states);
        int memState = mStatsManager.getMemState();
        int memColor;
        if (memState >= 0 && memState < memStatesStr.length) {
            memString = memStatesStr[memState];
            memColor = getResources().getIntArray(R.array.ram_colors)[memState];
        } else {
            memString = "?";
            memColor = context.getColor(R.color.running_processes_apps_ram);
        }
        mColors.setColors(memColor, memColor, context.getColor(R.color.running_processes_free_ram));
        if (mShowPercentage) {
            mMemUsed.setText(context.getString(
                    R.string.process_stats_total_duration_percentage,
                    Utils.formatPercentage((long) usedRam, (long) totalRam),
                    durationString));
        } else {
            mMemUsed.setText(context.getString(R.string.process_stats_total_duration,
                    usedString, totalString, durationString));
        }
        mMemStatus.setText(memString);
        float usedRatio = (float)(usedRam / (freeRam + usedRam));
        mColors.setRatios(usedRatio, 0, 1-usedRatio);

        List<ProcStatsPackageEntry> pkgEntries = mStatsManager.getEntries();

        // Update everything and get the absolute maximum of memory usage for scaling.
        mMaxMemoryUsage = 0;
        for (int i=0, N=pkgEntries.size(); i<N; i++) {
            ProcStatsPackageEntry pkg = pkgEntries.get(i);
            pkg.updateMetrics();
            double maxMem = Math.max(pkg.mMaxBgMem, pkg.mMaxRunMem) * 1024 * memInfo.totalScale;
            if (maxMem > mMaxMemoryUsage) {
                mMaxMemoryUsage = maxMem;
            }
        }

        Collections.sort(pkgEntries, sPackageEntryCompare);

        // Now collect the per-process information into applications, so that applications
        // running as multiple processes will have only one entry representing all of them.

        if (DEBUG) Log.d(TAG, "-------------------- BUILDING UI");

        // Find where we should stop.  Because we have two properties we are looking at,
        // we need to go from the back looking for the first place either holds.
        int end = pkgEntries.size()-1;
        while (end >= 0) {
            ProcStatsPackageEntry pkg = pkgEntries.get(end);
            final double percentOfWeight = (pkg.mRunWeight
                    / (memInfo.totalRam / memInfo.weightToRam)) * 100;
            final double percentOfTime = (((double) pkg.mRunDuration) / memTotalTime) * 100;
            if (percentOfWeight >= .01 || percentOfTime >= 25) {
                break;
            }
            end--;
        }
        for (int i=0; i <= end; i++) {
            ProcStatsPackageEntry pkg = pkgEntries.get(i);
            ProcessStatsPreference pref = new ProcessStatsPreference(context);
            pkg.retrieveUiData(context, mPm);
            pref.init(pkg, mPm, mMaxMemoryUsage, memInfo.weightToRam, memInfo.totalScale);
            pref.setOrder(i);
            mAppListGroup.addPreference(pref);
            if (mAppListGroup.getPreferenceCount() > (MAX_ITEMS_TO_LIST+1)) {
                if (DEBUG) Log.d(TAG, "Done with UI, hit item limit!");
                break;
            }
        }
    }
}
