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
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.os.Bundle;
import android.telephony.SubscriptionManager;
import android.widget.Switch;

import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.applications.AppStateBaseBridge.Callback;
import com.android.settings.datausage.DataSaverBackend.Listener;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.widget.SettingsMainSwitchBar;
import com.android.settingslib.applications.ApplicationsState;
import com.android.settingslib.applications.ApplicationsState.AppEntry;
import com.android.settingslib.applications.ApplicationsState.Callbacks;
import com.android.settingslib.applications.ApplicationsState.Session;
import com.android.settingslib.search.SearchIndexable;
import com.android.settingslib.widget.OnMainSwitchChangeListener;

import java.util.ArrayList;

@SearchIndexable
public class DataSaverSummary extends SettingsPreferenceFragment
        implements OnMainSwitchChangeListener, Listener, Callback, Callbacks {

    private static final String KEY_UNRESTRICTED_ACCESS = "unrestricted_access";

    private SettingsMainSwitchBar mSwitchBar;
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
        mSession = mApplicationsState.newSession(this, getSettingsLifecycle());
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mSwitchBar = ((SettingsActivity) getActivity()).getSwitchBar();
        mSwitchBar.setTitle(getContext().getString(R.string.data_saver_switch_title));
        mSwitchBar.show();
        mSwitchBar.addOnSwitchChangeListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        mDataSaverBackend.refreshAllowlist();
        mDataSaverBackend.refreshDenylist();
        mDataSaverBackend.addListener(this);
        mDataUsageBridge.resume(true /* forceLoadAllApps */);
    }

    @Override
    public void onPause() {
        super.onPause();
        mDataSaverBackend.remListener(this);
        mDataUsageBridge.pause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mDataUsageBridge.release();
    }

    @Override
    public void onSwitchChanged(Switch switchView, boolean isChecked) {
        synchronized (this) {
            if (mSwitching) {
                return;
            }
            mSwitching = true;
            mDataSaverBackend.setDataSaverEnabled(isChecked);
        }
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.DATA_SAVER_SUMMARY;
    }

    @Override
    public int getHelpResource() {
        return R.string.help_url_data_saver;
    }

    @Override
    public void onDataSaverChanged(boolean isDataSaving) {
        synchronized (this) {
            mSwitchBar.setChecked(isDataSaving);
            mSwitching = false;
        }
    }

    @Override
    public void onAllowlistStatusChanged(int uid, boolean isAllowlisted) {
    }

    @Override
    public void onDenylistStatusChanged(int uid, boolean isDenylisted) {
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
                    entry.extraInfo).isDataSaverAllowlisted) {
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

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.data_saver) {

                @Override
                protected boolean isPageSearchEnabled(Context context) {
                    return DataUsageUtils.hasMobileData(context)
                            && DataUsageUtils.getDefaultSubscriptionId(context)
                            != SubscriptionManager.INVALID_SUBSCRIPTION_ID;
                }
            };
}
