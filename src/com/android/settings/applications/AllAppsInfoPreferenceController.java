/*
 * Copyright (C) 2019 The Android Open Source Project
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

import android.app.usage.UsageStats;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

import java.util.List;

public class AllAppsInfoPreferenceController extends BasePreferenceController
        implements RecentAppStatsMixin.RecentAppStatsListener {

    @VisibleForTesting
    Preference mPreference;

    public AllAppsInfoPreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
        // In most cases, device has recently opened apps. So, we hide it by default.
        mPreference.setVisible(false);
    }

    @Override
    public void onReloadDataCompleted(@NonNull List<UsageStats> recentApps) {
        // If device has recently opened apps, we don't show all apps preference.
        if (!recentApps.isEmpty()) {
            mPreference.setVisible(false);
            return;
        }

        mPreference.setVisible(true);
        // Show total number of installed apps as See all's summary.
        new InstalledAppCounter(mContext, InstalledAppCounter.IGNORE_INSTALL_REASON,
                mContext.getPackageManager()) {
            @Override
            protected void onCountComplete(int num) {
                mPreference.setSummary(mContext.getString(R.string.apps_summary, num));
            }
        }.execute();
    }
}
