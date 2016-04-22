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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.SearchIndexableResource;
import android.support.v7.preference.Preference;
import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settings.applications.PermissionsSummaryHelper.PermissionsResultCallback;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settingslib.applications.ApplicationsState;
import com.android.settingslib.applications.ApplicationsState.AppEntry;
import com.android.settingslib.applications.ApplicationsState.Session;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AdvancedAppSettings extends SettingsPreferenceFragment implements
        ApplicationsState.Callbacks, Indexable {

    static final String TAG = "AdvancedAppSettings";

    private static final String KEY_APP_PERM = "manage_perms";
    private static final String KEY_APP_DOMAIN_URLS = "domain_urls";
    private static final String KEY_HIGH_POWER_APPS = "high_power_apps";
    private static final String KEY_SYSTEM_ALERT_WINDOW = "system_alert_window";
    private static final String KEY_WRITE_SETTINGS_APPS = "write_settings_apps";

    private Session mSession;
    private Preference mAppPermsPreference;
    private Preference mAppDomainURLsPreference;
    private Preference mHighPowerPreference;
    private Preference mSystemAlertWindowPreference;
    private Preference mWriteSettingsPreference;

    private BroadcastReceiver mPermissionReceiver;

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
        mSystemAlertWindowPreference = findPreference(KEY_SYSTEM_ALERT_WINDOW);
        mWriteSettingsPreference = findPreference(KEY_WRITE_SETTINGS_APPS);
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.APPLICATIONS_ADVANCED;
    }

    @Override
    public void onRunningStateChanged(boolean running) {
        // No-op.
    }

    @Override
    public void onPackageListChanged() {
        // No-op.
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
        public void onAppWithPermissionsCountsResult(int standardGrantedPermissionAppCount,
                int standardUsedPermissionAppCount) {
            if (getActivity() == null) {
                return;
            }
            mPermissionReceiver = null;
            if (standardUsedPermissionAppCount != 0) {
                mAppPermsPreference.setSummary(getContext().getString(
                        R.string.app_permissions_summary,
                        standardGrantedPermissionAppCount,
                        standardUsedPermissionAppCount));
            } else {
                mAppPermsPreference.setSummary(null);
            }
        }
    };

    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(
                        Context context, boolean enabled) {
                    SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.advanced_apps;
                    return Arrays.asList(sir);
                }

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    return Utils.getNonIndexable(R.xml.advanced_apps, context);
                }
            };
}
