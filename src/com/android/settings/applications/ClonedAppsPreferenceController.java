/*
 * Copyright (C) 2022 The Android Open Source Project
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

import static android.content.pm.PackageManager.GET_ACTIVITIES;

import static com.android.settings.Utils.PROPERTY_CLONED_APPS_ENABLED;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.DeviceConfig;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.dashboard.profileselector.ProfileSelectFragment;

import java.util.Arrays;
import java.util.List;

/**
 * A preference controller handling the logic for updating the summary of cloned apps.
 */
public class ClonedAppsPreferenceController extends BasePreferenceController
        implements LifecycleObserver {
    private Preference mPreference;
    private Context mContext;

    public ClonedAppsPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mContext = context;
    }

    @Override
    public int getAvailabilityStatus() {
        return DeviceConfig.getBoolean(DeviceConfig.NAMESPACE_APP_CLONING,
                PROPERTY_CLONED_APPS_ENABLED, false)
                && mContext.getResources().getBoolean(R.bool.config_cloned_apps_page_enabled)
                ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
    }
    /**
     * On lifecycle resume event.
     */
    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    public void onResume() {
        updatePreferenceSummary();
    }

    private void updatePreferenceSummary() {
        new AsyncTask<Void, Void, Integer[]>() {

            @Override
            protected Integer[] doInBackground(Void... unused) {
                // Get list of allowlisted cloneable apps.
                List<String> cloneableApps = Arrays.asList(
                        mContext.getResources().getStringArray(
                                com.android.internal.R.array.cloneable_apps));
                List<String> primaryUserApps = mContext.getPackageManager()
                        .getInstalledPackagesAsUser(GET_ACTIVITIES,
                                UserHandle.myUserId()).stream().map(x -> x.packageName).toList();
                // Count number of installed apps in system user.
                int availableAppsCount = (int) cloneableApps.stream()
                        .filter(x -> primaryUserApps.contains(x)).count();

                int cloneUserId = Utils.getCloneUserId(mContext);
                if (cloneUserId == -1) {
                    return new Integer[]{0, availableAppsCount};
                }
                // Get all apps in clone profile if present.
                List<String> cloneProfileApps = mContext.getPackageManager()
                        .getInstalledPackagesAsUser(GET_ACTIVITIES,
                                cloneUserId).stream().map(x -> x.packageName).toList();
                // Count number of allowlisted app present in clone profile.
                int clonedAppsCount = (int) cloneableApps.stream()
                        .filter(x -> cloneProfileApps.contains(x)).count();

                return new Integer[]{clonedAppsCount, availableAppsCount - clonedAppsCount};
            }

            @Override
            protected void onPostExecute(Integer[] countInfo) {
                updateSummary(countInfo[0], countInfo[1]);
            }
        }.execute();
    }

    private void updateSummary(int clonedAppsCount, int availableAppsCount) {
        mPreference.setSummary(mContext.getResources().getString(
                R.string.cloned_apps_summary, clonedAppsCount, availableAppsCount));
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        // Add this extra so that work tab is not displayed on Cloned Apps page.
        if (getPreferenceKey().equals(preference.getKey())) {
            final Bundle extras = preference.getExtras();
            extras.putInt(ProfileSelectFragment.EXTRA_PROFILE,
                    ProfileSelectFragment.ProfileType.PERSONAL);
        }

        return super.handlePreferenceTreeClick(preference);
    }
}
