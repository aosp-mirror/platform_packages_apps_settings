/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.applications.managedomainurls;

import android.app.Application;
import android.content.Context;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.IconDrawableFactory;

import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.applications.AppInfoBase;
import com.android.settings.applications.AppLaunchSettings;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.applications.ApplicationsState;
import com.android.settingslib.applications.ApplicationsState.AppEntry;

import java.util.ArrayList;
import java.util.Map;

public class DomainAppPreferenceController extends BasePreferenceController implements
        ApplicationsState.Callbacks {

    // constant value that can be used to check return code from sub activity.
    private static final int INSTALLED_APP_DETAILS = 1;

    private int mMetricsCategory;
    private ApplicationsState mApplicationsState;
    private ApplicationsState.Session mSession;
    private ManageDomainUrls mFragment;
    private PreferenceGroup mDomainAppList;
    private Map<String, Preference> mPreferenceCache;

    public DomainAppPreferenceController(Context context, String key) {
        super(context, key);
        mApplicationsState = ApplicationsState.getInstance(
                (Application) mContext.getApplicationContext());
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mDomainAppList = screen.findPreference(getPreferenceKey());
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (preference instanceof DomainAppPreference) {
            ApplicationsState.AppEntry entry = ((DomainAppPreference) preference).getEntry();
            AppInfoBase.startAppInfoFragment(AppLaunchSettings.class, R.string.auto_launch_label,
                    entry.info.packageName, entry.info.uid, mFragment,
                    INSTALLED_APP_DETAILS, mMetricsCategory);
            return true;
        }
        return false;
    }

    public void setFragment(ManageDomainUrls fragment) {
        mFragment = fragment;
        mMetricsCategory = fragment.getMetricsCategory();
        mSession = mApplicationsState.newSession(this, mFragment.getSettingsLifecycle());
    }

    @Override
    public void onRunningStateChanged(boolean running) {
    }

    @Override
    public void onPackageListChanged() {
    }

    @Override
    public void onRebuildComplete(ArrayList<AppEntry> apps) {
        if (mContext == null) {
            return;
        }
        rebuildAppList(mDomainAppList, apps);
    }

    @Override
    public void onPackageIconChanged() {
    }

    @Override
    public void onPackageSizeChanged(String packageName) {
    }

    @Override
    public void onAllSizesComputed() {
    }

    @Override
    public void onLauncherInfoChanged() {
    }

    @Override
    public void onLoadEntriesCompleted() {
        rebuild();
    }

    private void cacheAllPrefs(PreferenceGroup group) {
        mPreferenceCache = new ArrayMap();
        final int count = group.getPreferenceCount();
        for (int i = 0; i < count; i++) {
            Preference p = group.getPreference(i);
            if (TextUtils.isEmpty(p.getKey())) {
                continue;
            }
            mPreferenceCache.put(p.getKey(), p);
        }
    }

    private Preference getCachedPreference(String key) {
        return mPreferenceCache != null ? mPreferenceCache.remove(key) : null;
    }

    private void removeCachedPrefs(PreferenceGroup group) {
        for (Preference p : mPreferenceCache.values()) {
            group.removePreference(p);
        }
        mPreferenceCache = null;
    }

    private void rebuild() {
        final ArrayList<AppEntry> apps = mSession.rebuild(
                ApplicationsState.FILTER_WITH_DOMAIN_URLS, ApplicationsState.ALPHA_COMPARATOR);
        if (apps != null) {
            onRebuildComplete(apps);
        }
    }

    private void rebuildAppList(PreferenceGroup group, ArrayList<AppEntry> apps) {
        cacheAllPrefs(group);
        final int size = apps.size();
        final Context context = group.getContext();
        final IconDrawableFactory iconDrawableFactory = IconDrawableFactory.newInstance(context);
        for (int i = 0; i < size; i++) {
            final AppEntry entry = apps.get(i);
            final String key = entry.info.packageName + "|" + entry.info.uid;
            DomainAppPreference preference = (DomainAppPreference) getCachedPreference(key);
            if (preference == null) {
                preference = new DomainAppPreference(context, iconDrawableFactory, entry);
                preference.setKey(key);
                group.addPreference(preference);
            } else {
                preference.reuse();
            }
            preference.setOrder(i);
        }
        removeCachedPrefs(group);
    }
}
