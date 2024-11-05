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
import android.content.pm.PackageManager;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;

import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;
import com.android.settingslib.widget.AppPreference;

public class ProcessStatsPreference extends AppPreference {
    static final String TAG = "ProcessStatsPreference";

    private ProcStatsPackageEntry mEntry;
    private int mProgress;
    private boolean mProgressVisible;

    public ProcessStatsPreference(Context context) {
        super(context, null);
        setLayoutResource(R.layout.preference_process_stats);
    }

    public void init(ProcStatsPackageEntry entry, PackageManager pm, double maxMemory,
            double weightToRam, double totalScale, boolean avg) {
        mEntry = entry;
        String title = TextUtils.isEmpty(entry.mUiLabel) ? entry.mPackage : entry.mUiLabel;
        setTitle(title);
        if (TextUtils.isEmpty(title)) {
            Log.d(TAG, "PackageEntry contained no package name or uiLabel");
        }
        if (entry.mUiTargetApp != null) {
            setIcon(entry.mUiTargetApp.loadIcon(pm));
        } else {
            setIcon(pm.getDefaultActivityIcon());
        }
        boolean statsForeground = entry.mRunWeight > entry.mBgWeight;
        double amount = avg ? (statsForeground ? entry.mRunWeight : entry.mBgWeight) * weightToRam
                : (statsForeground ? entry.mMaxRunMem : entry.mMaxBgMem) * totalScale * 1024;
        setSummary(Formatter.formatShortFileSize(getContext(), (long) amount));
        setProgress((int) (100 * amount / maxMemory));
    }

    public ProcStatsPackageEntry getEntry() {
        return mEntry;
    }

    /**
     * Sets the current progress.
     * @param amount the current progress
     *
     * @see ProgressBar#setProgress(int)
     */
    public void setProgress(int amount) {
        mProgress = amount;
        mProgressVisible = true;
        notifyChanged();
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder view) {
        super.onBindViewHolder(view);

        final ProgressBar progress = (ProgressBar) view.findViewById(android.R.id.progress);
        if (mProgressVisible) {
            progress.setProgress(mProgress);
            progress.setVisibility(View.VISIBLE);
        } else {
            progress.setVisibility(View.GONE);
        }
    }
}
