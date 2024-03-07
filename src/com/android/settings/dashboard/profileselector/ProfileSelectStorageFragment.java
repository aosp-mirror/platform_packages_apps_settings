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

package com.android.settings.dashboard.profileselector;

import android.app.Activity;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.storage.DiskInfo;
import android.os.storage.StorageEventListener;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.os.storage.VolumeRecord;
import android.text.TextUtils;

import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.Fragment;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.deviceinfo.StorageCategoryFragment;
import com.android.settings.deviceinfo.VolumeOptionMenuController;
import com.android.settings.deviceinfo.storage.AutomaticStorageManagementSwitchPreferenceController;
import com.android.settings.deviceinfo.storage.DiskInitFragment;
import com.android.settings.deviceinfo.storage.StorageCacheHelper;
import com.android.settings.deviceinfo.storage.StorageEntry;
import com.android.settings.deviceinfo.storage.StorageSelectionPreferenceController;
import com.android.settings.deviceinfo.storage.StorageUsageProgressBarPreferenceController;
import com.android.settings.deviceinfo.storage.StorageUtils;

import java.util.ArrayList;
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
public class ProfileSelectStorageFragment extends ProfileSelectFragment {

    private static final String TAG = "ProfileSelStorageFrag";
    private static final String SELECTED_STORAGE_ENTRY_KEY = "selected_storage_entry_key";

    private StorageManager mStorageManager;

    private final List<StorageEntry> mStorageEntries = new ArrayList<>();
    private StorageEntry mSelectedStorageEntry;
    private Fragment[] mFragments;

    private StorageSelectionPreferenceController mStorageSelectionController;
    private StorageUsageProgressBarPreferenceController mStorageUsageProgressBarController;
    private VolumeOptionMenuController mOptionMenuController;
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
                    if (volumeState != VolumeInfo.STATE_REMOVED
                            && volumeState != VolumeInfo.STATE_BAD_REMOVAL) {
                        mStorageEntries.add(changedStorageEntry);
                    }
                    if (changedStorageEntry.equals(mSelectedStorageEntry)) {
                        mSelectedStorageEntry = changedStorageEntry;
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

    @Override
    public Fragment[] getFragments() {
        if (mFragments != null) {
            return mFragments;
        }

        mFragments = ProfileSelectFragment.getFragments(
                getContext(),
                null /* bundle */,
                StorageCategoryFragment::new,
                StorageCategoryFragment::new,
                StorageCategoryFragment::new);
        return mFragments;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.storage_dashboard_header_fragment;
    }

    private void refreshUi() {
        mStorageSelectionController.setStorageEntries(mStorageEntries);
        mStorageSelectionController.setSelectedStorageEntry(mSelectedStorageEntry);
        mStorageUsageProgressBarController.setSelectedStorageEntry(mSelectedStorageEntry);

        for (Fragment fragment : getFragments()) {
            if (!(fragment instanceof StorageCategoryFragment)) {
                throw new IllegalStateException("Wrong fragment type to refreshUi");
            }
            ((StorageCategoryFragment) fragment).refreshUi(mSelectedStorageEntry);
        }

        mOptionMenuController.setSelectedStorageEntry(mSelectedStorageEntry);
        getActivity().invalidateOptionsMenu();
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
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mStorageCacheHelper = new StorageCacheHelper(getContext(), UserHandle.myUserId());
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

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.SETTINGS_STORAGE_PROFILE_SELECTOR;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }
}
