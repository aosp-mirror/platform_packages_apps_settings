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
import android.content.Intent;
import android.content.Loader;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.provider.SearchIndexableResource;
import android.support.annotation.VisibleForTesting;
import android.util.Log;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.applications.PackageManagerWrapper;
import com.android.settings.applications.PackageManagerWrapperImpl;
import com.android.settings.applications.UserManagerWrapper;
import com.android.settings.applications.UserManagerWrapperImpl;
import com.android.settings.core.PreferenceController;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.deviceinfo.storage.AutomaticStorageManagementSwitchPreferenceController;
import com.android.settings.deviceinfo.storage.SecondaryUserController;
import com.android.settings.deviceinfo.storage.StorageAsyncLoader;
import com.android.settings.deviceinfo.storage.StorageItemPreferenceController;
import com.android.settings.deviceinfo.storage.StorageSummaryDonutPreferenceController;
import com.android.settings.deviceinfo.storage.UserIconLoader;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settingslib.applications.StorageStatsSource;
import com.android.settingslib.deviceinfo.StorageManagerVolumeProvider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class StorageDashboardFragment extends DashboardFragment
    implements LoaderManager.LoaderCallbacks<SparseArray<StorageAsyncLoader.AppsStorageResult>> {
    private static final String TAG = "StorageDashboardFrag";
    private static final int STORAGE_JOB_ID = 0;
    private static final int ICON_JOB_ID = 1;
    private static final int OPTIONS_MENU_MIGRATE_DATA = 100;

    private VolumeInfo mVolume;

    private StorageSummaryDonutPreferenceController mSummaryController;
    private StorageItemPreferenceController mPreferenceController;
    private PrivateVolumeOptionMenuController mOptionMenuController;
    private List<PreferenceController> mSecondaryUsers;

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

        mOptionMenuController = new PrivateVolumeOptionMenuController(
                context, mVolume, new PackageManagerWrapperImpl(context.getPackageManager()));

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

        mPreferenceController.setTotalSize(totalSize);
        for (int i = 0, size = mSecondaryUsers.size(); i < size; i++) {
            PreferenceController controller = mSecondaryUsers.get(i);
            if (controller instanceof SecondaryUserController) {
                SecondaryUserController userController = (SecondaryUserController) controller;
                userController.setTotalSize(totalSize);

            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        getLoaderManager().restartLoader(STORAGE_JOB_ID, Bundle.EMPTY, this);
        getLoaderManager().initLoader(ICON_JOB_ID, Bundle.EMPTY, new IconLoaderCallbacks());
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

        final AutomaticStorageManagementSwitchPreferenceController asmController =
                new AutomaticStorageManagementSwitchPreferenceController(
                        context, mMetricsFeatureProvider, getFragmentManager());
        getLifecycle().addObserver(asmController);
        getLifecycle().addObserver(mOptionMenuController);
        controllers.add(asmController);
        return controllers;
    }

    @VisibleForTesting
    protected void setVolume(VolumeInfo info) {
        mVolume = info;
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
                    return controllers;
                }

            };

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

    /**
     * IconLoaderCallbacks exists because StorageDashboardFragment already implements
     * LoaderCallbacks for a different type.
     */
    public final class IconLoaderCallbacks
            implements LoaderManager.LoaderCallbacks<SparseArray<Drawable>> {
        @Override
        public Loader<SparseArray<Drawable>> onCreateLoader(int id, Bundle args) {
            return new UserIconLoader(
                    getContext(),
                    () -> UserIconLoader.loadUserIconsWithContext(getContext()));
        }

        @Override
        public void onLoadFinished(
                Loader<SparseArray<Drawable>> loader, SparseArray<Drawable> data) {
            mSecondaryUsers
                    .stream()
                    .filter(controller -> controller instanceof UserIconLoader.UserIconHandler)
                    .forEach(
                            controller ->
                                    ((UserIconLoader.UserIconHandler) controller)
                                            .handleUserIcons(data));
        }

        @Override
        public void onLoaderReset(Loader<SparseArray<Drawable>> loader) {}
    }
}
