/*
 * Copyright (C) 2021 The Android Open Source Project
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
import android.app.usage.StorageStatsManager;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.UserManager;
import android.os.storage.StorageManager;
import android.util.SparseArray;
import android.view.View;

import androidx.annotation.VisibleForTesting;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.dashboard.profileselector.ProfileSelectFragment;
import com.android.settings.deviceinfo.storage.ManageStoragePreferenceController;
import com.android.settings.deviceinfo.storage.SecondaryUserController;
import com.android.settings.deviceinfo.storage.StorageAsyncLoader;
import com.android.settings.deviceinfo.storage.StorageCacheHelper;
import com.android.settings.deviceinfo.storage.StorageEntry;
import com.android.settings.deviceinfo.storage.StorageItemPreferenceController;
import com.android.settings.deviceinfo.storage.UserIconLoader;
import com.android.settings.deviceinfo.storage.VolumeSizesLoader;
import com.android.settingslib.applications.StorageStatsSource;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.deviceinfo.PrivateStorageInfo;
import com.android.settingslib.deviceinfo.StorageManagerVolumeProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Storage Settings main UI is composed by 3 fragments:
 *
 * StorageDashboardFragment only shows when there is only personal profile for current user.
 *
 * ProfileSelectStorageFragment (controls preferences above profile tab) and
 * StorageCategoryFragment (controls preferences below profile tab) only show when current
 * user has installed work profile.
 *
 * ProfileSelectStorageFragment and StorageCategoryFragment have many similar or the same
 * code as StorageDashboardFragment. Remember to sync code between these fragments when you have to
 * change Storage Settings.
 */
