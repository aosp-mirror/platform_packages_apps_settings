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
import android.content.ContentResolver;
import android.content.Context;
import android.icu.text.MessageFormat;
import android.os.Bundle;
import android.os.Flags;
import android.provider.Settings;
import android.text.format.Formatter;
import android.text.format.Formatter.BytesResult;

import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceClickListener;
import androidx.preference.PreferenceCategory;
import androidx.preference.SwitchPreference;

import com.android.settings.R;
import com.android.settings.SummaryPreference;
import com.android.settings.Utils;
import com.android.settings.applications.ProcStatsData.MemInfo;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.development.DisableDevSettingsDialogFragment;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ProcessStatsSummary extends ProcessStatsBase implements OnPreferenceClickListener {

    private static final String KEY_PREF_SCREEN = "app_list";
    private static final String KEY_MEMORY_INFO_PREF_GROUP = "memory_info";
    private static final String KEY_STATUS_HEADER = "status_header";

    private static final String KEY_PERFORMANCE = "performance";
    private static final String KEY_TOTAL_MEMORY = "total_memory";
    private static final String KEY_AVERAGY_USED = "average_used";
    private static final String KEY_FREE = "free";
    private static final String KEY_APP_LIST = "apps_list";
    private static final String KEY_FORCE_ENABLE_PSS_PROFILING = "force_enable_pss_profiling";

    private PreferenceCategory mMemoryInfoPrefCategory;
    private SummaryPreference mSummaryPref;

    private Preference mPerformance;
    private Preference mTotalMemory;
    private Preference mAverageUsed;
    private Preference mFree;
    private Preference mAppListPreference;
    private SwitchPreference mForceEnablePssProfiling;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        addPreferencesFromResource(R.xml.process_stats_summary);
        mMemoryInfoPrefCategory = (PreferenceCategory) findPreference(KEY_MEMORY_INFO_PREF_GROUP);
        mSummaryPref = (SummaryPreference) findPreference(KEY_STATUS_HEADER);
        mPerformance = findPreference(KEY_PERFORMANCE);
        mTotalMemory = findPreference(KEY_TOTAL_MEMORY);
        mAverageUsed = findPreference(KEY_AVERAGY_USED);
        mFree = findPreference(KEY_FREE);
        mAppListPreference = findPreference(KEY_APP_LIST);
        mAppListPreference.setOnPreferenceClickListener(this);

        // This preference is only applicable if the flag for PSS deprecation in AppProfiler is
        // enabled. Otherwise, it can immediately be hidden.
        mForceEnablePssProfiling =
                (SwitchPreference) findPreference(KEY_FORCE_ENABLE_PSS_PROFILING);
        if (Flags.removeAppProfilerPssCollection()) {
            mForceEnablePssProfiling.setOnPreferenceClickListener(this);
            // Make the toggle reflect the current state of the global setting.
            mForceEnablePssProfiling.setChecked(isPssProfilingForceEnabled(getContext()));
        } else {
            mForceEnablePssProfiling.setVisible(false);
        }
    }

    private void refreshPreferences() {
        // The memory fields should be static if the flag is not enabled.
        if (!Flags.removeAppProfilerPssCollection()) {
            return;
        }
        mMemoryInfoPrefCategory.setVisible(mForceEnablePssProfiling.isChecked());
    }

    @Override
    public void refreshUi() {
        Context context = getContext();
        refreshPreferences();

        // If PSS collection is not enabled, none of the following work needs to be done.
        if (Flags.removeAppProfilerPssCollection() && !isPssProfilingForceEnabled(context)) {
            return;
        }

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
        MessageFormat msgFormat = new MessageFormat(
                getResources().getString(R.string.memory_usage_apps_summary),
                        Locale.getDefault());
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("count", numApps);
        arguments.put("time", durationString);
        mAppListPreference.setSummary(msgFormat.format(arguments));
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
        } else if (preference == mForceEnablePssProfiling) {
            DisableDevSettingsDialogFragment.show(this);
        }
        return false;
    }

    private boolean isPssProfilingForceEnabled(Context context) {
        ContentResolver cr = context.getContentResolver();
        return Settings.Global.getInt(cr, Settings.Global.FORCE_ENABLE_PSS_PROFILING, 0) == 1;
    }

    /**
     * Called when the reboot confirmation button is clicked.
     */
    public void onRebootDialogConfirmed() {
        Context context = getContext();
        ContentResolver cr = context.getContentResolver();
        Settings.Global.putInt(cr, Settings.Global.FORCE_ENABLE_PSS_PROFILING,
                mForceEnablePssProfiling.isChecked() ? 1 : 0);
        refreshPreferences();
    }

    /**
     * Called when the reboot deny button is clicked.
     */
    public void onRebootDialogCanceled() {
        // Set the toggle to reflect the state of the setting, which should not have changed.
        mForceEnablePssProfiling.setChecked(isPssProfilingForceEnabled(getContext()));
    }

}
