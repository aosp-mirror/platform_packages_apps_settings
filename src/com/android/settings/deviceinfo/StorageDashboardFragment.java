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

import android.app.Activity;
import android.app.settings.SettingsEnums;
import android.app.usage.StorageStatsManager;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.storage.DiskInfo;
import android.os.storage.StorageEventListener;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.os.storage.VolumeRecord;
import android.provider.SearchIndexableResource;
import android.text.TextUtils;
import android.util.SparseArray;

import androidx.annotation.VisibleForTesting;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.deviceinfo.storage.AutomaticStorageManagementSwitchPreferenceController;
import com.android.settings.deviceinfo.storage.DiskInitFragment;
import com.android.settings.deviceinfo.storage.ManageStoragePreferenceController;
import com.android.settings.deviceinfo.storage.NonCurrentUserController;
import com.android.settings.deviceinfo.storage.StorageAsyncLoader;
import com.android.settings.deviceinfo.storage.StorageCacheHelper;
import com.android.settings.deviceinfo.storage.StorageEntry;
import com.android.settings.deviceinfo.storage.StorageItemPreferenceController;
import com.android.settings.deviceinfo.storage.StorageSelectionPreferenceController;
import com.android.settings.deviceinfo.storage.StorageUsageProgressBarPreferenceController;
import com.android.settings.deviceinfo.storage.StorageUtils;
import com.android.settings.deviceinfo.storage.UserIconLoader;
import com.android.settings.deviceinfo.storage.VolumeSizesLoader;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.applications.StorageStatsSource;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.deviceinfo.PrivateStorageInfo;
import com.android.settingslib.deviceinfo.StorageManagerVolumeProvider;
import com.android.settingslib.search.SearchIndexable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
@SearchIndexable
public class StorageDashboardFragment extends DashboardFragment
        implements
        LoaderManager.LoaderCallbacks<SparseArray<StorageAsyncLoader.StorageResult>> {
    private static final String TAG = "StorageDashboardFrag";
    private static final String SUMMARY_PREF_KEY = "storage_summary";
    private static final String SELECTED_STORAGE_ENTRY_KEY = "selected_storage_entry_key";
    private static final String TARGET_PREFERENCE_GROUP_KEY = "pref_non_current_users";
    private static final int STORAGE_JOB_ID = 0;
    private static final int ICON_JOB_ID = 1;
    private static final int VOLUME_SIZE_JOB_ID = 2;

    private StorageManager mStorageManager;
    private UserManager mUserManager;
    private final List<StorageEntry> mStorageEntries = new ArrayList<>();
    private StorageEntry mSelectedStorageEntry;
    private PrivateStorageInfo mStorageInfo;
    private SparseArray<StorageAsyncLoader.StorageResult> mAppsResult;

    private StorageItemPreferenceController mPreferenceController;
    private VolumeOptionMenuController mOptionMenuController;
    private StorageSelectionPreferenceController mStorageSelectionController;
    private StorageUsageProgressBarPreferenceController mStorageUsageProgressBarController;
    private List<NonCurrentUserController> mNonCurrentUsers;
    private boolean mIsWorkProfile;
    private int mUserId;
    private boolean mIsLoadedFromCache;
    private StorageCacheHelper mStorageCacheHelper;

    private final StorageEventListener mStorageEventListener = new StorageEventListener() {
        @Override
        public void onVolumeStateChanged(VolumeInfo volumeInfo, int oldState, int newState) {
            if (!StorageUtils.isStorageSettingsInterestedVolume(volumeInfo)) {
                return;
            }

            final StorageEntry changedStorageEntry = new StorageEntry(getContext(), volumeInfo);
            final int volumeState = volumeInfo.getState();
            switch (volumeState) {
                case VolumeInfo.STATE_REMOVED:
                case VolumeInfo.STATE_BAD_REMOVAL:
                    // Remove removed storage from list and don't show it on spinner.
                    if (!mStorageEntries.remove(changedStorageEntry)) {
                        break;
                    }
                case VolumeInfo.STATE_MOUNTED:
                case VolumeInfo.STATE_MOUNTED_READ_ONLY:
                case VolumeInfo.STATE_UNMOUNTABLE:
                case VolumeInfo.STATE_UNMOUNTED:
                case VolumeInfo.STATE_EJECTING:
                    // Add mounted or unmountable storage in the list and show it on spinner.
                    // Unmountable storages are the storages which has a problem format and android
                    // is not able to mount it automatically.
                    // Users can format an unmountable storage by the UI and then use the storage.
                    mStorageEntries.removeIf(storageEntry -> {
                        return storageEntry.equals(changedStorageEntry);
                    });
                    if (volumeState == VolumeInfo.STATE_MOUNTED
                            || volumeState == VolumeInfo.STATE_MOUNTED_READ_ONLY
                            || volumeState == VolumeInfo.STATE_UNMOUNTABLE) {
                        mStorageEntries.add(changedStorageEntry);
                        if (changedStorageEntry.equals(mSelectedStorageEntry)) {
                            mSelectedStorageEntry = changedStorageEntry;
                        }
                    } else {
                        if (changedStorageEntry.equals(mSelectedStorageEntry)) {
                            mSelectedStorageEntry =
                                    StorageEntry.getDefaultInternalStorageEntry(getContext());
                        }
                    }
                    refreshUi();
                    break;
                default:
                    // Do nothing.
            }
        }

        @Override
        public void onVolumeRecordChanged(VolumeRecord volumeRecord) {
            if (StorageUtils.isVolumeRecordMissed(mStorageManager, volumeRecord)) {
                // VolumeRecord is a metadata of VolumeInfo, if a VolumeInfo is missing
                // (e.g., internal SD card is removed.) show the missing storage to users,
                // users can insert the SD card or manually forget the storage from the device.
                final StorageEntry storageEntry = new StorageEntry(volumeRecord);
                if (!mStorageEntries.contains(storageEntry)) {
                    mStorageEntries.add(storageEntry);
                    refreshUi();
                }
            } else {
                // Find mapped VolumeInfo and replace with existing one for something changed.
                // (e.g., Renamed.)
                final VolumeInfo mappedVolumeInfo =
                        mStorageManager.findVolumeByUuid(volumeRecord.getFsUuid());
                if (mappedVolumeInfo == null) {
                    return;
                }

                final boolean removeMappedStorageEntry = mStorageEntries.removeIf(storageEntry ->
                        storageEntry.isVolumeInfo()
                            && TextUtils.equals(storageEntry.getFsUuid(), volumeRecord.getFsUuid())
                );
                if (removeMappedStorageEntry) {
                    mStorageEntries.add(new StorageEntry(getContext(), mappedVolumeInfo));
                    refreshUi();
                }
            }
        }

        @Override
        public void onVolumeForgotten(String fsUuid) {
            final StorageEntry storageEntry = new StorageEntry(
                    new VolumeRecord(VolumeInfo.TYPE_PUBLIC, fsUuid));
            if (mStorageEntries.remove(storageEntry)) {
                if (mSelectedStorageEntry.equals(storageEntry)) {
                    mSelectedStorageEntry =
                            StorageEntry.getDefaultInternalStorageEntry(getContext());
                }
                refreshUi();
            }
        }

        @Override
        public void onDiskScanned(DiskInfo disk, int volumeCount) {
            if (!StorageUtils.isDiskUnsupported(disk)) {
                return;
            }
            final StorageEntry storageEntry = new StorageEntry(disk);
            if (!mStorageEntries.contains(storageEntry)) {
                mStorageEntries.add(storageEntry);
                refreshUi();
            }
        }

        @Override
        public void onDiskDestroyed(DiskInfo disk) {
            final StorageEntry storageEntry = new StorageEntry(disk);
            if (mStorageEntries.remove(storageEntry)) {
                if (mSelectedStorageEntry.equals(storageEntry)) {
                    mSelectedStorageEntry =
                            StorageEntry.getDefaultInternalStorageEntry(getContext());
                }
                refreshUi();
            }
        }
    };

    private void refreshUi() {
        mStorageSelectionController.setStorageEntries(mStorageEntries);
        mStorageSelectionController.setSelectedStorageEntry(mSelectedStorageEntry);
        mStorageUsageProgressBarController.setSelectedStorageEntry(mSelectedStorageEntry);

        mOptionMenuController.setSelectedStorageEntry(mSelectedStorageEntry);
        getActivity().invalidateOptionsMenu();

        // To prevent flicker, hides non-current users preference.
        // onReceivedSizes will set it visible for private storage.
        setNonCurrentUsersVisible(false);

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
            // Hide the loading spinner if there is cached data.
            if (mStorageCacheHelper.hasCachedSizeInfo()) {
                //TODO(b/220259287): apply cache mechanism to non-current user
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

        final Activity activity = getActivity();
        mStorageManager = activity.getSystemService(StorageManager.class);

        if (icicle == null) {
            final VolumeInfo specifiedVolumeInfo =
                    Utils.maybeInitializeVolume(mStorageManager, getArguments());
            mSelectedStorageEntry = specifiedVolumeInfo == null
                    ? StorageEntry.getDefaultInternalStorageEntry(getContext())
                    : new StorageEntry(getContext(), specifiedVolumeInfo);
        } else {
            mSelectedStorageEntry = icicle.getParcelable(SELECTED_STORAGE_ENTRY_KEY);
        }

        initializeOptionsMenu(activity);

        if (mStorageCacheHelper.hasCachedSizeInfo()) {
            mIsLoadedFromCache = true;
            mStorageEntries.clear();
            mStorageEntries.addAll(
                    StorageUtils.getAllStorageEntries(getContext(), mStorageManager));
            refreshUi();
            updateNonCurrentUserControllers(mNonCurrentUsers, mAppsResult);
            setNonCurrentUsersVisible(true);
        }
    }

    @Override
    public void onAttach(Context context) {
        // These member variables are initialized befoer super.onAttach for
        // createPreferenceControllers to work correctly.
        mUserManager = context.getSystemService(UserManager.class);
        mUserId = UserHandle.myUserId();
        mStorageCacheHelper = new StorageCacheHelper(getContext(), mUserId);

        super.onAttach(context);
        use(AutomaticStorageManagementSwitchPreferenceController.class).setFragmentManager(
                getFragmentManager());
        mStorageSelectionController = use(StorageSelectionPreferenceController.class);
        mStorageSelectionController.setOnItemSelectedListener(storageEntry -> {
            mSelectedStorageEntry = storageEntry;
            refreshUi();

            if (storageEntry.isDiskInfoUnsupported() || storageEntry.isUnmountable()) {
                DiskInitFragment.show(this, R.string.storage_dialog_unmountable,
                        storageEntry.getDiskId());
            } else if (storageEntry.isVolumeRecordMissed()) {
                StorageUtils.launchForgetMissingVolumeRecordFragment(getContext(), storageEntry);
            }
        });
        mStorageUsageProgressBarController = use(StorageUsageProgressBarPreferenceController.class);

        ManageStoragePreferenceController manageStoragePreferenceController =
                use(ManageStoragePreferenceController.class);
        manageStoragePreferenceController.setUserId(mUserId);
    }

    @VisibleForTesting
    void initializeOptionsMenu(Activity activity) {
        mOptionMenuController = new VolumeOptionMenuController(activity, this,
                mSelectedStorageEntry);
        getSettingsLifecycle().addObserver(mOptionMenuController);
        setHasOptionsMenu(true);
        activity.invalidateOptionsMenu();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mIsLoadedFromCache) {
            mIsLoadedFromCache = false;
        } else {
            mStorageEntries.clear();
            mStorageEntries.addAll(
                    StorageUtils.getAllStorageEntries(getContext(), mStorageManager));
            refreshUi();
        }
        mStorageManager.registerListener(mStorageEventListener);
    }

    @Override
    public void onPause() {
        super.onPause();
        mStorageManager.unregisterListener(mStorageEventListener);
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

    @Override
    public int getHelpResource() {
        return R.string.help_url_storage_dashboard;
    }

    private void onReceivedSizes() {
        if (mStorageInfo == null || mAppsResult == null) {
            return;
        }

        setLoading(false /* loading */, false /* animate */);

        final long privateUsedBytes = mStorageInfo.totalBytes - mStorageInfo.freeBytes;
        mPreferenceController.setVolume(mSelectedStorageEntry.getVolumeInfo());
        mPreferenceController.setUsedSize(privateUsedBytes);
        mPreferenceController.setTotalSize(mStorageInfo.totalBytes);
        // Cache total size and used size
        mStorageCacheHelper
                .cacheTotalSizeAndTotalUsedSize(mStorageInfo.totalBytes, privateUsedBytes);
        for (NonCurrentUserController userController : mNonCurrentUsers) {
            userController.setTotalSize(mStorageInfo.totalBytes);
        }

        mPreferenceController.onLoadFinished(mAppsResult, mUserId);
        updateNonCurrentUserControllers(mNonCurrentUsers, mAppsResult);
        setNonCurrentUsersVisible(true);
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
        return R.xml.storage_dashboard_fragment;
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        final List<AbstractPreferenceController> controllers = new ArrayList<>();
        final StorageManager sm = context.getSystemService(StorageManager.class);
        mPreferenceController = new StorageItemPreferenceController(context, this,
                null /* volume */, new StorageManagerVolumeProvider(sm));
        controllers.add(mPreferenceController);

        mNonCurrentUsers = NonCurrentUserController.getNonCurrentUserControllers(context,
                mUserManager);
        controllers.addAll(mNonCurrentUsers);

        return controllers;
    }

    /**
     * Updates the non-current user controller sizes.
     */
    private void updateNonCurrentUserControllers(List<NonCurrentUserController> controllers,
            SparseArray<StorageAsyncLoader.StorageResult> stats) {
        for (AbstractPreferenceController controller : controllers) {
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
    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(
                        Context context, boolean enabled) {
                    final SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.storage_dashboard_fragment;
                    return Arrays.asList(sir);
                }

                @Override
                public List<AbstractPreferenceController> createPreferenceControllers(
                        Context context) {
                    final StorageManager sm = context.getSystemService(StorageManager.class);
                    final UserManager userManager = context.getSystemService(UserManager.class);
                    final List<AbstractPreferenceController> controllers = new ArrayList<>();
                    controllers.add(new StorageItemPreferenceController(context, null /* host */,
                            null /* volume */, new StorageManagerVolumeProvider(sm)));
                    controllers.addAll(NonCurrentUserController.getNonCurrentUserControllers(
                            context, userManager));
                    return controllers;
                }

            };

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


    @Override
    public void displayResourceTilesToScreen(PreferenceScreen screen) {
        final PreferenceGroup group = screen.findPreference(TARGET_PREFERENCE_GROUP_KEY);
        if (mNonCurrentUsers.isEmpty()) {
            screen.removePreference(group);
        }
        super.displayResourceTilesToScreen(screen);
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

    private void setNonCurrentUsersVisible(boolean visible) {
        if (!mNonCurrentUsers.isEmpty()) {
            mNonCurrentUsers.get(0).setPreferenceGroupVisible(visible);
        }
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
            mNonCurrentUsers
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