public class StorageCategoryFragment extends DashboardFragment
        implements
        LoaderManager.LoaderCallbacks<SparseArray<StorageAsyncLoader.StorageResult>> {
    private static final String TAG = "StorageCategoryFrag";
    private static final String SELECTED_STORAGE_ENTRY_KEY = "selected_storage_entry_key";
    private static final String SUMMARY_PREF_KEY = "storage_summary";
    private static final int STORAGE_JOB_ID = 0;
    private static final int ICON_JOB_ID = 1;
    private static final int VOLUME_SIZE_JOB_ID = 2;

    private StorageManager mStorageManager;
    private UserManager mUserManager;
    private StorageEntry mSelectedStorageEntry;
    private PrivateStorageInfo mStorageInfo;
    private SparseArray<StorageAsyncLoader.StorageResult> mAppsResult;

    private StorageItemPreferenceController mPreferenceController;
    private List<AbstractPreferenceController> mSecondaryUsers;
    private boolean mIsWorkProfile;
    private int mUserId;
    private boolean mIsLoadedFromCache;
    private StorageCacheHelper mStorageCacheHelper;

    /**
     * Refresh UI for specified storageEntry.
     */
    public void refreshUi(StorageEntry storageEntry) {
        mSelectedStorageEntry = storageEntry;
        if (mPreferenceController == null) {
            // Check null here because when onResume, StorageCategoryFragment may not
            // onAttach to createPreferenceControllers and mPreferenceController will be null.
            return;
        }

        // To prevent flicker, hides secondary users preference.
        // onReceivedSizes will set it visible for private storage.
        setSecondaryUsersVisible(false);

        if (!mSelectedStorageEntry.isMounted()) {
            // Set null volume to hide category stats.
            mPreferenceController.setVolume(null);
            return;
        }
        if (mStorageCacheHelper.hasCachedSizeInfo() && mSelectedStorageEntry.isPrivate()) {
            StorageCacheHelper.StorageCache cachedData = mStorageCacheHelper.retrieveCachedSize();
            mPreferenceController.setVolume(mSelectedStorageEntry.getVolumeInfo());
            mPreferenceController.setUsedSize(cachedData.totalUsedSize);
            mPreferenceController.setTotalSize(cachedData.totalSize);
        }
        if (mSelectedStorageEntry.isPrivate()) {
            mStorageInfo = null;
            mAppsResult = null;
            if (mStorageCacheHelper.hasCachedSizeInfo()) {
                mPreferenceController.onLoadFinished(mAppsResult, mUserId);
            } else {
                maybeSetLoading(isQuotaSupported());
                // To prevent flicker, sets null volume to hide category preferences.
                // onReceivedSizes will setVolume with the volume of selected storage.
                mPreferenceController.setVolume(null);
            }

            // Stats data is only available on private volumes.
            getLoaderManager().restartLoader(STORAGE_JOB_ID, Bundle.EMPTY, this);
            getLoaderManager()
                 .restartLoader(VOLUME_SIZE_JOB_ID, Bundle.EMPTY, new VolumeSizeCallbacks());
            getLoaderManager().restartLoader(ICON_JOB_ID, Bundle.EMPTY, new IconLoaderCallbacks());
        } else {
            mPreferenceController.setVolume(mSelectedStorageEntry.getVolumeInfo());
        }
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        mStorageManager = getActivity().getSystemService(StorageManager.class);

        if (icicle != null) {
            mSelectedStorageEntry = icicle.getParcelable(SELECTED_STORAGE_ENTRY_KEY);
        }

        if (mStorageCacheHelper.hasCachedSizeInfo()) {
            mIsLoadedFromCache = true;
            if (mSelectedStorageEntry != null) {
                refreshUi(mSelectedStorageEntry);
            }
            updateSecondaryUserControllers(mSecondaryUsers, mAppsResult);
            setSecondaryUsersVisible(true);
        }
    }

    @Override
    public void onAttach(Context context) {
        // These member variables are initialized befoer super.onAttach for
        // createPreferenceControllers to work correctly.
        mUserManager = context.getSystemService(UserManager.class);
        mIsWorkProfile = getArguments().getInt(ProfileSelectFragment.EXTRA_PROFILE)
                == ProfileSelectFragment.ProfileType.WORK;
        mUserId = Utils.getCurrentUserId(mUserManager, mIsWorkProfile);
        mStorageCacheHelper = new StorageCacheHelper(getContext(), mUserId);

        super.onAttach(context);

        ManageStoragePreferenceController manageStoragePreferenceController =
                use(ManageStoragePreferenceController.class);
        manageStoragePreferenceController.setUserId(mUserId);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mIsLoadedFromCache) {
            mIsLoadedFromCache = false;
        } else {
            if (mSelectedStorageEntry != null) {
                refreshUi(mSelectedStorageEntry);
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        // Destroy the data loaders to prevent unnecessary data loading when switching back to the
        // page.
        getLoaderManager().destroyLoader(STORAGE_JOB_ID);
        getLoaderManager().destroyLoader(ICON_JOB_ID);
        getLoaderManager().destroyLoader(VOLUME_SIZE_JOB_ID);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putParcelable(SELECTED_STORAGE_ENTRY_KEY, mSelectedStorageEntry);
        super.onSaveInstanceState(outState);
    }

    private void onReceivedSizes() {
        if (mStorageInfo == null || mAppsResult == null) {
            return;
        }

        if (getView().findViewById(R.id.loading_container).getVisibility() == View.VISIBLE) {
            setLoading(false /* loading */, true /* animate */);
        }

        final long privateUsedBytes = mStorageInfo.totalBytes - mStorageInfo.freeBytes;
        mPreferenceController.setVolume(mSelectedStorageEntry.getVolumeInfo());
        mPreferenceController.setUsedSize(privateUsedBytes);
        mPreferenceController.setTotalSize(mStorageInfo.totalBytes);
        // Cache total size infor and used size info
        mStorageCacheHelper
                .cacheTotalSizeAndTotalUsedSize(mStorageInfo.totalBytes, privateUsedBytes);
        for (int i = 0, size = mSecondaryUsers.size(); i < size; i++) {
            final AbstractPreferenceController controller = mSecondaryUsers.get(i);
            if (controller instanceof SecondaryUserController) {
                SecondaryUserController userController = (SecondaryUserController) controller;
                userController.setTotalSize(mStorageInfo.totalBytes);
            }
        }

        mPreferenceController.onLoadFinished(mAppsResult, mUserId);
        updateSecondaryUserControllers(mSecondaryUsers, mAppsResult);
        setSecondaryUsersVisible(true);
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.SETTINGS_STORAGE_CATEGORY;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.storage_category_fragment;
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        final List<AbstractPreferenceController> controllers = new ArrayList<>();
        final StorageManager sm = context.getSystemService(StorageManager.class);
        mPreferenceController = new StorageItemPreferenceController(context, this,
                null /* volume */, new StorageManagerVolumeProvider(sm), mIsWorkProfile);
        controllers.add(mPreferenceController);

        mSecondaryUsers = SecondaryUserController.getSecondaryUserControllers(context,
                mUserManager, mIsWorkProfile /* isWorkProfileOnly */);
        controllers.addAll(mSecondaryUsers);

        return controllers;
    }

    /**
     * Updates the secondary user controller sizes.
     */
    private void updateSecondaryUserControllers(List<AbstractPreferenceController> controllers,
            SparseArray<StorageAsyncLoader.StorageResult> stats) {
        for (int i = 0, size = controllers.size(); i < size; i++) {
            final AbstractPreferenceController controller = controllers.get(i);
            if (controller instanceof StorageAsyncLoader.ResultHandler) {
                StorageAsyncLoader.ResultHandler userController =
                        (StorageAsyncLoader.ResultHandler) controller;
                userController.handleResult(stats);
            }
        }
    }

    @Override
    public Loader<SparseArray<StorageAsyncLoader.StorageResult>> onCreateLoader(int id,
            Bundle args) {
        final Context context = getContext();
        return new StorageAsyncLoader(context, mUserManager,
                mSelectedStorageEntry.getFsUuid(),
                new StorageStatsSource(context),
                context.getPackageManager());
    }

    @Override
    public void onLoadFinished(Loader<SparseArray<StorageAsyncLoader.StorageResult>> loader,
            SparseArray<StorageAsyncLoader.StorageResult> data) {
        mAppsResult = data;
        onReceivedSizes();
    }

    @Override
    public void onLoaderReset(Loader<SparseArray<StorageAsyncLoader.StorageResult>> loader) {
    }

    @VisibleForTesting
    public PrivateStorageInfo getPrivateStorageInfo() {
        return mStorageInfo;
    }

    @VisibleForTesting
    public void setPrivateStorageInfo(PrivateStorageInfo info) {
        mStorageInfo = info;
    }

    @VisibleForTesting
    public SparseArray<StorageAsyncLoader.StorageResult> getStorageResult() {
        return mAppsResult;
    }

    @VisibleForTesting
    public void setStorageResult(SparseArray<StorageAsyncLoader.StorageResult> info) {
        mAppsResult = info;
    }

    /**
     * Activate loading UI and animation if it's necessary.
     */
    @VisibleForTesting
    public void maybeSetLoading(boolean isQuotaSupported) {
        // If we have fast stats, we load until both have loaded.
        // If we have slow stats, we load when we get the total volume sizes.
        if ((isQuotaSupported && (mStorageInfo == null || mAppsResult == null))
                || (!isQuotaSupported && mStorageInfo == null)) {
            setLoading(true /* loading */, false /* animate */);
        }
    }

    private boolean isQuotaSupported() {
        return mSelectedStorageEntry.isMounted()
                && getActivity().getSystemService(StorageStatsManager.class)
                        .isQuotaSupported(mSelectedStorageEntry.getFsUuid());
    }

    private void setSecondaryUsersVisible(boolean visible) {
        final Optional<SecondaryUserController> secondaryUserController = mSecondaryUsers.stream()
                .filter(controller -> controller instanceof SecondaryUserController)
                .map(controller -> (SecondaryUserController) controller)
                .findAny();
        if (secondaryUserController.isPresent()) {
            secondaryUserController.get().setPreferenceGroupVisible(visible);
        }
    }

    /**
     * IconLoaderCallbacks exists because StorageCategoryFragment already implements
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
        public void onLoaderReset(Loader<SparseArray<Drawable>> loader) {
        }
    }

    /**
     * VolumeSizeCallbacks exists because StorageCategoryFragment already implements
     * LoaderCallbacks for a different type.
     */
    public final class VolumeSizeCallbacks
            implements LoaderManager.LoaderCallbacks<PrivateStorageInfo> {
        @Override
        public Loader<PrivateStorageInfo> onCreateLoader(int id, Bundle args) {
            final Context context = getContext();
            final StorageManagerVolumeProvider smvp =
                    new StorageManagerVolumeProvider(mStorageManager);
            final StorageStatsManager stats = context.getSystemService(StorageStatsManager.class);
            return new VolumeSizesLoader(context, smvp, stats,
                    mSelectedStorageEntry.getVolumeInfo());
        }

        @Override
        public void onLoaderReset(Loader<PrivateStorageInfo> loader) {
        }

        @Override
        public void onLoadFinished(
                Loader<PrivateStorageInfo> loader, PrivateStorageInfo privateStorageInfo) {
            if (privateStorageInfo == null) {
                getActivity().finish();
                return;
            }

            mStorageInfo = privateStorageInfo;
            onReceivedSizes();
        }
    }
}
