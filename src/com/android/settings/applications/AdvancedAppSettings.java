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

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.preference.Preference;

import com.android.internal.logging.MetricsLogger;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.applications.PermissionsSummaryHelper.PermissionsResultCallback;
import com.android.settings.fuelgauge.PowerWhitelistBackend;
import com.android.settingslib.applications.ApplicationsState;
import com.android.settingslib.applications.ApplicationsState.AppEntry;
import com.android.settingslib.applications.ApplicationsState.Session;

import java.util.ArrayList;

public class AdvancedAppSettings extends SettingsPreferenceFragment implements
        ApplicationsState.Callbacks {

    static final String TAG = "AdvancedAppSettings";

    private static final String KEY_APP_PERM = "manage_perms";
    private static final String KEY_APP_DOMAIN_URLS = "domain_urls";
    private static final String KEY_HIGH_POWER_APPS = "high_power_apps";

    private Session mSession;
    private Preference mAppPermsPreference;
    private Preference mAppDomainURLsPreference;
    private Preference mHighPowerPreference;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.advanced_apps);

        Preference permissions = getPreferenceScreen().findPreference(KEY_APP_PERM);
        permissions.setIntent(new Intent(Intent.ACTION_MANAGE_PERMISSIONS));

        ApplicationsState applicationsState = ApplicationsState.getInstance(
                getActivity().getApplication());
        mSession = applicationsState.newSession(this);

        mAppPermsPreference = findPreference(KEY_APP_PERM);
        mAppDomainURLsPreference = findPreference(KEY_APP_DOMAIN_URLS);
        mHighPowerPreference = findPreference(KEY_HIGH_POWER_APPS);
        updateUI();
    }

    private void updateUI() {
        ArrayList<AppEntry> allApps = mSession.getAllApps();

        int countAppWithDomainURLs = 0;
        for (AppEntry entry : allApps) {
            boolean hasDomainURLs =
                    (entry.info.privateFlags & ApplicationInfo.PRIVATE_FLAG_HAS_DOMAIN_URLS) != 0;
            if (hasDomainURLs) countAppWithDomainURLs++;
        }
        String summary = getResources().getQuantityString(
                R.plurals.domain_urls_apps_summary, countAppWithDomainURLs, countAppWithDomainURLs);
        mAppDomainURLsPreference.setSummary(summary);

        int highPowerCount = PowerWhitelistBackend.getInstance().getWhitelistSize();
        mHighPowerPreference.setSummary(getResources().getQuantityString(R.plurals.high_power_count,
                highPowerCount, highPowerCount));
        PermissionsSummaryHelper.getAppWithPermissionsCounts(getContext(), mPermissionCallback);
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.APPLICATIONS_ADVANCED;
    }

    @Override
    public void onRunningStateChanged(boolean running) {
        // No-op.
    }

    @Override
    public void onPackageListChanged() {
        updateUI();
    }

    @Override
    public void onRebuildComplete(ArrayList<AppEntry> apps) {
        // No-op.
    }

    @Override
    public void onPackageIconChanged() {
        // No-op.
    }

    @Override
    public void onPackageSizeChanged(String packageName) {
        // No-op.
    }

    @Override
    public void onAllSizesComputed() {
        // No-op.
    }

    @Override
    public void onLauncherInfoChanged() {
        // No-op.
    }

    @Override
    public void onLoadEntriesCompleted() {
        // No-op.
    }

    private final PermissionsResultCallback mPermissionCallback = new PermissionsResultCallback() {
        @Override
        public void onPermissionSummaryResult(int[] counts, CharSequence[] groupLabels) {
            if (getActivity() == null) {
                return;
            }
            if (counts != null) {
                mAppPermsPreference.setSummary(getContext().getString(
                        R.string.app_permissions_summary, counts[0], counts[1]));
            } else {
                mAppPermsPreference.setSummary(null);
            }
        }
    };
}
