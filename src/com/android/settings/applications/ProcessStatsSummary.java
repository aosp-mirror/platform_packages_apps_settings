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

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.os.Bundle;
import android.text.format.Formatter;
import android.text.format.Formatter.BytesResult;

import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceClickListener;

import com.android.settings.R;
import com.android.settings.SummaryPreference;
import com.android.settings.Utils;
import com.android.settings.applications.ProcStatsData.MemInfo;
import com.android.settings.core.SubSettingLauncher;

public class ProcessStatsSummary extends ProcessStatsBase implements OnPreferenceClickListener {

    private static final String KEY_STATUS_HEADER = "status_header";

    private static final String KEY_PERFORMANCE = "performance";
    private static final String KEY_TOTAL_MEMORY = "total_memory";
    private static final String KEY_AVERAGY_USED = "average_used";
    private static final String KEY_FREE = "free";
    private static final String KEY_APP_LIST = "apps_list";

    private SummaryPreference mSummaryPref;

    private Preference mPerformance;
    private Preference mTotalMemory;
    private Preference mAverageUsed;
    private Preference mFree;
    private Preference mAppListPreference;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        addPreferencesFromResource(R.xml.process_stats_summary);
        mSummaryPref = (SummaryPreference) findPreference(KEY_STATUS_HEADER);
        mPerformance = findPreference(KEY_PERFORMANCE);
        mTotalMemory = findPreference(KEY_TOTAL_MEMORY);
        mAverageUsed = findPreference(KEY_AVERAGY_USED);
        mFree = findPreference(KEY_FREE);
        mAppListPreference = findPreference(KEY_APP_LIST);
        mAppListPreference.setOnPreferenceClickListener(this);
    }

    @Override
    public void refreshUi() {
        Context context = getContext();

        MemInfo memInfo = mStatsManager.getMemInfo();

        double usedRam = memInfo.realUsedRam;
        double totalRam = memInfo.realTotalRam;
        double freeRam = memInfo.realFreeRam;
        BytesResult usedResult = Formatter.formatBytes(context.getResources(), (long) usedRam,
                Formatter.FLAG_SHORTER);
        String totalString = Formatter.formatShortFileSize(context, (long) totalRam);
        String freeString = Formatter.formatShortFileSize(context, (long) freeRam);
        CharSequence memString;
        CharSequence[] memStatesStr = getResources().getTextArray(R.array.ram_states);
        int memState = mStatsManager.getMemState();
        if (memState >= 0 && memState < memStatesStr.length - 1) {
            memString = memStatesStr[memState];
        } else {
            memString = memStatesStr[memStatesStr.length - 1];
        }
        mSummaryPref.setAmount(usedResult.value);
        mSummaryPref.setUnits(usedResult.units);
        float usedRatio = (float)(usedRam / (freeRam + usedRam));
        mSummaryPref.setRatios(usedRatio, 0, 1 - usedRatio);

        mPerformance.setSummary(memString);
        mTotalMemory.setSummary(totalString);
        mAverageUsed.setSummary(Utils.formatPercentage((long) usedRam, (long) totalRam));
        mFree.setSummary(freeString);
        String durationString = getString(sDurationLabels[mDurationIndex]);
        int numApps = mStatsManager.getEntries().size();
        mAppListPreference.setSummary(getResources().getQuantityString(
                R.plurals.memory_usage_apps_summary, numApps, numApps, durationString));
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.PROCESS_STATS_SUMMARY;
    }

    @Override
    public int getHelpResource() {
        return R.string.help_uri_process_stats_summary;
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (preference == mAppListPreference) {
            final Bundle args = new Bundle();
            args.putBoolean(ARG_TRANSFER_STATS, true);
            args.putInt(ARG_DURATION_INDEX, mDurationIndex);
            mStatsManager.xferStats();
            new SubSettingLauncher(getContext())
                    .setDestination(ProcessStatsUi.class.getName())
                    .setTitleRes(R.string.memory_usage_apps)
                    .setArguments(args)
                    .setSourceMetricsCategory(getMetricsCategory())
                    .launch();
            return true;
        }
        return false;
    }
}
