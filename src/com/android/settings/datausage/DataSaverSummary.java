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
import android.os.Bundle;
import android.support.v7.preference.Preference;
import android.widget.Switch;

import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.applications.AppStateBaseBridge.Callback;
import com.android.settings.datausage.DataSaverBackend.Listener;
import com.android.settings.widget.SwitchBar;
import com.android.settings.widget.SwitchBar.OnSwitchChangeListener;
import com.android.settingslib.applications.ApplicationsState;
import com.android.settingslib.applications.ApplicationsState.AppEntry;
import com.android.settingslib.applications.ApplicationsState.Callbacks;
import com.android.settingslib.applications.ApplicationsState.Session;

import java.util.ArrayList;

public class DataSaverSummary extends SettingsPreferenceFragment
        implements OnSwitchChangeListener, Listener, Callback, Callbacks {

    private static final String KEY_UNRESTRICTED_ACCESS = "unrestricted_access";

    private SwitchBar mSwitchBar;
    private DataSaverBackend mDataSaverBackend;
    private Preference mUnrestrictedAccess;
    private ApplicationsState mApplicationsState;
    private AppStateDataUsageBridge mDataUsageBridge;
    private Session mSession;

    // Flag used to avoid infinite loop due if user switch it on/off too quicky.
    private boolean mSwitching;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        addPreferencesFromResource(R.xml.data_saver);
        mUnrestrictedAccess = findPreference(KEY_UNRESTRICTED_ACCESS);
        mApplicationsState = ApplicationsState.getInstance(
                (Application) getContext().getApplicationContext());
        mDataSaverBackend = new DataSaverBackend(getContext());
        mDataUsageBridge = new AppStateDataUsageBridge(mApplicationsState, this, mDataSaverBackend);
        mSession = mApplicationsState.newSession(this);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mSwitchBar = ((SettingsActivity) getActivity()).getSwitchBar();
        mSwitchBar.show();
        mSwitchBar.addOnSwitchChangeListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        mDataSaverBackend.refreshWhitelist();
        mDataSaverBackend.refreshBlacklist();
        mDataSaverBackend.addListener(this);
        mSession.resume();
        mDataUsageBridge.resume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mDataSaverBackend.remListener(this);
        mDataUsageBridge.pause();
        mSession.pause();
    }

    @Override
    public void onSwitchChanged(Switch switchView, boolean isChecked) {
        synchronized(this) {
            if (mSwitching) {
                return;
            }
            mSwitching = true;
            mDataSaverBackend.setDataSaverEnabled(isChecked);
        }
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.DATA_SAVER_SUMMARY;
    }

    @Override
    protected int getHelpResource() {
        return R.string.help_url_data_saver;
    }

    @Override
    public void onDataSaverChanged(boolean isDataSaving) {
        synchronized(this) {
            mSwitchBar.setChecked(isDataSaving);
            mSwitching = false;
        }
    }

    @Override
    public void onWhitelistStatusChanged(int uid, boolean isWhitelisted) {
    }

    @Override
    public void onBlacklistStatusChanged(int uid, boolean isBlacklisted) {
    }

    @Override
    public void onExtraInfoUpdated() {
        if (!isAdded()) {
            return;
        }
        int count = 0;
        final ArrayList<AppEntry> allApps = mSession.getAllApps();
        final int N = allApps.size();
        for (int i = 0; i < N; i++) {
            final AppEntry entry = allApps.get(i);
            if (!ApplicationsState.FILTER_DOWNLOADED_AND_LAUNCHER.filterApp(entry)) {
                continue;
            }
            if (entry.extraInfo != null && ((AppStateDataUsageBridge.DataUsageState)
                    entry.extraInfo).isDataSaverWhitelisted) {
                count++;
            }
        }
        mUnrestrictedAccess.setSummary(getResources().getQuantityString(
                R.plurals.data_saver_unrestricted_summary, count, count));
    }

    @Override
    public void onRunningStateChanged(boolean running) {

    }

    @Override
    public void onPackageListChanged() {

    }

    @Override
    public void onRebuildComplete(ArrayList<AppEntry> apps) {

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
}
