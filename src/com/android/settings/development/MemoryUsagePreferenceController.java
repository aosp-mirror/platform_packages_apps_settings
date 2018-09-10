/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.development;

import android.content.Context;
import android.text.format.Formatter;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.applications.ProcStatsData;
import com.android.settings.applications.ProcessStatsBase;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;
import com.android.settingslib.utils.ThreadUtils;

public class MemoryUsagePreferenceController extends DeveloperOptionsPreferenceController implements
        PreferenceControllerMixin {

    private static final String MEMORY_USAGE_KEY = "memory";

    private ProcStatsData mProcStatsData;

    public MemoryUsagePreferenceController(Context context) {
        super(context);
    }

    @Override
    public String getPreferenceKey() {
        return MEMORY_USAGE_KEY;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);

        mProcStatsData = getProcStatsData();
        setDuration();
    }

    @Override
    public void updateState(Preference preference) {
        // This is posted on the background thread to speed up fragment launch time for dev options
        // mProcStasData.refreshStats(true) takes ~20ms to run.
        ThreadUtils.postOnBackgroundThread(() -> {
            mProcStatsData.refreshStats(true);
            final ProcStatsData.MemInfo memInfo = mProcStatsData.getMemInfo();
            final String usedResult = Formatter.formatShortFileSize(mContext,
                    (long) memInfo.realUsedRam);
            final String totalResult = Formatter.formatShortFileSize(mContext,
                    (long) memInfo.realTotalRam);
            ThreadUtils.postOnMainThread(
                    () -> mPreference.setSummary(mContext.getString(R.string.memory_summary,
                            usedResult, totalResult)));
        });
    }

    @VisibleForTesting
    void setDuration() {
        mProcStatsData.setDuration(ProcessStatsBase.sDurations[0] /* 3 hours */);
    }

    @VisibleForTesting
    ProcStatsData getProcStatsData() {
        return new ProcStatsData(mContext, false);
    }
}
