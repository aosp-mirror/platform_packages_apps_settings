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

package com.android.settings.applications.appinfo;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.os.AsyncTask;
import android.text.format.Formatter;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.applications.ProcStatsData;
import com.android.settings.applications.ProcStatsEntry;
import com.android.settings.applications.ProcStatsPackageEntry;
import com.android.settings.applications.ProcessStatsBase;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnResume;
import com.android.settingslib.development.DevelopmentSettingsEnabler;

public class AppMemoryPreferenceController extends BasePreferenceController
        implements LifecycleObserver, OnResume {

    private static final String KEY_MEMORY = "memory";

    private Preference mPreference;
    private final AppInfoDashboardFragment mParent;
    private ProcStatsData mStatsManager;
    private ProcStatsPackageEntry mStats;

    private class MemoryUpdater extends AsyncTask<Void, Void, ProcStatsPackageEntry> {

        @Override
        protected ProcStatsPackageEntry doInBackground(Void... params) {
            final Activity activity = mParent.getActivity();
            if (activity == null) {
                return null;
            }
            PackageInfo packageInfo = mParent.getPackageInfo();
            if (packageInfo == null) {
                return null;
            }
            if (mStatsManager == null) {
                mStatsManager = new ProcStatsData(activity, false);
                mStatsManager.setDuration(ProcessStatsBase.sDurations[0]);
            }
            mStatsManager.refreshStats(true);
            for (ProcStatsPackageEntry pkgEntry : mStatsManager.getEntries()) {
                for (ProcStatsEntry entry : pkgEntry.getEntries()) {
                    if (entry.getUid() == packageInfo.applicationInfo.uid) {
                        pkgEntry.updateMetrics();
                        return pkgEntry;
                    }
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(ProcStatsPackageEntry entry) {
            if (mParent.getActivity() == null) {
                return;
            }
            if (entry != null) {
                mStats = entry;
                mPreference.setEnabled(true);
                double amount = Math.max(entry.getRunWeight(), entry.getBgWeight())
                        * mStatsManager.getMemInfo().getWeightToRam();
                mPreference.setSummary(mContext.getString(R.string.memory_use_summary,
                        Formatter.formatShortFileSize(mContext, (long) amount)));
            } else {
                mPreference.setEnabled(false);
                mPreference.setSummary(mContext.getString(R.string.no_memory_use_summary));
            }
        }
    }

    public AppMemoryPreferenceController(Context context, AppInfoDashboardFragment parent,
            Lifecycle lifecycle) {
        super(context, KEY_MEMORY);
        mParent = parent;
        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }
    }

    @Override
    public int getAvailabilityStatus() {
        if (!mContext.getResources().getBoolean(R.bool.config_show_app_info_settings_memory)) {
            return UNSUPPORTED_ON_DEVICE;
        }

        return DevelopmentSettingsEnabler.isDevelopmentSettingsEnabled(mContext)
                ? AVAILABLE : CONDITIONALLY_UNAVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (KEY_MEMORY.equals(preference.getKey())) {
            ProcessStatsBase.launchMemoryDetail((SettingsActivity) mParent.getActivity(),
                    mStatsManager.getMemInfo(), mStats, false);
            return true;
        }
        return false;
    }

    @Override
    public void onResume() {
        if (isAvailable()) {
            new MemoryUpdater().execute();
        }
    }

}
