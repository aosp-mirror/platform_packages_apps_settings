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
import android.content.Intent;
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
import android.view.View;

import androidx.annotation.VisibleForTesting;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.dashboard.profileselector.ProfileSelectFragment;
import com.android.settings.deviceinfo.storage.AutomaticStorageManagementSwitchPreferenceController;
import com.android.settings.deviceinfo.storage.CachedStorageValuesHelper;
import com.android.settings.deviceinfo.storage.DiskInitFragment;
import com.android.settings.deviceinfo.storage.SecondaryUserController;
import com.android.settings.deviceinfo.storage.StorageAsyncLoader;
import com.android.settings.deviceinfo.storage.StorageEntry;
import com.android.settings.deviceinfo.storage.StorageItemPreferenceController;
import com.android.settings.deviceinfo.storage.StorageSelectionPreferenceController;
import com.android.settings.deviceinfo.storage.StorageUsageProgressBarPreferenceController;
import com.android.settings.deviceinfo.storage.StorageUtils;
import com.android.settings.deviceinfo.storage.UserIconLoader;
import com.android.settings.deviceinfo.storage.VolumeSizesLoader;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.widget.EntityHeaderController;
import com.android.settingslib.applications.StorageStatsSource;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.deviceinfo.PrivateStorageInfo;
import com.android.settingslib.deviceinfo.StorageManagerVolumeProvider;
import com.android.settingslib.search.SearchIndexable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@SearchIndexable
public class StorageDashboardFragment extends DashboardFragment
        implements
        LoaderManager.LoaderCallbacks<SparseArray<StorageAsyncLoader.AppsStorageResult>>,
        Preference.OnPreferenceClickListener {
    private static final String TAG = "StorageDashboardFrag";
    private static final String SUMMARY_PREF_KEY = "storage_summary";
    private static final String FREE_UP_SPACE_PREF_KEY = "free_up_space";
    private static final String SELECTED_STORAGE_ENTRY_KEY = "selected_storage_entry_key";
    private static final int STORAGE_JOB_ID = 0;
    private static final int ICON_JOB_ID = 1;
    private static final int VOLUME_SIZE_JOB_ID = 2;

    private StorageManager mStorageManager;
    private UserManager mUserManager;
    private final List<StorageEntry> mStorageEntries = new ArrayList<>();
    private StorageEntry mSelectedStorageEntry;
    private PrivateStorageInfo mStorageInfo;
    private SparseArray<StorageAsyncLoader.AppsStorageResult> mAppsResult;
    private CachedStorageValuesHelper mCachedStorageValuesHelper;

    private StorageItemPreferenceController mPreferenceController;
    private VolumeOptionMenuController mOptionMenuController;
    private StorageSelectionPreferenceController mStorageSelectionController;
    private StorageUsageProgressBarPreferenceController mStorageUsageProgressBarController;
    private List<AbstractPreferenceController> mSecondaryUsers;
    private boolean mIsWorkProfile;
    private int mUserId;
    private Preference mFreeUpSpacePreference;

    private final StorageEventListener mStorageEventListener = new StorageEventListener() {
        @Override
        public void onVolumeStateChanged(VolumeInfo volumeInfo, int oldState, int newState) {
            if (!isInteresting(volumeInfo)) {
                return;
            }

            final StorageEntry changedStorageEntry = new StorageEntry(getContext(), volumeInfo);
            switch (volumeInfo.getState()) {
                case VolumeInfo.STATE_MOUNTED:
                case VolumeInfo.STATE_MOUNTED_READ_ONLY:
                case VolumeInfo.STATE_UNMOUNTABLE:
                    // Add mounted or unmountable storage in the list and show it on spinner.
                    // Unmountable storages are the storages which has a problem format and android
                    // is not able to mount it automatically.
                    // Users can format an unmountable storage by the UI and then use the storage.
                    mStorageEntries.removeIf(storageEntry -> {
                        return storageEntry.equals(changedStorageEntry);
                    });
                    mStorageEntries.add(changedStorageEntry);
                    if (changedStorageEntry.equals(mSelectedStorageEntry)) {
                        mSelectedStorageEntry = changedStorageEntry;
                    }
                    refreshUi();
                    break;
                case VolumeInfo.STATE_REMOVED:
                case VolumeInfo.STATE_UNMOUNTED:
                case VolumeInfo.STATE_BAD_REMOVAL:
                case VolumeInfo.STATE_EJECTING:
                    // Remove removed storage from list and don't show it on spinner.
                    if (mStorageEntries.remove(changedStorageEntry)) {
                        if (changedStorageEntry.equals(mSelectedStorageEntry)) {
                            mSelectedStorageEntry =
                                    StorageEntry.getDefaultInternalStorageEntry(getContext());
                        }
                        refreshUi();
                    }
                    break;
                default:
                    // Do nothing.
            }
        }

        @Override
        public void onVolumeRecordChanged(VolumeRecord volumeRecord) {
            if (isVolumeRecordMissed(volumeRecord)) {
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
            if (!isDiskUnsupported(disk)) {
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

    private static boolean isInteresting(VolumeInfo volumeInfo) {
        switch (volumeInfo.getType()) {
            case VolumeInfo.TYPE_PRIVATE:
            case VolumeInfo.TYPE_PUBLIC:
            case VolumeInfo.TYPE_STUB:
                return true;
            default:
                return false;
        }
    }

    /**
     * VolumeRecord is a metadata of VolumeInfo, this is the case where a VolumeInfo is missing.
     * (e.g., internal SD card is removed.)
     */
    private boolean isVolumeRecordMissed(VolumeRecord volumeRecord) {
        return volumeRecord.getType() == VolumeInfo.TYPE_PRIVATE
                && mStorageManager.findVolumeByUuid(volumeRecord.getFsUuid()) == null;
    }

    /**
     * A unsupported disk is the disk of problem format, android is not able to mount automatically.
     */
    private static boolean isDiskUnsupported(DiskInfo disk) {
        return disk.volumeCount == 0 && disk.size > 0;
    }

    private void refreshUi() {
        mStorageSelectionController.setStorageEntries(mStorageEntries);
        mStorageSelectionController.setSelectedStorageEntry(mSelectedStorageEntry);
        mStorageUsageProgressBarController.setSelectedStorageEntry(mSelectedStorageEntry);

        mOptionMenuController.setSelectedStorageEntry(mSelectedStorageEntry);
        getActivity().invalidateOptionsMenu();

        mPreferenceController.setVolume(mSelectedStorageEntry.getVolumeInfo());

        if (!mSelectedStorageEntry.isMounted()) {
            // Set null volume to hide category stats.
            mPreferenceController.setVolume(null);
            return;
        }
        if (mSelectedStorageEntry.isPrivate()) {
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

        initializePreference();
        initializeOptionsMenu(activity);
    }

    private void initializePreference() {
        mFreeUpSpacePreference = getPreferenceScreen().findPreference(FREE_UP_SPACE_PREF_KEY);
        mFreeUpSpacePreference.setOnPreferenceClickListener(this);
    }

    @Override
    public void onAttach(Context context) {
        // These member variables are initialized befoer super.onAttach for
        // createPreferenceControllers to work correctly.
        mUserManager = context.getSystemService(UserManager.class);
        mIsWorkProfile = getArguments().getInt(ProfileSelectFragment.EXTRA_PROFILE)
                == ProfileSelectFragment.ProfileType.WORK;
        mUserId = Utils.getCurrentUserId(mUserManager, mIsWorkProfile);

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
    public void onViewCreated(View v, Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);
        initializeCacheProvider();
        maybeSetLoading(isQuotaSupported());

        final Activity activity = getActivity();
        EntityHeaderController.newInstance(activity, this /*fragment*/,
                null /* header view */)
                .setRecyclerView(getListView(), getSettingsLifecycle());
    }

    @Override
    public void onResume() {
        super.onResume();

        mStorageEntries.clear();
        mStorageEntries.addAll(mStorageManager.getVolumes().stream()
                .filter(volumeInfo -> isInteresting(volumeInfo))
                .map(volumeInfo -> new StorageEntry(getContext(), volumeInfo))
                .collect(Collectors.toList()));
        mStorageEntries.addAll(mStorageManager.getDisks().stream()
                .filter(disk -> isDiskUnsupported(disk))
                .map(disk -> new StorageEntry(disk))
                .collect(Collectors.toList()));
        mStorageEntries.addAll(mStorageManager.getVolumeRecords().stream()
                .filter(volumeRecord -> isVolumeRecordMissed(volumeRecord))
                .map(volumeRecord -> new StorageEntry(volumeRecord))
                .collect(Collectors.toList()));
        refreshUi();
        mStorageManager.registerListener(mStorageEventListener);
    }

    @Override
    public void onPause() {
        super.onPause();
        mStorageManager.unregisterListener(mStorageEventListener);
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
        boolean stopLoading = false;
        if (mStorageInfo != null) {
            long privateUsedBytes = mStorageInfo.totalBytes - mStorageInfo.freeBytes;
            mPreferenceController.setVolume(mSelectedStorageEntry.getVolumeInfo());
            mPreferenceController.setUsedSize(privateUsedBytes);
            mPreferenceController.setTotalSize(mStorageInfo.totalBytes);
            for (int i = 0, size = mSecondaryUsers.size(); i < size; i++) {
                AbstractPreferenceController controller = mSecondaryUsers.get(i);
                if (controller instanceof SecondaryUserController) {
                    SecondaryUserController userController = (SecondaryUserController) controller;
                    userController.setTotalSize(mStorageInfo.totalBytes);
                }
            }
            stopLoading = true;

        }

        if (mAppsResult != null) {
            mPreferenceController.onLoadFinished(mAppsResult, mUserId);
            updateSecondaryUserControllers(mSecondaryUsers, mAppsResult);
            stopLoading = true;
        }

        // setLoading always causes a flicker, so let's avoid doing it.
        if (stopLoading) {
            if (getView().findViewById(R.id.loading_container).getVisibility() == View.VISIBLE) {
                setLoading(false, true);
            }
        }
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
        StorageManager sm = context.getSystemService(StorageManager.class);
        mPreferenceController = new StorageItemPreferenceController(context, this,
                null /* volume */, new StorageManagerVolumeProvider(sm), mIsWorkProfile);
        controllers.add(mPreferenceController);

        mSecondaryUsers = SecondaryUserController.getSecondaryUserControllers(context,
                mUserManager, mIsWorkProfile /* isWorkProfileOnly */);
        controllers.addAll(mSecondaryUsers);

        return controllers;
    }

    @VisibleForTesting
    protected void setVolume(VolumeInfo info) {
        mSelectedStorageEntry = new StorageEntry(getContext(), info);
    }

    /**
     * Updates the secondary user controller sizes.
     */
    private void updateSecondaryUserControllers(List<AbstractPreferenceController> controllers,
            SparseArray<StorageAsyncLoader.AppsStorageResult> stats) {
        for (int i = 0, size = controllers.size(); i < size; i++) {
            AbstractPreferenceController controller = controllers.get(i);
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
                            null /* volume */, new StorageManagerVolumeProvider(sm),
                            false /* isWorkProfile */));
                    controllers.addAll(SecondaryUserController.getSecondaryUserControllers(
                            context, userManager, false /* isWorkProfileOnly */));
                    return controllers;
                }

            };

    @Override
    public Loader<SparseArray<StorageAsyncLoader.AppsStorageResult>> onCreateLoader(int id,
            Bundle args) {
        final Context context = getContext();
        return new StorageAsyncLoader(context, mUserManager,
                mSelectedStorageEntry.getFsUuid(),
                new StorageStatsSource(context),
                context.getPackageManager());
    }

    @Override
    public void onLoadFinished(Loader<SparseArray<StorageAsyncLoader.AppsStorageResult>> loader,
            SparseArray<StorageAsyncLoader.AppsStorageResult> data) {
        mAppsResult = data;
        maybeCacheFreshValues();
        onReceivedSizes();
    }

    @Override
    public void onLoaderReset(Loader<SparseArray<StorageAsyncLoader.AppsStorageResult>> loader) {
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (preference == mFreeUpSpacePreference) {
            final Context context = getContext();
            final MetricsFeatureProvider metricsFeatureProvider =
                    FeatureFactory.getFactory(context).getMetricsFeatureProvider();
            metricsFeatureProvider.logClickedPreference(preference, getMetricsCategory());
            metricsFeatureProvider.action(context, SettingsEnums.STORAGE_FREE_UP_SPACE_NOW);
            final Intent intent = new Intent(StorageManager.ACTION_MANAGE_STORAGE);
            context.startActivityAsUser(intent, new UserHandle(mUserId));
            return true;
        }
        return false;
    }

    @VisibleForTesting
    public void setCachedStorageValuesHelper(CachedStorageValuesHelper helper) {
        mCachedStorageValuesHelper = helper;
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
    public SparseArray<StorageAsyncLoader.AppsStorageResult> getAppsStorageResult() {
        return mAppsResult;
    }

    @VisibleForTesting
    public void setAppsStorageResult(SparseArray<StorageAsyncLoader.AppsStorageResult> info) {
        mAppsResult = info;
    }

    @VisibleForTesting
    public void initializeCachedValues() {
        PrivateStorageInfo info = mCachedStorageValuesHelper.getCachedPrivateStorageInfo();
        SparseArray<StorageAsyncLoader.AppsStorageResult> loaderResult =
                mCachedStorageValuesHelper.getCachedAppsStorageResult();
        if (info == null || loaderResult == null) {
            return;
        }

        mStorageInfo = info;
        mAppsResult = loaderResult;
    }

    @VisibleForTesting
    public void maybeSetLoading(boolean isQuotaSupported) {
        // If we have fast stats, we load until both have loaded.
        // If we have slow stats, we load when we get the total volume sizes.
        if ((isQuotaSupported && (mStorageInfo == null || mAppsResult == null)) ||
                (!isQuotaSupported && mStorageInfo == null)) {
            setLoading(true /* loading */, false /* animate */);
        }
    }

    private void initializeCacheProvider() {
        mCachedStorageValuesHelper = new CachedStorageValuesHelper(getContext(), mUserId);
        initializeCachedValues();
        onReceivedSizes();
    }

    private void maybeCacheFreshValues() {
        if (mStorageInfo != null && mAppsResult != null) {
            mCachedStorageValuesHelper.cacheResult(mStorageInfo, mAppsResult.get(mUserId));
        }
    }

    private boolean isQuotaSupported() {
        return mSelectedStorageEntry.isMounted()
                && getActivity().getSystemService(StorageStatsManager.class)
                        .isQuotaSupported(mSelectedStorageEntry.getFsUuid());
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
        public void onLoaderReset(Loader<SparseArray<Drawable>> loader) {
        }
    }

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
            maybeCacheFreshValues();
            onReceivedSizes();
        }
    }
}
