/*
 * Copyright (C) 2017 The Android Open Source Project
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
package com.android.settings.applications;

import android.app.Application;
import android.content.Context;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.datausage.AppStateDataUsageBridge;
import com.android.settings.datausage.AppStateDataUsageBridge.DataUsageState;
import com.android.settings.datausage.DataSaverBackend;
import com.android.settingslib.applications.ApplicationsState;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnDestroy;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;

import java.util.ArrayList;

public class SpecialAppAccessPreferenceController extends BasePreferenceController implements
        AppStateBaseBridge.Callback, ApplicationsState.Callbacks, LifecycleObserver, OnStart,
        OnStop, OnDestroy {

    @VisibleForTesting
    ApplicationsState.Session mSession;

    private final ApplicationsState mApplicationsState;
    private final AppStateDataUsageBridge mDataUsageBridge;
    private final DataSaverBackend mDataSaverBackend;

    private Preference mPreference;
    private boolean mExtraLoaded;


    public SpecialAppAccessPreferenceController(Context context, String key) {
        super(context, key);
        mApplicationsState = ApplicationsState.getInstance(
                (Application) context.getApplicationContext());
        mDataSaverBackend = new DataSaverBackend(context);
        mDataUsageBridge = new AppStateDataUsageBridge(mApplicationsState, this, mDataSaverBackend);
    }

    public void setSession(Lifecycle lifecycle) {
        mSession = mApplicationsState.newSession(this, lifecycle);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
    }

    @Override
    public void onStart() {
        mDataUsageBridge.resume();
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
    public void updateState(Preference preference) {
        updateSummary();
    }

    @Override
    public void onExtraInfoUpdated() {
        mExtraLoaded = true;
        updateSummary();
    }

    private void updateSummary() {
        if (!mExtraLoaded || mPreference == null) {
            return;
        }

        final ArrayList<ApplicationsState.AppEntry> allApps = mSession.getAllApps();
        int count = 0;
        for (ApplicationsState.AppEntry entry : allApps) {
            if (!ApplicationsState.FILTER_DOWNLOADED_AND_LAUNCHER.filterApp(entry)) {
                continue;
            }
            if (entry.extraInfo instanceof DataUsageState
                    && ((DataUsageState) entry.extraInfo).isDataSaverWhitelisted) {
                count++;
            }
        }
        mPreference.setSummary(mContext.getResources().getQuantityString(
                R.plurals.special_access_summary, count, count));
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
        // when the value of the AppEntry.hasLauncherEntry was changed.
        updateSummary();
    }

    @Override
    public void onLoadEntriesCompleted() {
    }
}
