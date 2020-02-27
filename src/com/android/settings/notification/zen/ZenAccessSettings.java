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

package com.android.settings.notification.zen;

import android.annotation.Nullable;
import android.app.NotificationManager;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageItemInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.ArraySet;
import android.view.View;

import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.applications.AppInfoBase;
import com.android.settings.applications.specialaccess.zenaccess.ZenAccessController;
import com.android.settings.applications.specialaccess.zenaccess.ZenAccessDetails;
import com.android.settings.applications.specialaccess.zenaccess.ZenAccessSettingObserverMixin;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.widget.EmptyTextSettings;
import com.android.settingslib.search.SearchIndexable;
import com.android.settingslib.widget.apppreference.AppPreference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@SearchIndexable
public class ZenAccessSettings extends EmptyTextSettings implements
        ZenAccessSettingObserverMixin.Listener {
    private final String TAG = "ZenAccessSettings";

    private Context mContext;
    private PackageManager mPkgMan;
    private NotificationManager mNoMan;

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.NOTIFICATION_ZEN_MODE_ACCESS;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        mContext = getActivity();
        mPkgMan = mContext.getPackageManager();
        mNoMan = mContext.getSystemService(NotificationManager.class);
        getSettingsLifecycle().addObserver(
                new ZenAccessSettingObserverMixin(getContext(), this /* listener */));
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setEmptyText(R.string.zen_access_empty_text);
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.zen_access_settings;
    }

    @Override
    public void onResume() {
        super.onResume();
        reloadList();
    }

    @Override
    public void onZenAccessPolicyChanged() {
        reloadList();
    }

    private void reloadList() {
        final PreferenceScreen screen = getPreferenceScreen();
        screen.removeAll();
        final ArrayList<ApplicationInfo> apps = new ArrayList<>();
        final Set<String> requesting =
                ZenAccessController.getPackagesRequestingNotificationPolicyAccess();
        if (!requesting.isEmpty()) {
            final List<ApplicationInfo> installed = mPkgMan.getInstalledApplications(0);
            if (installed != null) {
                for (ApplicationInfo app : installed) {
                    if (requesting.contains(app.packageName)) {
                        apps.add(app);
                    }
                }
            }
        }
        ArraySet<String> autoApproved = new ArraySet<>();
        autoApproved.addAll(mNoMan.getEnabledNotificationListenerPackages());
        Collections.sort(apps, new PackageItemInfo.DisplayNameComparator(mPkgMan));
        for (ApplicationInfo app : apps) {
            final String pkg = app.packageName;
            final CharSequence label = app.loadLabel(mPkgMan);
            final AppPreference pref = new AppPreference(getPrefContext());
            pref.setKey(pkg);
            pref.setIcon(app.loadIcon(mPkgMan));
            pref.setTitle(label);
            if (autoApproved.contains(pkg)) {
                //Auto approved, user cannot do anything. Hard code summary and disable preference.
                pref.setEnabled(false);
                pref.setSummary(getString(R.string.zen_access_disabled_package_warning));
            } else {
                // Not auto approved, update summary according to notification backend.
                pref.setSummary(getPreferenceSummary(pkg));
            }
            pref.setOnPreferenceClickListener(preference -> {
                AppInfoBase.startAppInfoFragment(
                        ZenAccessDetails.class  /* fragment */,
                        R.string.manage_zen_access_title /* titleRes */,
                        pkg,
                        app.uid,
                        this /* source */,
                        -1 /* requestCode */,
                        getMetricsCategory() /* sourceMetricsCategory */);
                return true;
            });

            screen.addPreference(pref);
        }
    }

    /**
     * @return the summary for the current state of whether the app associated with the given
     * {@param packageName} is allowed to enter picture-in-picture.
     */
    private int getPreferenceSummary(String packageName) {
        final boolean enabled = ZenAccessController.hasAccess(getContext(), packageName);
        return enabled ? R.string.app_permission_summary_allowed
                : R.string.app_permission_summary_not_allowed;
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.zen_access_settings);
}
