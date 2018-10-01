/*
 * Copyright (C) 2017 The Android Open Source Project
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
import android.net.INetworkStatsService;
import android.net.INetworkStatsSession;
import android.net.NetworkTemplate;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
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
import com.android.settings.datausage.DataUsageList;
import com.android.settings.datausage.DataUsageUtils;
import com.android.settingslib.AppItem;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;
import com.android.settingslib.net.ChartData;
import com.android.settingslib.net.ChartDataLoaderCompat;

public class AppDataUsagePreferenceControllerV2 extends AppInfoPreferenceControllerBase
        implements LoaderManager.LoaderCallbacks<ChartData>, LifecycleObserver, OnResume, OnPause {

    private ChartData mChartData;
    private INetworkStatsSession mStatsSession;

    public AppDataUsagePreferenceControllerV2(Context context,String key) {
        super(context, key);
    }

    @Override
    public int getAvailabilityStatus() {
        return isBandwidthControlEnabled() ? AVAILABLE : CONDITIONALLY_UNAVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        if (isAvailable()) {
            final INetworkStatsService statsService = INetworkStatsService.Stub.asInterface(
                    ServiceManager.getService(Context.NETWORK_STATS_SERVICE));
            try {
                mStatsSession = statsService.openSession();
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void updateState(Preference preference) {
        preference.setSummary(getDataSummary());
    }

    @Override
    public void onResume() {
        if (mStatsSession != null) {
            final int uid = mParent.getAppEntry().info.uid;
            final AppItem app = new AppItem(uid);
            app.addUid(uid);
            mParent.getLoaderManager().restartLoader(mParent.LOADER_CHART_DATA,
                    ChartDataLoaderCompat.buildArgs(getTemplate(mContext), app),
                    this);
        }
    }

    @Override
    public void onPause() {
        mParent.getLoaderManager().destroyLoader(mParent.LOADER_CHART_DATA);
    }

    @Override
    public Loader<ChartData> onCreateLoader(int id, Bundle args) {
        return new ChartDataLoaderCompat(mContext, mStatsSession, args);
    }

    @Override
    public void onLoadFinished(Loader<ChartData> loader, ChartData data) {
        mChartData = data;
        updateState(mPreference);
    }

    @Override
    public void onLoaderReset(Loader<ChartData> loader) {
        // Leave last result.
    }

    @Override
    protected Class<? extends SettingsPreferenceFragment> getDetailFragmentClass() {
        return AppDataUsage.class;
    }

    private CharSequence getDataSummary() {
        if (mChartData != null) {
            final long totalBytes = mChartData.detail.getTotalBytes();
            if (totalBytes == 0) {
                return mContext.getString(R.string.no_data_usage);
            }
            return mContext.getString(R.string.data_summary_format,
                    Formatter.formatFileSize(mContext, totalBytes),
                    DateUtils.formatDateTime(mContext, mChartData.detail.getStart(),
                            DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_ABBREV_MONTH));
        }
        return mContext.getString(R.string.computing_size);
    }

    private static NetworkTemplate getTemplate(Context context) {
        if (DataUsageUtils.hasReadyMobileRadio(context)) {
            return NetworkTemplate.buildTemplateMobileWildcard();
        }
        if (DataUsageUtils.hasWifiRadio(context)) {
            return NetworkTemplate.buildTemplateWifiWildcard();
        }
        return NetworkTemplate.buildTemplateEthernet();
    }

    @VisibleForTesting
    boolean isBandwidthControlEnabled() {
        return Utils.isBandwidthControlEnabled();
    }

}
