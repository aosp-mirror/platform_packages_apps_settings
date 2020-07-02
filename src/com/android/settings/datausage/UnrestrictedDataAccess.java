/*
 * Copyright (C) 2016 The Android Open Source Project
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

import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.os.UserHandle;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.applications.AppStateBaseBridge;
import com.android.settings.applications.InstalledAppDetails;
import com.android.settings.datausage.AppStateDataUsageBridge.DataUsageState;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.widget.FilterTouchesSwitchPreference;
import com.android.settingslib.applications.ApplicationsState;
import com.android.settingslib.applications.ApplicationsState.AppEntry;
import com.android.settingslib.applications.ApplicationsState.AppFilter;

import java.util.ArrayList;

public class UnrestrictedDataAccess extends SettingsPreferenceFragment
        implements ApplicationsState.Callbacks, AppStateBaseBridge.Callback,
        Preference.OnPreferenceChangeListener {

    private static final int MENU_SHOW_SYSTEM = Menu.FIRST + 42;
    private static final String EXTRA_SHOW_SYSTEM = "show_system";

    private ApplicationsState mApplicationsState;
    private AppStateDataUsageBridge mDataUsageBridge;
    private ApplicationsState.Session mSession;
    private DataSaverBackend mDataSaverBackend;
    private boolean mShowSystem;
    private boolean mExtraLoaded;
    private AppFilter mFilter;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setAnimationAllowed(true);
        setPreferenceScreen(getPreferenceManager().createPreferenceScreen(getContext()));
        mApplicationsState = ApplicationsState.getInstance(
                (Application) getContext().getApplicationContext());
        mDataSaverBackend = new DataSaverBackend(getContext());
        mDataUsageBridge = new AppStateDataUsageBridge(mApplicationsState, this, mDataSaverBackend);
        mSession = mApplicationsState.newSession(this);
        mShowSystem = icicle != null && icicle.getBoolean(EXTRA_SHOW_SYSTEM);
        mFilter = mShowSystem ? ApplicationsState.FILTER_ALL_ENABLED
                : ApplicationsState.FILTER_DOWNLOADED_AND_LAUNCHER;
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.add(Menu.NONE, MENU_SHOW_SYSTEM, Menu.NONE,
                mShowSystem ? R.string.menu_hide_system : R.string.menu_show_system);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_SHOW_SYSTEM:
                mShowSystem = !mShowSystem;
                item.setTitle(mShowSystem ? R.string.menu_hide_system : R.string.menu_show_system);
                mFilter = mShowSystem ? ApplicationsState.FILTER_ALL_ENABLED
                        : ApplicationsState.FILTER_DOWNLOADED_AND_LAUNCHER;
                if (mExtraLoaded) {
                    rebuild();
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(EXTRA_SHOW_SYSTEM, mShowSystem);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setLoading(true, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        mSession.resume();
        mDataUsageBridge.resume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mDataUsageBridge.pause();
        mSession.pause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mSession.release();
        mDataUsageBridge.release();
    }

    @Override
    public void onExtraInfoUpdated() {
        mExtraLoaded = true;
        rebuild();
    }

    @Override
    protected int getHelpResource() {
        return R.string.help_url_unrestricted_data_access;
    }

    private void rebuild() {
        ArrayList<AppEntry> apps = mSession.rebuild(mFilter, ApplicationsState.ALPHA_COMPARATOR);
        if (apps != null) {
            onRebuildComplete(apps);
        }
    }

    @Override
    public void onRunningStateChanged(boolean running) {

    }

    @Override
    public void onPackageListChanged() {

    }

    @Override
    public void onRebuildComplete(ArrayList<AppEntry> apps) {
        if (getContext() == null) return;
        cacheRemoveAllPrefs(getPreferenceScreen());
        final int N = apps.size();
        for (int i = 0; i < N; i++) {
            AppEntry entry = apps.get(i);
            if (!shouldAddPreference(entry)) {
                continue;
            }
            String key = entry.info.packageName + "|" + entry.info.uid;
            AccessPreference preference = (AccessPreference) getCachedPreference(key);
            if (preference == null) {
                preference = new AccessPreference(getPrefContext(), entry);
                preference.setKey(key);
                preference.setOnPreferenceChangeListener(this);
                getPreferenceScreen().addPreference(preference);
            } else {
                preference.reuse();
            }
            preference.setOrder(i);
        }
        setLoading(false, true);
        removeCachedPrefs(getPreferenceScreen());
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
    public int getMetricsCategory() {
        return MetricsEvent.DATA_USAGE_UNRESTRICTED_ACCESS;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference instanceof AccessPreference) {
            AccessPreference accessPreference = (AccessPreference) preference;
            boolean whitelisted = newValue == Boolean.TRUE;
            logSpecialPermissionChange(whitelisted, accessPreference.mEntry.info.packageName);
            mDataSaverBackend.setIsWhitelisted(accessPreference.mEntry.info.uid,
                    accessPreference.mEntry.info.packageName, whitelisted);
            accessPreference.mState.isDataSaverWhitelisted = whitelisted;
            return true;
        }
        return false;
    }

    @VisibleForTesting
    void logSpecialPermissionChange(boolean whitelisted, String packageName) {
        int logCategory = whitelisted ? MetricsEvent.APP_SPECIAL_PERMISSION_UNL_DATA_ALLOW
                : MetricsEvent.APP_SPECIAL_PERMISSION_UNL_DATA_DENY;
        FeatureFactory.getFactory(getContext()).getMetricsFeatureProvider().action(getContext(),
                logCategory, packageName);
    }

    @VisibleForTesting
    boolean shouldAddPreference(AppEntry app) {
        return app != null && UserHandle.isApp(app.info.uid);
    }

    private class AccessPreference extends FilterTouchesSwitchPreference implements
            DataSaverBackend.Listener {
        private final AppEntry mEntry;
        private final DataUsageState mState;

        public AccessPreference(final Context context, AppEntry entry) {
            super(context);
            mEntry = entry;
            mState = (DataUsageState) mEntry.extraInfo;
            mEntry.ensureLabel(getContext());
            setState();
            if (mEntry.icon != null) {
                setIcon(mEntry.icon);
            }
        }

        @Override
        public void onAttached() {
            super.onAttached();
            mDataSaverBackend.addListener(this);
        }

        @Override
        public void onDetached() {
            mDataSaverBackend.remListener(this);
            super.onDetached();
        }

        @Override
        protected void onClick() {
            if (mState.isDataSaverBlacklisted) {
                // app is blacklisted, launch App Data Usage screen
                InstalledAppDetails.startAppInfoFragment(AppDataUsage.class,
                        getContext().getString(R.string.app_data_usage),
                        UnrestrictedDataAccess.this,
                        mEntry);
            } else {
                // app is not blacklisted, let superclass handle toggle switch
                super.onClick();
            }
        }

        // Sets UI state based on whitelist/blacklist status.
        private void setState() {
            setTitle(mEntry.label);
            if (mState != null) {
                setChecked(mState.isDataSaverWhitelisted);
                if (mState.isDataSaverBlacklisted) {
                    setSummary(R.string.restrict_background_blacklisted);
                } else {
                    setSummary("");
                }
            }
        }

        public void reuse() {
            setState();
            notifyChanged();
        }

        @Override
        public void onBindViewHolder(PreferenceViewHolder holder) {
            if (mEntry.icon == null) {
                holder.itemView.post(new Runnable() {
                    @Override
                    public void run() {
                        // Ensure we have an icon before binding.
                        mApplicationsState.ensureIcon(mEntry);
                        // This might trigger us to bind again, but it gives an easy way to only
                        // load the icon once its needed, so its probably worth it.
                        setIcon(mEntry.icon);
                    }
                });
            }
            holder.findViewById(android.R.id.widget_frame)
                    .setVisibility(mState != null && mState.isDataSaverBlacklisted
                            ? View.INVISIBLE : View.VISIBLE);
            super.onBindViewHolder(holder);
        }

        @Override
        public void onDataSaverChanged(boolean isDataSaving) {
        }

        @Override
        public void onWhitelistStatusChanged(int uid, boolean isWhitelisted) {
            if (mState != null && mEntry.info.uid == uid) {
                mState.isDataSaverWhitelisted = isWhitelisted;
                reuse();
            }
        }

        @Override
        public void onBlacklistStatusChanged(int uid, boolean isBlacklisted) {
            if (mState != null && mEntry.info.uid == uid) {
                mState.isDataSaverBlacklisted = isBlacklisted;
                reuse();
            }
        }
    }

}
