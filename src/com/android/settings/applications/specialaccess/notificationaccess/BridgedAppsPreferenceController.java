/*
 * Copyright (C) 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.android.settings.applications.specialaccess.notificationaccess;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.VersionedPackage;
import android.service.notification.NotificationListenerFilter;

import androidx.preference.CheckBoxPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.applications.AppStateBaseBridge;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.notification.NotificationBackend;
import com.android.settings.widget.AppCheckBoxPreference;
import com.android.settingslib.applications.AppUtils;
import com.android.settingslib.applications.ApplicationsState;
import com.android.settingslib.applications.ApplicationsState.AppEntry;
import com.android.settingslib.applications.ApplicationsState.AppFilter;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;

import java.util.ArrayList;
import java.util.Set;
import java.util.TreeSet;


public class BridgedAppsPreferenceController extends BasePreferenceController implements
        LifecycleObserver, ApplicationsState.Callbacks,
        AppStateBaseBridge.Callback {

    private ApplicationsState mApplicationsState;
    private ApplicationsState.Session mSession;
    private AppFilter mFilter;
    private PreferenceScreen mScreen;

    private ComponentName mCn;
    private int mUserId;
    private NotificationBackend mNm;
    private NotificationListenerFilter mNlf;

    public BridgedAppsPreferenceController(Context context, String key) {
        super(context, key);
    }

    public BridgedAppsPreferenceController setAppState(ApplicationsState appState) {
        mApplicationsState = appState;
        return this;
    }

    public BridgedAppsPreferenceController setCn(ComponentName cn) {
        mCn = cn;
        return this;
    }

    public BridgedAppsPreferenceController setUserId(int userId) {
        mUserId = userId;
        return this;
    }

    public BridgedAppsPreferenceController setNm(NotificationBackend nm) {
        mNm = nm;
        return this;
    }

    public BridgedAppsPreferenceController setFilter(AppFilter filter) {
        mFilter = filter;
        return this;
    }

    public BridgedAppsPreferenceController setSession(Lifecycle lifecycle) {
        mSession = mApplicationsState.newSession(this, lifecycle);
        return this;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        mScreen = screen;
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }


    @Override
    public void onExtraInfoUpdated() {
        rebuild();
    }

    @Override
    public void onRunningStateChanged(boolean running) {

    }

    @Override
    public void onPackageListChanged() {
        rebuild();
    }

    @Override
    public void onRebuildComplete(ArrayList<AppEntry> apps) {
        if (apps == null) {
            return;
        }
        mNlf = mNm.getListenerFilter(mCn, mUserId);

        // Create apps key set for removing useless preferences
        final Set<String> appsKeySet = new TreeSet<>();
        // Add or update preferences
        final int N = apps.size();
        for (int i = 0; i < N; i++) {
            final AppEntry entry = apps.get(i);
            final String prefKey = entry.info.packageName + "|" + entry.info.uid;
            appsKeySet.add(prefKey);
            AppCheckBoxPreference preference = mScreen.findPreference(prefKey);
            if (preference == null) {
                preference = new AppCheckBoxPreference(mScreen.getContext());
                preference.setIcon(AppUtils.getIcon(mContext, entry));
                preference.setTitle(entry.label);
                preference.setKey(prefKey);
                mScreen.addPreference(preference);
            }
            preference.setOrder(i);
            preference.setChecked(mNlf.isPackageAllowed(
                    new VersionedPackage(entry.info.packageName, entry.info.uid)));
            preference.setOnPreferenceChangeListener(this::onPreferenceChange);
        }

        // Remove preferences that are no longer existing in the updated list of apps
        removeUselessPrefs(appsKeySet);
    }

    @Override
    public void onPackageIconChanged() {
        rebuild();
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

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference instanceof CheckBoxPreference) {
            String packageName = preference.getKey().substring(0, preference.getKey().indexOf("|"));
            int uid = Integer.parseInt(preference.getKey().substring(
                    preference.getKey().indexOf("|") + 1));
            boolean allowlisted = newValue == Boolean.TRUE;
            mNlf = mNm.getListenerFilter(mCn, mUserId);
            if (allowlisted) {
                mNlf.removePackage(new VersionedPackage(packageName, uid));
            } else {
                mNlf.addPackage(new VersionedPackage(packageName, uid));
            }
            mNm.setListenerFilter(mCn, mUserId, mNlf);
            return true;
        }
        return false;
    }

    public void rebuild() {
        final ArrayList<AppEntry> apps = mSession.rebuild(mFilter,
                ApplicationsState.ALPHA_COMPARATOR);
        if (apps != null) {
            onRebuildComplete(apps);
        }
    }

    private void removeUselessPrefs(final Set<String> appsKeySet) {
        final int prefCount = mScreen.getPreferenceCount();
        String prefKey;
        if (prefCount > 0) {
            for (int i = prefCount - 1; i >= 0; i--) {
                Preference pref = mScreen.getPreference(i);
                prefKey = pref.getKey();
                if (!appsKeySet.contains(prefKey)) {
                    mScreen.removePreference(pref);
                }
            }
        }
    }
}
