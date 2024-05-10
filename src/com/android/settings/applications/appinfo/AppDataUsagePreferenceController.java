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

package com.android.settings.applications.appinfo;

import android.content.Context;
import android.net.NetworkTemplate;
import android.os.Bundle;
import android.os.Process;
import android.text.format.DateUtils;
import android.text.format.Formatter;

import androidx.annotation.VisibleForTesting;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settings.datausage.AppDataUsage;
import com.android.settings.datausage.lib.NetworkTemplates;
import com.android.settings.spa.app.appinfo.AppDataUsagePreferenceKt;
import com.android.settingslib.AppItem;
import com.android.settingslib.applications.AppUtils;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;
import com.android.settingslib.net.NetworkCycleDataForUid;
import com.android.settingslib.net.NetworkCycleDataForUidLoader;

import java.util.List;

/**
 * @deprecated Will be removed, use {@link AppDataUsagePreferenceKt} instead.
 */
@Deprecated(forRemoval = true)
public class AppDataUsagePreferenceController extends AppInfoPreferenceControllerBase
        implements LoaderManager.LoaderCallbacks<List<NetworkCycleDataForUid>>, LifecycleObserver,
        OnResume, OnPause {

    private List<NetworkCycleDataForUid> mAppUsageData;

    public AppDataUsagePreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public int getAvailabilityStatus() {
        return isBandwidthControlEnabled() ? AVAILABLE : CONDITIONALLY_UNAVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference.setEnabled(AppUtils.isAppInstalled(mAppEntry));
    }

    @Override
    public void updateState(Preference preference) {
        preference.setSummary(getDataSummary());
    }

    @Override
    public void onResume() {
        if (isAvailable()) {
            final int uid = mParent.getAppEntry().info.uid;
            final AppItem app = new AppItem(uid);
            app.addUid(uid);
            mParent.getLoaderManager().restartLoader(mParent.LOADER_CHART_DATA, null /* args */,
                    this);
        }
    }

    @Override
    public void onPause() {
        if (isAvailable()) {
            mParent.getLoaderManager().destroyLoader(mParent.LOADER_CHART_DATA);
        }
    }

    @Override
    public Loader<List<NetworkCycleDataForUid>> onCreateLoader(int id, Bundle args) {
        final NetworkTemplate template = NetworkTemplates.INSTANCE.getDefaultTemplate(mContext);
        final int uid = mParent.getAppEntry().info.uid;

        final NetworkCycleDataForUidLoader.Builder builder =
                NetworkCycleDataForUidLoader.builder(mContext);
        builder.setRetrieveDetail(false)
               .setNetworkTemplate(template);

        builder.addUid(uid);
        if (Process.isApplicationUid(uid)) {
            // Also add in network usage for the app's SDK sandbox
            builder.addUid(Process.toSdkSandboxUid(uid));
        }
        return builder.build();
    }

    @Override
    public void onLoadFinished(Loader<List<NetworkCycleDataForUid>> loader,
            List<NetworkCycleDataForUid> data) {
        mAppUsageData = data;
        updateState(mPreference);
    }

    @Override
    public void onLoaderReset(Loader<List<NetworkCycleDataForUid>> loader) {
        // Leave last result.
    }

    @Override
    protected Class<? extends SettingsPreferenceFragment> getDetailFragmentClass() {
        return AppDataUsage.class;
    }

    private CharSequence getDataSummary() {
        if (mAppUsageData != null) {
            long totalBytes = 0;
            long startTime = System.currentTimeMillis();
            for (NetworkCycleDataForUid data : mAppUsageData) {
                totalBytes += data.getTotalUsage();
                final long cycleStart = data.getStartTime();
                if (cycleStart < startTime) {
                    startTime = cycleStart;
                }
            }
            if (totalBytes == 0) {
                return mContext.getString(R.string.no_data_usage);
            }
            return mContext.getString(R.string.data_summary_format,
                    Formatter.formatFileSize(mContext, totalBytes, Formatter.FLAG_IEC_UNITS),
                    DateUtils.formatDateTime(mContext, startTime,
                            DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_ABBREV_MONTH));
        }
        return mContext.getString(R.string.computing_size);
    }

    @VisibleForTesting
    boolean isBandwidthControlEnabled() {
        return Utils.isBandwidthControlEnabled();
    }

}
