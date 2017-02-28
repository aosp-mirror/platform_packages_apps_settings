/*
 * Copyright (C) 2016 The Android Open Source Project
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

import android.app.LoaderManager;
import android.content.Context;
import android.content.Loader;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.provider.SearchIndexableResource;
import android.util.SparseArray;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.applications.PackageManagerWrapperImpl;
import com.android.settings.applications.UserManagerWrapper;
import com.android.settings.applications.UserManagerWrapperImpl;
import com.android.settings.core.PreferenceController;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.deviceinfo.storage.SecondaryUserController;
import com.android.settings.deviceinfo.storage.StorageAsyncLoader;
import com.android.settings.deviceinfo.storage.StorageItemPreferenceController;
import com.android.settings.deviceinfo.storage.StorageSummaryDonutPreferenceController;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settingslib.applications.StorageStatsSource;
import com.android.settingslib.deviceinfo.StorageManagerVolumeProvider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class StorageDashboardFragment extends DashboardFragment
    implements LoaderManager.LoaderCallbacks<SparseArray<StorageAsyncLoader.AppsStorageResult>> {
    private static final String TAG = "StorageDashboardFrag";
    private static final int STORAGE_JOB_ID = 0;

    private VolumeInfo mVolume;

    private StorageSummaryDonutPreferenceController mSummaryController;
    private StorageItemPreferenceController mPreferenceController;
    private List<PreferenceController> mSecondaryUsers;

    @Override
    public void onResume() {
        super.onResume();
        getLoaderManager().initLoader(STORAGE_JOB_ID, Bundle.EMPTY, this);
    }

    @Override
    public Loader<SparseArray<StorageAsyncLoader.AppsStorageResult>> onCreateLoader(int id,
            Bundle args) {
        Context context = getContext();
        return new StorageAsyncLoader(context,
                new UserManagerWrapperImpl(context.getSystemService(UserManager.class)),
                mVolume.fsUuid,
                new StorageStatsSource(context),
                new PackageManagerWrapperImpl(context.getPackageManager()));
    }

    @Override
    public void onLoadFinished(Loader<SparseArray<StorageAsyncLoader.AppsStorageResult>> loader,
            SparseArray<StorageAsyncLoader.AppsStorageResult> data) {
        mPreferenceController.onLoadFinished(data.get(UserHandle.myUserId()));
        updateSecondaryUserControllers(mSecondaryUsers, data);
    }

    @Override
    public void onLoaderReset(Loader<SparseArray<StorageAsyncLoader.AppsStorageResult>> loader) {
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        // Initialize the storage sizes that we can quickly calc.
        final Context context = getActivity();
        StorageManager sm = context.getSystemService(StorageManager.class);
        mVolume = Utils.maybeInitializeVolume(sm, getArguments());
        if (mVolume == null) {
            getActivity().finish();
            return;
        }

        final long sharedDataSize = mVolume.getPath().getTotalSpace();
        long totalSize = sm.getPrimaryStorageSize();
        long systemSize = totalSize - sharedDataSize;

        if (totalSize <= 0) {
            totalSize = sharedDataSize;
            systemSize = 0;
        }

        final long usedBytes = totalSize - mVolume.getPath().getFreeSpace();
        mSummaryController.updateBytes(usedBytes, totalSize);
        mPreferenceController.setVolume(mVolume);
        mPreferenceController.setSystemSize(systemSize);
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.SETTINGS_STORAGE_CATEGORY;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.storage_dashboard_fragment;
    }

    @Override
    protected List<PreferenceController> getPreferenceControllers(Context context) {
        final List<PreferenceController> controllers = new ArrayList<>();
        mSummaryController = new StorageSummaryDonutPreferenceController(context);
        controllers.add(mSummaryController);

        StorageManager sm = context.getSystemService(StorageManager.class);
        mPreferenceController = new StorageItemPreferenceController(context, this,
                mVolume, new StorageManagerVolumeProvider(sm));
        controllers.add(mPreferenceController);

        UserManagerWrapper userManager =
                new UserManagerWrapperImpl(context.getSystemService(UserManager.class));
        mSecondaryUsers = SecondaryUserController.getSecondaryUserControllers(context, userManager);
        controllers.addAll(mSecondaryUsers);

        controllers.add(new ManageStoragePreferenceController(context));
        return controllers;
    }

    /**
     * Updates the secondary user controller sizes.
     */
    private void updateSecondaryUserControllers(List<PreferenceController> controllers,
            SparseArray<StorageAsyncLoader.AppsStorageResult> stats) {
        for (int i = 0, size = controllers.size(); i < size; i++) {
            PreferenceController controller = controllers.get(i);
            if (controller instanceof StorageAsyncLoader.ResultHandler) {
                StorageAsyncLoader.ResultHandler userController =
                        (StorageAsyncLoader.ResultHandler) controller;
                userController.handleResult(stats);
            }
        }
    }

    /**
     * For Search.
     */
    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(
                        Context context, boolean enabled) {
                    if (!FeatureFactory.getFactory(context).getDashboardFeatureProvider(context)
                            .isEnabled()) {
                        return null;
                    }
                    final SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.storage_dashboard_fragment;
                    return Arrays.asList(sir);
                }

                @Override
                public List<PreferenceController> getPreferenceControllers(Context context) {
                    final StorageManager sm = context.getSystemService(StorageManager.class);
                    final UserManagerWrapper userManager =
                            new UserManagerWrapperImpl(context.getSystemService(UserManager.class));
                    final List<PreferenceController> controllers = new ArrayList<>();
                    controllers.add(new StorageSummaryDonutPreferenceController(context));
                    controllers.add(new StorageItemPreferenceController(context, null /* host */,
                            null /* volume */, new StorageManagerVolumeProvider(sm)));
                    controllers.addAll(SecondaryUserController.getSecondaryUserControllers(
                            context, userManager));
                    controllers.add(new ManageStoragePreferenceController(context));
                    return controllers;
                }

            };
}
