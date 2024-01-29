/*
 * Copyright (C) 2018 The Android Open Source Project
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
package com.android.settings.datausage;

import static com.android.settingslib.RestrictedLockUtilsInternal.checkIfMeteredDataUsageUserControlDisabled;

import android.app.Application;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.os.UserHandle;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.applications.AppStateBaseBridge;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.applications.AppUtils;
import com.android.settingslib.applications.ApplicationsState;
import com.android.settingslib.applications.ApplicationsState.AppEntry;
import com.android.settingslib.applications.ApplicationsState.AppFilter;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnDestroy;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;

import java.util.ArrayList;
import java.util.Set;
import java.util.TreeSet;

public class UnrestrictedDataAccessPreferenceController extends BasePreferenceController implements
        LifecycleObserver, OnStart, OnStop, OnDestroy, ApplicationsState.Callbacks,
        AppStateBaseBridge.Callback, Preference.OnPreferenceChangeListener {

    private final ApplicationsState mApplicationsState;
    private final AppStateDataUsageBridge mDataUsageBridge;
    private final DataSaverBackend mDataSaverBackend;
    private ApplicationsState.Session mSession;
    private AppFilter mFilter;
    private DashboardFragment mParentFragment;
    private PreferenceScreen mScreen;
    private boolean mExtraLoaded;

    public UnrestrictedDataAccessPreferenceController(Context context, String key) {
        super(context, key);
        mApplicationsState = ApplicationsState.getInstance(
                (Application) context.getApplicationContext());
        mDataSaverBackend = new DataSaverBackend(context);
        mDataUsageBridge = new AppStateDataUsageBridge(mApplicationsState, this, mDataSaverBackend);
    }

    public void setFilter(AppFilter filter) {
        mFilter = filter;
    }

    public void setParentFragment(DashboardFragment parentFragment) {
        mParentFragment = parentFragment;
    }

    public void setSession(Lifecycle lifecycle) {
        mSession = mApplicationsState.newSession(this, lifecycle);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mScreen = screen;
    }

    @Override
    public int getAvailabilityStatus() {
        return mContext.getResources().getBoolean(R.bool.config_show_data_saver)
                ? AVAILABLE_UNSEARCHABLE
                : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public void onStart() {
        mDataUsageBridge.resume(true /* forceLoadAllApps */);
    }

    @Override
    public void onStop() {
        mDataUsageBridge.pause();
    }

    @Override
    public void onDestroy() {
        mDataUsageBridge.release();
    }

    @Override
    public void onExtraInfoUpdated() {
        mExtraLoaded = true;
        rebuild();
    }

    @Override
    public void onRunningStateChanged(boolean running) {

    }

    @Override
    public void onPackageListChanged() {

    }

    @Override
    public void onRebuildComplete(ArrayList<AppEntry> apps) {
        if (apps == null) {
            return;
        }

        // Preload top visible icons of app list.
        AppUtils.preloadTopIcons(mContext, apps,
                mContext.getResources().getInteger(R.integer.config_num_visible_app_icons));

        // Create apps key set for removing useless preferences
        final Set<String> appsKeySet = new TreeSet<>();
        // Add or update preferences
        final int N = apps.size();
        for (int i = 0; i < N; i++) {
            final AppEntry entry = apps.get(i);
            if (!shouldAddPreference(entry)) {
                continue;
            }
            final String prefkey = UnrestrictedDataAccessPreference.generateKey(entry);
            appsKeySet.add(prefkey);
            UnrestrictedDataAccessPreference preference =
                    (UnrestrictedDataAccessPreference) mScreen.findPreference(prefkey);
            if (preference == null) {
                preference = new UnrestrictedDataAccessPreference(mScreen.getContext(), entry,
                        mApplicationsState, mDataSaverBackend, mParentFragment);
                preference.setOnPreferenceChangeListener(this);
                mScreen.addPreference(preference);
            } else {
                preference.setDisabledByAdmin(checkIfMeteredDataUsageUserControlDisabled(mContext,
                        entry.info.packageName, UserHandle.getUserId(entry.info.uid)));
                preference.checkEcmRestrictionAndSetDisabled(entry.info.packageName);
                preference.updateState();
            }
            preference.setOrder(i);
        }

        // Remove useless preferences
        removeUselessPrefs(appsKeySet);
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

    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference instanceof UnrestrictedDataAccessPreference) {
            final UnrestrictedDataAccessPreference
                    accessPreference = (UnrestrictedDataAccessPreference) preference;
            boolean allowlisted = newValue == Boolean.TRUE;
            logSpecialPermissionChange(allowlisted, accessPreference.getEntry().info.packageName);
            mDataSaverBackend.setIsAllowlisted(accessPreference.getEntry().info.uid,
                    accessPreference.getEntry().info.packageName, allowlisted);
            if (accessPreference.getDataUsageState() != null) {
                accessPreference.getDataUsageState().isDataSaverAllowlisted = allowlisted;
            }
            return true;
        }
        return false;
    }

    public void rebuild() {
        if (!mExtraLoaded) {
            return;
        }

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
                if (!appsKeySet.isEmpty() && appsKeySet.contains(prefKey)) {
                    continue;
                }
                mScreen.removePreference(pref);
            }
        }
    }

    @VisibleForTesting
    void logSpecialPermissionChange(boolean allowlisted, String packageName) {
        final int logCategory = allowlisted ? SettingsEnums.APP_SPECIAL_PERMISSION_UNL_DATA_ALLOW
                : SettingsEnums.APP_SPECIAL_PERMISSION_UNL_DATA_DENY;
        FeatureFactory.getFeatureFactory().getMetricsFeatureProvider().action(mContext,
                logCategory, packageName);
    }

    @VisibleForTesting
    static boolean shouldAddPreference(AppEntry app) {
        return app != null && UserHandle.isApp(app.info.uid);
    }
}
