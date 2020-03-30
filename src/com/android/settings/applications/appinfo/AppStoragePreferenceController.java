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
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.os.UserHandle;
import android.text.format.Formatter;

import androidx.annotation.VisibleForTesting;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.applications.AppStorageSettings;
import com.android.settings.applications.FetchPackageStorageAsyncLoader;
import com.android.settingslib.applications.ApplicationsState;
import com.android.settingslib.applications.StorageStatsSource;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;

public class AppStoragePreferenceController extends AppInfoPreferenceControllerBase
        implements LoaderManager.LoaderCallbacks<StorageStatsSource.AppStorageStats>,
        LifecycleObserver, OnResume, OnPause {

    private StorageStatsSource.AppStorageStats mLastResult;

    public AppStoragePreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public void updateState(Preference preference) {
        final ApplicationsState.AppEntry entry = mParent.getAppEntry();
        if (entry != null && entry.info != null) {
            final boolean isExternal =
                    (entry.info.flags & ApplicationInfo.FLAG_EXTERNAL_STORAGE) != 0;
            preference.setSummary(getStorageSummary(mLastResult, isExternal));
        }
    }

    @Override
    public void onResume() {
        mParent.getLoaderManager().restartLoader(mParent.LOADER_STORAGE, Bundle.EMPTY, this);
    }

    @Override
    public void onPause() {
        mParent.getLoaderManager().destroyLoader(mParent.LOADER_STORAGE);
    }

    @Override
    protected Class<? extends SettingsPreferenceFragment> getDetailFragmentClass() {
        return AppStorageSettings.class;
    }

    @VisibleForTesting
    CharSequence getStorageSummary(
            StorageStatsSource.AppStorageStats stats, boolean isExternal) {
        if (stats == null) {
            return mContext.getText(R.string.computing_size);
        }
        final CharSequence storageType = mContext.getString(isExternal
                ? R.string.storage_type_external
                : R.string.storage_type_internal);
        return mContext.getString(R.string.storage_summary_format,
                Formatter.formatFileSize(mContext, stats.getTotalBytes()),
                storageType.toString());
    }

    @Override
    public Loader<StorageStatsSource.AppStorageStats> onCreateLoader(int id, Bundle args) {
        return new FetchPackageStorageAsyncLoader(mContext, new StorageStatsSource(mContext),
                mParent.getAppEntry().info, UserHandle.of(UserHandle.myUserId()));
    }

    @Override
    public void onLoadFinished(Loader<StorageStatsSource.AppStorageStats> loader,
            StorageStatsSource.AppStorageStats result) {
        mLastResult = result;
        updateState(mPreference);
    }

    @Override
    public void onLoaderReset(Loader<StorageStatsSource.AppStorageStats> loader) {
    }

}
