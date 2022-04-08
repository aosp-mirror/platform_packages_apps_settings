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

package com.android.settings.deviceinfo;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.util.SparseArray;

import androidx.annotation.VisibleForTesting;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.deviceinfo.storage.StorageAsyncLoader;
import com.android.settings.deviceinfo.storage.StorageAsyncLoader.AppsStorageResult;
import com.android.settings.deviceinfo.storage.StorageItemPreferenceController;
import com.android.settingslib.applications.StorageStatsSource;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.deviceinfo.StorageManagerVolumeProvider;

import java.util.ArrayList;
import java.util.List;

/**
 * StorageProfileFragment is a fragment which shows the storage results for a profile of the
 * primary user.
 */
public class StorageProfileFragment extends DashboardFragment
        implements LoaderManager.LoaderCallbacks<SparseArray<AppsStorageResult>> {
    private static final String TAG = "StorageProfileFragment";
    public static final String USER_ID_EXTRA = "userId";
    private static final int APPS_JOB_ID = 0;

    private VolumeInfo mVolume;
    private int mUserId;
    private StorageItemPreferenceController mPreferenceController;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        final Bundle args = getArguments();

        // Initialize the storage sizes that we can quickly calc.
        final Context context = getActivity();
        final StorageManager sm = context.getSystemService(StorageManager.class);
        mVolume = Utils.maybeInitializeVolume(sm, args);
        if (mVolume == null) {
            getActivity().finish();
            return;
        }

        mPreferenceController.setVolume(mVolume);
        mUserId = args.getInt(USER_ID_EXTRA, UserHandle.myUserId());
        mPreferenceController.setUserId(UserHandle.of(mUserId));
    }

    @Override
    public void onResume() {
        super.onResume();
        getLoaderManager().initLoader(APPS_JOB_ID, Bundle.EMPTY, this);
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.SETTINGS_STORAGE_PROFILE;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.storage_profile_fragment;
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        final List<AbstractPreferenceController> controllers = new ArrayList<>();
        final StorageManager sm = context.getSystemService(StorageManager.class);
        mPreferenceController =
                new StorageItemPreferenceController(
                        context,
                        this,
                        mVolume,
                        new StorageManagerVolumeProvider(sm),
                        /* isWorkProfile */ true);
        controllers.add(mPreferenceController);
        return controllers;
    }

    @Override
    public Loader<SparseArray<AppsStorageResult>> onCreateLoader(int id, Bundle args) {
        final Context context = getContext();
        return new StorageAsyncLoader(context,
                context.getSystemService(UserManager.class),
                mVolume.fsUuid,
                new StorageStatsSource(context),
                context.getPackageManager());
    }

    @Override
    public void onLoadFinished(Loader<SparseArray<AppsStorageResult>> loader,
            SparseArray<AppsStorageResult> result) {
        mPreferenceController.onLoadFinished(result, mUserId);
    }

    @Override
    public void onLoaderReset(Loader<SparseArray<AppsStorageResult>> loader) {
    }

    @VisibleForTesting
    void setPreferenceController(StorageItemPreferenceController controller) {
        mPreferenceController = controller;
    }
}
