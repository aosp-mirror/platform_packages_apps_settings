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

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.android.internal.app.procstats.ProcessStats;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.applications.ProcStatsData.MemInfo;
import com.android.settings.core.SubSettingLauncher;
import com.android.settingslib.core.instrumentation.Instrumentable;
import com.android.settingslib.widget.settingsspinner.SettingsSpinnerAdapter;

public abstract class ProcessStatsBase extends SettingsPreferenceFragment
        implements OnItemSelectedListener {
    private static final String DURATION = "duration";

    protected static final String ARG_TRANSFER_STATS = "transfer_stats";
    protected static final String ARG_DURATION_INDEX = "duration_index";

    protected static final int NUM_DURATIONS = 4;

    // The actual duration value to use for each duration option.  Note these
    // are lower than the actual duration, since our durations are computed in
    // batches of 3 hours so we want to allow the time we use to be slightly
    // smaller than the actual time selected instead of bumping up to 3 hours
    // beyond it.
    private static final long DURATION_QUANTUM = ProcessStats.COMMIT_PERIOD;
    public static long[] sDurations = new long[] {
            3 * 60 * 60 * 1000 - DURATION_QUANTUM / 2, 6 * 60 * 60 * 1000 - DURATION_QUANTUM / 2,
            12 * 60 * 60 * 1000 - DURATION_QUANTUM / 2, 24 * 60 * 60 * 1000 - DURATION_QUANTUM / 2
    };
    protected static int[] sDurationLabels = new int[] {
            R.string.menu_duration_3h, R.string.menu_duration_6h,
            R.string.menu_duration_12h, R.string.menu_duration_1d
    };

    private ViewGroup mSpinnerHeader;
    private Spinner mFilterSpinner;
    private ArrayAdapter<String> mFilterAdapter;

    protected ProcStatsData mStatsManager;
    protected int mDurationIndex;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        Bundle args = getArguments();
        mStatsManager = new ProcStatsData(getActivity(), icicle != null
                || (args != null && args.getBoolean(ARG_TRANSFER_STATS, false)));

        mDurationIndex = icicle != null
                ? icicle.getInt(ARG_DURATION_INDEX)
                : args != null ? args.getInt(ARG_DURATION_INDEX) : 0;
        mStatsManager.setDuration(icicle != null
                ? icicle.getLong(DURATION, sDurations[0]) : sDurations[0]);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(DURATION, mStatsManager.getDuration());
        outState.putInt(ARG_DURATION_INDEX, mDurationIndex);
    }

    @Override
    public void onResume() {
        super.onResume();
        mStatsManager.refreshStats(false);
        refreshUi();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (getActivity().isChangingConfigurations()) {
            mStatsManager.xferStats();
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mSpinnerHeader = (ViewGroup) setPinnedHeaderView(R.layout.apps_filter_spinner);
        mFilterSpinner = (Spinner) mSpinnerHeader.findViewById(R.id.filter_spinner);
        mFilterAdapter = new SettingsSpinnerAdapter<String>(mFilterSpinner.getContext());

        for (int i = 0; i < NUM_DURATIONS; i++) {
            mFilterAdapter.add(getString(sDurationLabels[i]));
        }
        mFilterSpinner.setAdapter(mFilterAdapter);
        mFilterSpinner.setSelection(mDurationIndex);
        mFilterSpinner.setOnItemSelectedListener(this);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        mDurationIndex = position;
        mStatsManager.setDuration(sDurations[position]);
        refreshUi();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        // Select something.
        mFilterSpinner.setSelection(0);
    }

    public abstract void refreshUi();

    public static void launchMemoryDetail(SettingsActivity activity, MemInfo memInfo,
            ProcStatsPackageEntry entry, boolean includeAppInfo) {
        Bundle args = new Bundle();
        args.putParcelable(ProcessStatsDetail.EXTRA_PACKAGE_ENTRY, entry);
        args.putDouble(ProcessStatsDetail.EXTRA_WEIGHT_TO_RAM, memInfo.weightToRam);
        args.putLong(ProcessStatsDetail.EXTRA_TOTAL_TIME, memInfo.memTotalTime);
        args.putDouble(ProcessStatsDetail.EXTRA_MAX_MEMORY_USAGE,
                memInfo.usedWeight * memInfo.weightToRam);
        args.putDouble(ProcessStatsDetail.EXTRA_TOTAL_SCALE, memInfo.totalScale);
        new SubSettingLauncher(activity)
                .setDestination(ProcessStatsDetail.class.getName())
                .setTitleRes(R.string.memory_usage)
                .setArguments(args)
                .setSourceMetricsCategory(Instrumentable.METRICS_CATEGORY_UNKNOWN)
                .launch();
    }
}
