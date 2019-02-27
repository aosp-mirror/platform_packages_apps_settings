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

import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

import java.util.List;

public class AllAppsInfoPreferenceController extends BasePreferenceController {

    private List<UsageStats> mRecentApps;

    public AllAppsInfoPreferenceController(Context context, String key) {
        super(context, key);
    }

    public void setRecentApps(List<UsageStats> recentApps) {
        mRecentApps = recentApps;
    }

    @Override
    public int getAvailabilityStatus() {
        return mRecentApps == null || mRecentApps.isEmpty() ? AVAILABLE : CONDITIONALLY_UNAVAILABLE;
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        // Show total number of installed apps as See all's summary.
        new InstalledAppCounter(mContext, InstalledAppCounter.IGNORE_INSTALL_REASON,
                mContext.getPackageManager()) {
            @Override
            protected void onCountComplete(int num) {
                preference.setSummary(mContext.getString(R.string.apps_summary, num));
            }
        }.execute();
    }
}
