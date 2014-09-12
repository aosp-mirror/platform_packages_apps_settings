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

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.UserManager;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.text.format.Formatter;
import android.util.Log;
import android.util.SparseArray;
import android.util.TimeUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import com.android.internal.app.IProcessStats;
import com.android.internal.app.ProcessMap;
import com.android.internal.app.ProcessStats;
import com.android.internal.util.MemInfoReader;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.Utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class ProcessStatsUi extends PreferenceFragment
        implements LinearColorBar.OnRegionTappedListener {
    static final String TAG = "ProcessStatsUi";
    static final boolean DEBUG = false;

    private static final String KEY_APP_LIST = "app_list";
    private static final String KEY_MEM_STATUS = "mem_status";

    private static final int NUM_DURATIONS = 4;

    private static final int MENU_STATS_REFRESH = Menu.FIRST;
    private static final int MENU_DURATION = Menu.FIRST + 1;
    private static final int MENU_SHOW_SYSTEM = MENU_DURATION + NUM_DURATIONS;
    private static final int MENU_USE_USS = MENU_SHOW_SYSTEM + 1;
    private static final int MENU_TYPE_BACKGROUND = MENU_USE_USS + 1;
    private static final int MENU_TYPE_FOREGROUND = MENU_TYPE_BACKGROUND + 1;
    private static final int MENU_TYPE_CACHED = MENU_TYPE_FOREGROUND + 1;
    private static final int MENU_HELP = MENU_TYPE_CACHED + 1;

    static final int MAX_ITEMS_TO_LIST = 60;

    final static Comparator<ProcStatsEntry> sEntryCompare = new Comparator<ProcStatsEntry>() {
        @Override
        public int compare(ProcStatsEntry lhs, ProcStatsEntry rhs) {
            if (lhs.mWeight < rhs.mWeight) {
                return 1;
            } else if (lhs.mWeight > rhs.mWeight) {
                return -1;
            } else if (lhs.mDuration < rhs.mDuration) {
                return 1;
            } else if (lhs.mDuration > rhs.mDuration) {
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

    private long mDuration;
    private long mLastDuration;
    private boolean mShowSystem;
    private boolean mUseUss;
    private int mStatsType;
    private int mMemRegion;

    private MenuItem[] mDurationMenus = new MenuItem[NUM_DURATIONS];
    private MenuItem mShowSystemMenu;
    private MenuItem mUseUssMenu;
    private MenuItem mTypeBackgroundMenu;
    private MenuItem mTypeForegroundMenu;
    private MenuItem mTypeCachedMenu;

    private PreferenceGroup mAppListGroup;
    private Preference mMemStatusPref;

    long mMaxWeight;
    long mTotalTime;

    long[] mMemTimes = new long[ProcessStats.ADJ_MEM_FACTOR_COUNT];
    double[] mMemStateWeights = new double[ProcessStats.STATE_COUNT];
    double mMemCachedWeight;
    double mMemFreeWeight;
    double mMemZRamWeight;
    double mMemKernelWeight;
    double mMemNativeWeight;
    double mMemTotalWeight;

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
        mDuration = icicle != null ? icicle.getLong("duration", sDurations[0]) : sDurations[0];
        mShowSystem = icicle != null ? icicle.getBoolean("show_system") : false;
        mUseUss = icicle != null ? icicle.getBoolean("use_uss") : false;
        mStatsType = icicle != null ? icicle.getInt("stats_type", MENU_TYPE_BACKGROUND)
                : MENU_TYPE_BACKGROUND;
        mMemRegion = icicle != null ? icicle.getInt("mem_region", LinearColorBar.REGION_GREEN)
                : LinearColorBar.REGION_GREEN;
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
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong("duration", mDuration);
        outState.putBoolean("show_system", mShowSystem);
        outState.putBoolean("use_uss", mUseUss);
        outState.putInt("stats_type", mStatsType);
        outState.putInt("mem_region", mMemRegion);
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
        if (preference instanceof LinearColorPreference) {
            Bundle args = new Bundle();
            args.putLongArray(ProcessStatsMemDetail.EXTRA_MEM_TIMES, mMemTimes);
            args.putDoubleArray(ProcessStatsMemDetail.EXTRA_MEM_STATE_WEIGHTS, mMemStateWeights);
            args.putDouble(ProcessStatsMemDetail.EXTRA_MEM_CACHED_WEIGHT, mMemCachedWeight);
            args.putDouble(ProcessStatsMemDetail.EXTRA_MEM_FREE_WEIGHT, mMemFreeWeight);
            args.putDouble(ProcessStatsMemDetail.EXTRA_MEM_ZRAM_WEIGHT, mMemZRamWeight);
            args.putDouble(ProcessStatsMemDetail.EXTRA_MEM_KERNEL_WEIGHT, mMemKernelWeight);
            args.putDouble(ProcessStatsMemDetail.EXTRA_MEM_NATIVE_WEIGHT, mMemNativeWeight);
            args.putDouble(ProcessStatsMemDetail.EXTRA_MEM_TOTAL_WEIGHT, mMemTotalWeight);
            args.putBoolean(ProcessStatsMemDetail.EXTRA_USE_USS, mUseUss);
            args.putLong(ProcessStatsMemDetail.EXTRA_TOTAL_TIME, mTotalTime);
            ((SettingsActivity) getActivity()).startPreferencePanel(
                    ProcessStatsMemDetail.class.getName(), args, R.string.mem_details_title,
                    null, null, 0);
            return true;
        }

        if (!(preference instanceof ProcessStatsPreference)) {
            return false;
        }

        ProcessStatsPreference pgp = (ProcessStatsPreference) preference;
        Bundle args = new Bundle();
        args.putParcelable(ProcessStatsDetail.EXTRA_ENTRY, pgp.getEntry());
        args.putBoolean(ProcessStatsDetail.EXTRA_USE_USS, mUseUss);
        args.putLong(ProcessStatsDetail.EXTRA_MAX_WEIGHT, mMaxWeight);
        args.putLong(ProcessStatsDetail.EXTRA_TOTAL_TIME, mTotalTime);
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
        SubMenu subMenu = menu.addSubMenu(R.string.menu_proc_stats_duration);
        for (int i=0; i<NUM_DURATIONS; i++) {
            mDurationMenus[i] = subMenu.add(0, MENU_DURATION+i, 0, sDurationLabels[i])
                            .setCheckable(true);
        }
        mShowSystemMenu = menu.add(0, MENU_SHOW_SYSTEM, 0, R.string.menu_show_system)
                .setAlphabeticShortcut('s')
                .setCheckable(true);
        mUseUssMenu = menu.add(0, MENU_USE_USS, 0, R.string.menu_use_uss)
                .setAlphabeticShortcut('u')
                .setCheckable(true);
        subMenu = menu.addSubMenu(R.string.menu_proc_stats_type);
        mTypeBackgroundMenu = subMenu.add(0, MENU_TYPE_BACKGROUND, 0,
                R.string.menu_proc_stats_type_background)
                .setAlphabeticShortcut('b')
                .setCheckable(true);
        mTypeForegroundMenu = subMenu.add(0, MENU_TYPE_FOREGROUND, 0,
                R.string.menu_proc_stats_type_foreground)
                .setAlphabeticShortcut('f')
                .setCheckable(true);
        mTypeCachedMenu = subMenu.add(0, MENU_TYPE_CACHED, 0,
                R.string.menu_proc_stats_type_cached)
                .setCheckable(true);

        updateMenus();

        /*
        String helpUrl;
        if (!TextUtils.isEmpty(helpUrl = getResources().getString(R.string.help_url_battery))) {
            final MenuItem help = menu.add(0, MENU_HELP, 0, R.string.help_label);
            HelpUtils.prepareHelpMenuItem(getActivity(), help, helpUrl);
        }
        */
    }

    void updateMenus() {
        int closestIndex = 0;
        long closestDelta = Math.abs(sDurations[0]-mDuration);
        for (int i=1; i<NUM_DURATIONS; i++) {
            long delta = Math.abs(sDurations[i]-mDuration);
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
        mDuration = sDurations[closestIndex];
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
        final int id = item.getItemId();
        switch (id) {
            case MENU_STATS_REFRESH:
                mStats = null;
                refreshStats();
                return true;
            case MENU_SHOW_SYSTEM:
                mShowSystem = !mShowSystem;
                refreshStats();
                return true;
            case MENU_USE_USS:
                mUseUss = !mUseUss;
                refreshStats();
                return true;
            case MENU_TYPE_BACKGROUND:
            case MENU_TYPE_FOREGROUND:
            case MENU_TYPE_CACHED:
                mStatsType = item.getItemId();
                refreshStats();
                return true;
            default:
                if (id >= MENU_DURATION && id < (MENU_DURATION+NUM_DURATIONS)) {
                    mDuration = sDurations[id-MENU_DURATION];
                    refreshStats();
                }
                return false;
        }
    }

    @Override
    public void onRegionTapped(int region) {
        if (mMemRegion != region) {
            mMemRegion = region;
            refreshStats();
        }
    }

    private void addNotAvailableMessage() {
        Preference notAvailable = new Preference(getActivity());
        notAvailable.setTitle(R.string.power_usage_not_available);
        mAppListGroup.addPreference(notAvailable);
    }

    public static final int[] BACKGROUND_AND_SYSTEM_PROC_STATES = new int[] {
            ProcessStats.STATE_PERSISTENT, ProcessStats.STATE_IMPORTANT_FOREGROUND,
            ProcessStats.STATE_IMPORTANT_BACKGROUND, ProcessStats.STATE_BACKUP,
            ProcessStats.STATE_HEAVY_WEIGHT, ProcessStats.STATE_SERVICE,
            ProcessStats.STATE_SERVICE_RESTARTING, ProcessStats.STATE_RECEIVER
    };

    public static final int[] FOREGROUND_PROC_STATES = new int[] {
            ProcessStats.STATE_TOP
    };

    public static final int[] CACHED_PROC_STATES = new int[] {
            ProcessStats.STATE_CACHED_ACTIVITY, ProcessStats.STATE_CACHED_ACTIVITY_CLIENT,
            ProcessStats.STATE_CACHED_EMPTY
    };

    public static final int[] RED_MEM_STATES = new int[] {
            ProcessStats.ADJ_MEM_FACTOR_CRITICAL
    };

    public static final int[] YELLOW_MEM_STATES = new int[] {
            ProcessStats.ADJ_MEM_FACTOR_CRITICAL, ProcessStats.ADJ_MEM_FACTOR_LOW,
            ProcessStats.ADJ_MEM_FACTOR_MODERATE
    };

    private String makeDuration(long time) {
        StringBuilder sb = new StringBuilder(32);
        TimeUtils.formatDuration(time, sb);
        return sb.toString();
    }

    private void refreshStats() {
        updateMenus();

        if (mStats == null || mLastDuration != mDuration) {
            load();
        }

        int[] stats;
        int statsLabel;
        if (mStatsType == MENU_TYPE_FOREGROUND) {
            stats = FOREGROUND_PROC_STATES;
            statsLabel = R.string.process_stats_type_foreground;
        } else if (mStatsType == MENU_TYPE_CACHED) {
            stats = CACHED_PROC_STATES;
            statsLabel = R.string.process_stats_type_cached;
        } else {
            stats = mShowSystem ? BACKGROUND_AND_SYSTEM_PROC_STATES
                    : ProcessStats.BACKGROUND_PROC_STATES;
            statsLabel = R.string.process_stats_type_background;
        }

        mAppListGroup.removeAll();
        mAppListGroup.setOrderingAsAdded(false);

        final long elapsedTime = mStats.mTimePeriodEndRealtime-mStats.mTimePeriodStartRealtime;

        mMemStatusPref.setOrder(-2);
        mAppListGroup.addPreference(mMemStatusPref);
        String durationString = Utils.formatElapsedTime(getActivity(), elapsedTime, false);
        CharSequence memString;
        CharSequence[] memStatesStr = getResources().getTextArray(R.array.ram_states);
        if (mMemState >= 0 && mMemState < memStatesStr.length) {
            memString = memStatesStr[mMemState];
        } else {
            memString = "?";
        }
        mMemStatusPref.setTitle(getActivity().getString(R.string.process_stats_total_duration,
                getActivity().getString(statsLabel), durationString));
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

        long now = SystemClock.uptimeMillis();

        final PackageManager pm = getActivity().getPackageManager();

        mTotalTime = ProcessStats.dumpSingleTime(null, null, mStats.mMemFactorDurations,
                mStats.mMemFactor, mStats.mStartTime, now);
        if (DEBUG) Log.d(TAG, "Total time of stats: " + makeDuration(mTotalTime));

        for (int i=0; i<mMemTimes.length; i++) {
            mMemTimes[i] = 0;
        }
        for (int iscreen=0; iscreen<ProcessStats.ADJ_COUNT; iscreen+=ProcessStats.ADJ_SCREEN_MOD) {
            for (int imem=0; imem<ProcessStats.ADJ_MEM_FACTOR_COUNT; imem++) {
                int state = imem+iscreen;
                mMemTimes[imem] += mStats.mMemFactorDurations[state];
            }
        }

        long memTotalTime;
        int[] memStates;

        LinearColorPreference colors = new LinearColorPreference(getActivity());
        colors.setOrder(-1);
        switch (mMemRegion) {
            case LinearColorBar.REGION_RED:
                memTotalTime = mMemTimes[ProcessStats.ADJ_MEM_FACTOR_CRITICAL];
                memStates = RED_MEM_STATES;
                break;
            case LinearColorBar.REGION_YELLOW:
                memTotalTime = mMemTimes[ProcessStats.ADJ_MEM_FACTOR_CRITICAL]
                        + mMemTimes[ProcessStats.ADJ_MEM_FACTOR_LOW]
                        + mMemTimes[ProcessStats.ADJ_MEM_FACTOR_MODERATE];
                memStates = YELLOW_MEM_STATES;
                break;
            default:
                memTotalTime = mTotalTime;
                memStates = ProcessStats.ALL_MEM_ADJ;
                break;
        }
        colors.setColoredRegions(LinearColorBar.REGION_RED);

        // Compute memory badness for chart color.
        int[] badColors = com.android.settings.Utils.BADNESS_COLORS;
        long timeGood = mMemTimes[ProcessStats.ADJ_MEM_FACTOR_NORMAL];
        timeGood += (mMemTimes[ProcessStats.ADJ_MEM_FACTOR_MODERATE]*2)/3;
        timeGood += mMemTimes[ProcessStats.ADJ_MEM_FACTOR_LOW]/3;
        float memBadness = ((float)timeGood)/mTotalTime;
        int badnessColor = badColors[1 + Math.round(memBadness*(badColors.length-2))];
        colors.setColors(badnessColor, badnessColor, badnessColor);

        // We are now going to scale the mMemTimes to match the total elapsed time.
        // These are in uptime, so they will often be smaller than the elapsed time,
        // but if the user taps on the bar we want to show the times to them.  It is confusing
        // to see them be smaller than what we told them the measured duration is, so just
        // scaling them up with make things look reasonable with them none the wiser.
        for (int i=0; i<ProcessStats.ADJ_MEM_FACTOR_COUNT; i++) {
            mMemTimes[i] = (long)((mMemTimes[i]*(double)elapsedTime)/mTotalTime);
        }

        ProcessStats.TotalMemoryUseCollection totalMem = new ProcessStats.TotalMemoryUseCollection(
                ProcessStats.ALL_SCREEN_ADJ, memStates);
        mStats.computeTotalMemoryUse(totalMem, now);
        double freeWeight = totalMem.sysMemFreeWeight + totalMem.sysMemCachedWeight;
        double usedWeight = totalMem.sysMemKernelWeight + totalMem.sysMemNativeWeight
                + totalMem.sysMemZRamWeight;
        double backgroundWeight = 0, persBackgroundWeight = 0;
        mMemCachedWeight = totalMem.sysMemCachedWeight;
        mMemFreeWeight = totalMem.sysMemFreeWeight;
        mMemZRamWeight = totalMem.sysMemZRamWeight;
        mMemKernelWeight = totalMem.sysMemKernelWeight;
        mMemNativeWeight = totalMem.sysMemNativeWeight;
        for (int i=0; i<ProcessStats.STATE_COUNT; i++) {
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
                if (i >= ProcessStats.STATE_IMPORTANT_FOREGROUND) {
                    backgroundWeight += totalMem.processStateWeight[i];
                    persBackgroundWeight += totalMem.processStateWeight[i];
                }
                if (i == ProcessStats.STATE_PERSISTENT) {
                    persBackgroundWeight += totalMem.processStateWeight[i];
                }
            }
        }
        if (DEBUG) {
            Log.i(TAG, "Used RAM: " + Formatter.formatShortFileSize(getActivity(),
                    (long)((usedWeight * 1024) / memTotalTime)));
            Log.i(TAG, "Free RAM: " + Formatter.formatShortFileSize(getActivity(),
                    (long)((freeWeight * 1024) / memTotalTime)));
            Log.i(TAG, "Total RAM: " + Formatter.formatShortFileSize(getActivity(),
                    (long)(((freeWeight+usedWeight) * 1024) / memTotalTime)));
            Log.i(TAG, "Background+Cached RAM: " + Formatter.formatShortFileSize(getActivity(),
                    (long)((backgroundWeight * 1024) / memTotalTime)));
        }
        mMemTotalWeight = freeWeight + usedWeight;

        // For computing the ratio to show, we want to count the baseline cached RAM we
        // need (at which point we start killing processes) as used RAM, so that if we
        // reach the point of thrashing due to no RAM for any background processes we
        // report that as RAM being full.  To do this, we need to first convert the weights
        // back to actual RAM...  and since the RAM values we compute here won't exactly
        // match the real physical RAM, scale those to the actual physical RAM.  No problem!
        double usedRam = (usedWeight*1024)/memTotalTime;
        double freeRam = (freeWeight*1024)/memTotalTime;
        double totalRam = usedRam + freeRam;
        MemInfoReader memReader = new MemInfoReader();
        memReader.readMemInfo();
        double realTotalRam = memReader.getTotalSize();
        double totalScale = realTotalRam / totalRam;
        double realUsedRam = usedRam * totalScale;
        double realFreeRam = freeRam * totalScale;
        if (DEBUG) {
            Log.i(TAG, "Scaled Used RAM: " + Formatter.formatShortFileSize(getActivity(),
                    (long)realUsedRam));
            Log.i(TAG, "Scaled Free RAM: " + Formatter.formatShortFileSize(getActivity(),
                    (long)realFreeRam));
        }
        ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
        ((ActivityManager)getActivity().getSystemService(Context.ACTIVITY_SERVICE)).getMemoryInfo(
                memInfo);
        if (memInfo.hiddenAppThreshold >= realFreeRam) {
            realUsedRam = realFreeRam;
            realFreeRam = 0;
        } else {
            realUsedRam += memInfo.hiddenAppThreshold;
            realFreeRam -= memInfo.hiddenAppThreshold;
        }
        if (DEBUG) {
            Log.i(TAG, "Adj Scaled Used RAM: " + Formatter.formatShortFileSize(getActivity(),
                    (long)realUsedRam));
            Log.i(TAG, "Adj Scaled Free RAM: " + Formatter.formatShortFileSize(getActivity(),
                    (long)realFreeRam));
        }

        float usedRatio = (float)(realUsedRam/(realFreeRam+realUsedRam));
        colors.setRatios(usedRatio, 0, 1-usedRatio);

        if (false) {
            colors.setOnRegionTappedListener(this);
            switch (mMemRegion) {
                case LinearColorBar.REGION_RED:
                    colors.setColoredRegions(LinearColorBar.REGION_RED);
                    memTotalTime = mMemTimes[ProcessStats.ADJ_MEM_FACTOR_CRITICAL];
                    memStates = RED_MEM_STATES;
                    break;
                case LinearColorBar.REGION_YELLOW:
                    colors.setColoredRegions(LinearColorBar.REGION_RED
                            | LinearColorBar.REGION_YELLOW);
                    memTotalTime = mMemTimes[ProcessStats.ADJ_MEM_FACTOR_CRITICAL]
                            + mMemTimes[ProcessStats.ADJ_MEM_FACTOR_LOW]
                            + mMemTimes[ProcessStats.ADJ_MEM_FACTOR_MODERATE];
                    memStates = YELLOW_MEM_STATES;
                    break;
                default:
                    colors.setColoredRegions(LinearColorBar.REGION_ALL);
                    memTotalTime = mTotalTime;
                    memStates = ProcessStats.ALL_MEM_ADJ;
                    break;
            }
            colors.setRatios(mMemTimes[ProcessStats.ADJ_MEM_FACTOR_CRITICAL] / (float)mTotalTime,
                    (mMemTimes[ProcessStats.ADJ_MEM_FACTOR_LOW]
                            + mMemTimes[ProcessStats.ADJ_MEM_FACTOR_MODERATE]) / (float)mTotalTime,
                    mMemTimes[ProcessStats.ADJ_MEM_FACTOR_NORMAL] / (float)mTotalTime);
        }

        mAppListGroup.addPreference(colors);

        ProcessStats.ProcessDataCollection totals = new ProcessStats.ProcessDataCollection(
                ProcessStats.ALL_SCREEN_ADJ, memStates, stats);

        ArrayList<ProcStatsEntry> entries = new ArrayList<ProcStatsEntry>();

        /*
        ArrayList<ProcessStats.ProcessState> rawProcs = mStats.collectProcessesLocked(
                ProcessStats.ALL_SCREEN_ADJ, ProcessStats.ALL_MEM_ADJ,
                ProcessStats.BACKGROUND_PROC_STATES, now, null);
        for (int i=0, N=(rawProcs != null ? rawProcs.size() : 0); i<N; i++) {
            procs.add(new ProcStatsEntry(rawProcs.get(i), totals));
        }
        */

        if (DEBUG) Log.d(TAG, "-------------------- PULLING PROCESSES");

        final ProcessMap<ProcStatsEntry> entriesMap = new ProcessMap<ProcStatsEntry>();
        for (int ipkg=0, N=mStats.mPackages.getMap().size(); ipkg<N; ipkg++) {
            final SparseArray<SparseArray<ProcessStats.PackageState>> pkgUids
                    = mStats.mPackages.getMap().valueAt(ipkg);
            for (int iu=0; iu<pkgUids.size(); iu++) {
                final SparseArray<ProcessStats.PackageState> vpkgs = pkgUids.valueAt(iu);
                for (int iv=0; iv<vpkgs.size(); iv++) {
                    final ProcessStats.PackageState st = vpkgs.valueAt(iv);
                    for (int iproc=0; iproc<st.mProcesses.size(); iproc++) {
                        final ProcessStats.ProcessState pkgProc = st.mProcesses.valueAt(iproc);
                        final ProcessStats.ProcessState proc = mStats.mProcesses.get(pkgProc.mName,
                                pkgProc.mUid);
                        if (proc == null) {
                            Log.w(TAG, "No process found for pkg " + st.mPackageName
                                    + "/" + st.mUid + " proc name " + pkgProc.mName);
                            continue;
                        }
                        ProcStatsEntry ent = entriesMap.get(proc.mName, proc.mUid);
                        if (ent == null) {
                            ent = new ProcStatsEntry(proc, st.mPackageName, totals, mUseUss,
                                    mStatsType == MENU_TYPE_BACKGROUND);
                            if (ent.mDuration > 0) {
                                if (DEBUG) Log.d(TAG, "Adding proc " + proc.mName + "/"
                                        + proc.mUid + ": time=" + makeDuration(ent.mDuration) + " ("
                                        + ((((double)ent.mDuration) / memTotalTime) * 100) + "%)"
                                        + " pss=" + ent.mAvgPss);
                                entriesMap.put(proc.mName, proc.mUid, ent);
                                entries.add(ent);
                            }
                        }  else {
                            ent.addPackage(st.mPackageName);
                        }
                    }
                }
            }
        }

        if (DEBUG) Log.d(TAG, "-------------------- MAPPING SERVICES");

        // Add in service info.
        if (mStatsType == MENU_TYPE_BACKGROUND) {
            for (int ip=0, N=mStats.mPackages.getMap().size(); ip<N; ip++) {
                SparseArray<SparseArray<ProcessStats.PackageState>> uids
                        = mStats.mPackages.getMap().valueAt(ip);
                for (int iu=0; iu<uids.size(); iu++) {
                    SparseArray<ProcessStats.PackageState> vpkgs = uids.valueAt(iu);
                    for (int iv=0; iv<vpkgs.size(); iv++) {
                        ProcessStats.PackageState ps = vpkgs.valueAt(iv);
                        for (int is=0, NS=ps.mServices.size(); is<NS; is++) {
                            ProcessStats.ServiceState ss = ps.mServices.valueAt(is);
                            if (ss.mProcessName != null) {
                                ProcStatsEntry ent = entriesMap.get(ss.mProcessName, uids.keyAt(iu));
                                if (ent != null) {
                                    if (DEBUG) Log.d(TAG, "Adding service " + ps.mPackageName
                                            + "/" + ss.mName + "/" + uids.keyAt(iu) + " to proc "
                                            + ss.mProcessName);
                                    ent.addService(ss);
                                } else {
                                    Log.w(TAG, "No process " + ss.mProcessName + "/" + uids.keyAt(iu)
                                            + " for service " + ss.mName);
                                }
                            }
                        }
                    }
                }
            }
        }

        /*
        SparseArray<ArrayMap<String, ProcStatsEntry>> processes
                = new SparseArray<ArrayMap<String, ProcStatsEntry>>();
        for (int ip=0, N=mStats.mProcesses.getMap().size(); ip<N; ip++) {
            SparseArray<ProcessStats.ProcessState> uids = mStats.mProcesses.getMap().valueAt(ip);
            for (int iu=0; iu<uids.size(); iu++) {
                ProcessStats.ProcessState st = uids.valueAt(iu);
                ProcStatsEntry ent = new ProcStatsEntry(st, totals, mUseUss,
                        mStatsType == MENU_TYPE_BACKGROUND);
                if (ent.mDuration > 0) {
                    if (DEBUG) Log.d(TAG, "Adding proc " + st.mName + "/" + st.mUid + ": time="
                            + makeDuration(ent.mDuration) + " ("
                            + ((((double)ent.mDuration) / memTotalTime) * 100) + "%)");
                    procs.add(ent);
                    ArrayMap<String, ProcStatsEntry> uidProcs = processes.get(ent.mUid);
                    if (uidProcs == null) {
                        uidProcs = new ArrayMap<String, ProcStatsEntry>();
                        processes.put(ent.mUid, uidProcs);
                    }
                    uidProcs.put(ent.mName, ent);
                }
            }
        }
        */

        Collections.sort(entries, sEntryCompare);

        long maxWeight = 1;
        for (int i=0, N=(entries != null ? entries.size() : 0); i<N; i++) {
            ProcStatsEntry proc = entries.get(i);
            if (maxWeight < proc.mWeight) {
                maxWeight = proc.mWeight;
            }
        }
        if (mStatsType == MENU_TYPE_BACKGROUND) {
            mMaxWeight = (long)(mShowSystem ? persBackgroundWeight : backgroundWeight);
            if (mMaxWeight < maxWeight) {
                mMaxWeight = maxWeight;
            }
            if (DEBUG) {
                Log.i(TAG, "Bar max RAM: " + Formatter.formatShortFileSize(getActivity(),
                        (mMaxWeight * 1024) / memTotalTime));
            }
        } else {
            mMaxWeight = maxWeight;
        }

        if (DEBUG) Log.d(TAG, "-------------------- BUILDING UI");

        // Find where we should stop.  Because we have two properties we are looking at,
        // we need to go from the back looking for the first place either holds.
        int end = entries != null ? entries.size()-1 : -1;
        while (end >= 0) {
            ProcStatsEntry proc = entries.get(end);
            final double percentOfWeight = (((double)proc.mWeight) / mMaxWeight) * 100;
            final double percentOfTime = (((double)proc.mDuration) / memTotalTime) * 100;
            if (percentOfWeight >= 1 || percentOfTime >= 25) {
                break;
            }
            end--;
        }
        for (int i=0; i<=end; i++) {
            ProcStatsEntry proc = entries.get(i);
            final double percentOfWeight = (((double)proc.mWeight) / mMaxWeight) * 100;
            final double percentOfTime = (((double)proc.mDuration) / memTotalTime) * 100;
            ProcessStatsPreference pref = new ProcessStatsPreference(getActivity());
            pref.init(null, proc);
            proc.evaluateTargetPackage(pm, mStats, totals, sEntryCompare, mUseUss,
                    mStatsType == MENU_TYPE_BACKGROUND);
            proc.retrieveUiData(pm);
            pref.setTitle(proc.mUiLabel);
            if (proc.mUiTargetApp != null) {
                pref.setIcon(proc.mUiTargetApp.loadIcon(pm));
            }
            pref.setOrder(i);
            pref.setPercent(percentOfWeight, percentOfTime);
            mAppListGroup.addPreference(pref);
            if (mStatsType == MENU_TYPE_BACKGROUND) {
                if (DEBUG) {
                    Log.i(TAG, "App " + proc.mUiLabel + ": weightedRam="
                            + Formatter.formatShortFileSize(getActivity(),
                                    (proc.mWeight * 1024) / memTotalTime)
                            + ", avgRam=" + Formatter.formatShortFileSize(getActivity(),
                                    (proc.mAvgPss*1024)));
                }

            }
            if (mAppListGroup.getPreferenceCount() > (MAX_ITEMS_TO_LIST+1)) {
                if (DEBUG) Log.d(TAG, "Done with UI, hit item limit!");
                break;
            }
        }
    }

    private void load() {
        try {
            mLastDuration = mDuration;
            mMemState = mProcessStats.getCurrentMemoryState();
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
}
