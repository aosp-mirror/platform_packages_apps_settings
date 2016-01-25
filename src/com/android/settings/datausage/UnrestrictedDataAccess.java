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
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.view.View;
import com.android.settings.InstrumentedFragment;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.applications.AppStateBaseBridge;
import com.android.settingslib.applications.ApplicationsState;

import java.util.ArrayList;

public class UnrestrictedDataAccess extends SettingsPreferenceFragment
        implements ApplicationsState.Callbacks, AppStateBaseBridge.Callback, Preference.OnPreferenceChangeListener {

    private ApplicationsState mApplicationsState;
    private AppStateDataUsageBridge mDataUsageBridge;
    private ApplicationsState.Session mSession;
    private DataSaverBackend mDataSaverBackend;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setPreferenceScreen(getPreferenceManager().createPreferenceScreen(getContext()));
        getPreferenceScreen().setOrderingAsAdded(false);
        mApplicationsState = ApplicationsState.getInstance(
                (Application) getContext().getApplicationContext());
        mDataSaverBackend = new DataSaverBackend(getContext());
        mDataUsageBridge = new AppStateDataUsageBridge(mApplicationsState, this, mDataSaverBackend);
        mSession = mApplicationsState.newSession(this);
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
        ArrayList<ApplicationsState.AppEntry> apps = mSession.getAllApps();
        final int N = apps.size();
        for (int i = 0; i < N; i++) {
            ApplicationsState.AppEntry entry = apps.get(i);
            String key = entry.info.packageName + "|" + entry.info.uid;
            AccessPreference preference = (AccessPreference) findPreference(key);
            if (preference == null) {
                preference = new AccessPreference(getContext(), entry);
                preference.setKey(key);
                preference.setOnPreferenceChangeListener(this);
                getPreferenceScreen().addPreference(preference);
            }
            AppStateDataUsageBridge.DataUsageState state =
                    (AppStateDataUsageBridge.DataUsageState) entry.extraInfo;
            preference.setChecked(state.isDataSaverWhitelisted);
        }
        setLoading(false, true);
    }

    @Override
    public void onRunningStateChanged(boolean running) {

    }

    @Override
    public void onPackageListChanged() {

    }

    @Override
    public void onRebuildComplete(ArrayList<ApplicationsState.AppEntry> apps) {

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
    protected int getMetricsCategory() {
        return InstrumentedFragment.DATA_USAGE_UNRESTRICTED_ACCESS;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference instanceof AccessPreference) {
            AccessPreference accessPreference = (AccessPreference) preference;
            boolean whitelisted = newValue == Boolean.TRUE;
            mDataSaverBackend.setIsWhitelisted(accessPreference.mEntry.info.uid, whitelisted);
            ((AppStateDataUsageBridge.DataUsageState) accessPreference.mEntry.extraInfo)
                    .isDataSaverWhitelisted = whitelisted;
            return true;
        }
        return false;
    }

    private class AccessPreference extends SwitchPreference {
        private final ApplicationsState.AppEntry mEntry;

        public AccessPreference(Context context, ApplicationsState.AppEntry entry) {
            super(context);
            mEntry = entry;
            mEntry.ensureLabel(getContext());
            setTitle(entry.label);
            setChecked(((AppStateDataUsageBridge.DataUsageState) entry.extraInfo)
                    .isDataSaverWhitelisted);
        }

        @Override
        public void onBindViewHolder(PreferenceViewHolder holder) {
            holder.itemView.post(new Runnable() {
                @Override
                public void run() {
                    // Ensure we have an icon before binding.
                    mApplicationsState.ensureIcon(mEntry);
                    // This might trigger us to bind again, but it gives an easy way to only load the icon
                    // once its needed, so its probably worth it.
                    setIcon(mEntry.icon);
                }
            });
            super.onBindViewHolder(holder);
        }
    }
}
